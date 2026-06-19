package com.sushil.repository;

import com.sushil.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    Page<User> findAll(Pageable pageable);

    /** Single query to find highest suffix for username base — avoids N+1 loop. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.username = :base OR u.username LIKE CONCAT(:base, '%')")
    long countByUsernameStartingWith(@Param("base") String base);
}
