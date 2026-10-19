package com.lax.lms.repository;

import com.lax.lms.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByActorEmailIgnoreCaseOrderByCreatedAtDesc(
            String actorEmail);

    List<AuditLog> findByOutcomeOrderByCreatedAtDesc(String outcome);
}