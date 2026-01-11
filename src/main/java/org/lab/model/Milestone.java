package org.lab.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Milestone {
    private final UUID id;
    private final String name;
    private final String description;
    private MilestoneStatus status;
    private final UUID projectId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<UUID> ticketIds;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Milestone(UUID id, String name, String description, MilestoneStatus status,
                    UUID projectId, LocalDate startDate, LocalDate endDate,
                    List<UUID> ticketIds, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Milestone name cannot be null or blank");
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.projectId = projectId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.ticketIds = new ArrayList<>(ticketIds);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Milestone create(String name, String description, UUID projectId,
                                   LocalDate startDate, LocalDate endDate) {
        return new Milestone(
                UUID.randomUUID(),
                name,
                description,
                MilestoneStatus.OPEN,
                projectId,
                startDate,
                endDate,
                new ArrayList<>(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void changeStatus(MilestoneStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(STR."Cannot transition from \{status} to \{newStatus}");
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void addTicket(UUID ticketId) {
        if (!ticketIds.contains(ticketId)) {
            ticketIds.add(ticketId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean canClose(List<Ticket> tickets) {
        var milestoneTickets = tickets.stream()
                .filter(t -> ticketIds.contains(t.id()))
                .collect(Collectors.toList());

        return milestoneTickets.stream()
                .allMatch(t -> t.status().isCompleted());
    }

    public String getProgressSummary(List<Ticket> tickets) {
        var milestoneTickets = tickets.stream()
                .filter(t -> ticketIds.contains(t.id()))
                .toList();

        long completed = milestoneTickets.stream()
                .filter(t -> t.status().isCompleted())
                .count();

        return STR."\{name}: \{completed}/\{milestoneTickets.size()} tickets completed";
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public MilestoneStatus status() { return status; }
    public UUID projectId() { return projectId; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public List<UUID> ticketIds() { return Collections.unmodifiableList(ticketIds); }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
