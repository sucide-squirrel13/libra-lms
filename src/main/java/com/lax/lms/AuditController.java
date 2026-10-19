package com.lax.lms;

import com.lax.lms.repository.AuditLogRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/librarian/audit-logs")
    public String showAuditLogs(Model model) {

        List<AuditLog> logs =
                auditLogRepository.findAllByOrderByCreatedAtDesc();

        long successfulEvents = logs.stream()
                .filter(log -> "SUCCESS".equals(log.getOutcome()))
                .count();

        long failedEvents = logs.stream()
                .filter(log -> "FAILURE".equals(log.getOutcome()))
                .count();

        model.addAttribute("logs", logs);
        model.addAttribute("totalEvents", logs.size());
        model.addAttribute("successfulEvents", successfulEvents);
        model.addAttribute("failedEvents", failedEvents);

        return "audit-logs";
    }
}