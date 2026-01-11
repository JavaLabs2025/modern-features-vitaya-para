package org.lab.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Project {
    private final UUID id;
    private final String name;
    private final String description;
    private final Map<UUID, Role> teamMembers;
    private UUID managerId;
    private UUID teamLeaderId;
    private final List<UUID> milestoneIds;
    private final List<UUID> bugReportIds;
    private UUID activeMilestoneId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Project(UUID id, String name, String description, Map<UUID, Role> teamMembers,
                  UUID managerId, UUID teamLeaderId, List<UUID> milestoneIds,
                  List<UUID> bugReportIds, UUID activeMilestoneId,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.teamMembers = new HashMap<>(teamMembers);
        this.managerId = managerId;
        this.teamLeaderId = teamLeaderId;
        this.milestoneIds = new ArrayList<>(milestoneIds);
        this.bugReportIds = new ArrayList<>(bugReportIds);
        this.activeMilestoneId = activeMilestoneId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Project create(String name, String description, UUID creatorId) {
        var teamMembers = new HashMap<UUID, Role>();
        teamMembers.put(creatorId, new Role.Manager());

        return new Project(
                UUID.randomUUID(),
                name,
                description,
                teamMembers,
                creatorId,
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void addTeamMember(UUID userId, Role role) {
        teamMembers.put(userId, role);
        this.updatedAt = LocalDateTime.now();
    }

    public void setTeamLeader(UUID userId) {
        if (!teamMembers.containsKey(userId)) {
            throw new IllegalArgumentException("User is not a team member");
        }
        this.teamLeaderId = userId;
        teamMembers.put(userId, new Role.TeamLeader());
        this.updatedAt = LocalDateTime.now();
    }

    public void addMilestone(UUID milestoneId) {
        milestoneIds.add(milestoneId);
        this.updatedAt = LocalDateTime.now();
    }

    public void setActiveMilestone(UUID milestoneId) {
        if (!milestoneIds.contains(milestoneId)) {
            throw new IllegalArgumentException("Milestone does not belong to this project");
        }
        this.activeMilestoneId = milestoneId;
        this.updatedAt = LocalDateTime.now();
    }

    public void addBugReport(UUID bugReportId) {
        bugReportIds.add(bugReportId);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasRole(UUID userId, Class<? extends Role> roleClass) {
        var role = teamMembers.get(userId);
        return role != null && roleClass.isInstance(role);
    }

    public Optional<Role> getUserRole(UUID userId) {
        return Optional.ofNullable(teamMembers.get(userId));
    }

    public List<UUID> getUsersByRole(Class<? extends Role> roleClass) {
        return teamMembers.entrySet().stream()
                .filter(entry -> roleClass.isInstance(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public String getTeamSummary() {
        long developers = teamMembers.values().stream()
                .filter(r -> r instanceof Role.Developer)
                .count();
        long testers = teamMembers.values().stream()
                .filter(r -> r instanceof Role.Tester)
                .count();

        return STR."""
            Project: \{name}
            Team size: \{teamMembers.size()}
            Developers: \{developers}
            Testers: \{testers}
            Milestones: \{milestoneIds.size()}
            Bug reports: \{bugReportIds.size()}
            """;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Map<UUID, Role> teamMembers() { return Collections.unmodifiableMap(teamMembers); }
    public UUID managerId() { return managerId; }
    public UUID teamLeaderId() { return teamLeaderId; }
    public List<UUID> milestoneIds() { return Collections.unmodifiableList(milestoneIds); }
    public List<UUID> bugReportIds() { return Collections.unmodifiableList(bugReportIds); }
    public UUID activeMilestoneId() { return activeMilestoneId; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
