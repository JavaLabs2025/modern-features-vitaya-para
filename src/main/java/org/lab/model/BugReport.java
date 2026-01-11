package org.lab.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record BugReport(
        UUID id,
        String title,
        String description,
        BugReportStatus status,
        UUID projectId,
        UUID reportedBy,
        UUID assignedTo,
        String severity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BugReport {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Bug report title cannot be null or blank");
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        if (reportedBy == null) {
            throw new IllegalArgumentException("Reporter ID cannot be null");
        }
    }

    public static BugReport create(String title, String description, UUID projectId,
                                   UUID reportedBy, String severity) {
        return new BugReport(
                UUID.randomUUID(),
                title,
                description,
                BugReportStatus.NEW,
                projectId,
                reportedBy,
                null,
                severity,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public BugReport withStatus(BugReportStatus newStatus) {
        return new BugReport(id, title, description, newStatus, projectId,
                reportedBy, assignedTo, severity, createdAt, LocalDateTime.now());
    }

    public BugReport assignTo(UUID developerId) {
        return new BugReport(id, title, description, status, projectId,
                reportedBy, developerId, severity, createdAt, LocalDateTime.now());
    }

    public String getStatusDescription() {
        return switch (status) {
            case NEW -> STR."Bug '\{title}' [\{severity}] has been reported and needs attention";
            case FIXED -> STR."Bug '\{title}' has been fixed and is ready for testing";
            case TESTED -> STR."Bug '\{title}' has been tested and verified";
            case CLOSED -> STR."Bug '\{title}' has been closed and resolved";
        };
    }

    public String getSeverityLevel() {
        return switch (severity.toLowerCase()) {
            case "critical" -> "CRITICAL - Immediate attention required";
            case "high" -> "HIGH - Should be fixed soon";
            case "medium" -> "MEDIUM - Normal priority";
            case "low" -> "LOW - Can be addressed later";
            default -> STR."UNKNOWN - \{severity}";
        };
    }
}
