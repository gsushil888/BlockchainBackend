package com.sushil.audittrail.repository;

import com.sushil.audittrail.entity.ModelRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, String> {
    Optional<ModelRegistry> findByModelId(String modelId);

    @Query("SELECT m FROM ModelRegistry m WHERE " +
           "(:modelId IS NULL OR m.modelId = :modelId) AND " +
           "(:provider IS NULL OR m.provider = :provider) AND " +
           "(:modelType IS NULL OR m.modelType = :modelType)")
    Page<ModelRegistry> search(
            @Param("modelId") String modelId,
            @Param("provider") String provider,
            @Param("modelType") String modelType,
            Pageable pageable);
}
