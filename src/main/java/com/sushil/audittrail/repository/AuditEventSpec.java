package com.sushil.audittrail.repository;

import com.sushil.audittrail.entity.AuditEvent;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuditEventSpec {

    private AuditEventSpec() {}

    public static Specification<AuditEvent> filter(String auditEventId, String decisionType,
                                                    String modelId, String userId, String channel,
                                                    LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (auditEventId != null) predicates.add(cb.equal(root.get("auditEventId"), auditEventId));
            if (decisionType  != null) predicates.add(cb.equal(root.get("decisionType"), decisionType));
            if (modelId       != null) predicates.add(cb.equal(root.get("modelRegistry").get("modelId"), modelId));
            if (userId        != null) predicates.add(cb.equal(root.get("actorInfo").get("initiatingUserId"), userId));
            if (channel       != null) predicates.add(cb.equal(root.get("actorInfo").get("channel"), channel));
            if (from          != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to            != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
