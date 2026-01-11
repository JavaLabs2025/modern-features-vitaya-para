package org.lab.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record Ticket(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        UUID projectId,
        UUID milestoneId,
        Set<UUID> assignedDevelopers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public Ticket {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Ticket title cannot be null or blank");
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        assignedDevelopers = Set.copyOf(assignedDevelopers);
    }

    public static Ticket create(String title, String description, UUID projectId, UUID milestoneId) {
        return new Ticket(
                UUID.randomUUID(),
                title,
                description,
                TicketStatus.NEW,
                projectId,
                milestoneId,
                Set.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public Ticket withStatus(TicketStatus newStatus) {
        return new Ticket(id, title, description, newStatus, projectId, milestoneId,
                assignedDevelopers, createdAt, LocalDateTime.now());
    }

    public Ticket assignDevelopers(Set<UUID> developers) {
        return new Ticket(id, title, description, status, projectId, milestoneId,
                developers, createdAt, LocalDateTime.now());
    }

    public String getStatusDescription() {
        return switch (status) {
            case NEW -> STR."Ticket '\{title}' is new and waiting to be accepted";
            case ACCEPTED -> STR."Ticket '\{title}' has been accepted and ready to work on";
            case IN_PROGRESS -> STR."Ticket '\{title}' is currently being worked on by \{assignedDevelopers.size()} developer(s)";
            case COMPLETED -> STR."Ticket '\{title}' has been completed";
        };
    }
}
