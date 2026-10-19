package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String actorEmail,
            String action,
            String details,
            String outcome) {

        AuditLog log = new AuditLog();

        log.setActorEmail(actorEmail);
        log.setAction(action);
        log.setDetails(details);
        log.setOutcome(outcome);
        log.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(log);
    }
}