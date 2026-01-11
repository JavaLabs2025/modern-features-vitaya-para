package org.lab.service;

import org.lab.model.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ProjectManagementService {
    private final Map<UUID, User> users = new HashMap<>();
    private final Map<UUID, Project> projects = new HashMap<>();
    private final Map<UUID, Milestone> milestones = new HashMap<>();
    private final Map<UUID, Ticket> tickets = new HashMap<>();
    private final Map<UUID, BugReport> bugReports = new HashMap<>();

    public User registerUser(String username, String email, String fullName) {
        var user = User.create(username, email, fullName);
        users.put(user.id(), user);
        return user;
    }

    public Project createProject(String name, String description, UUID creatorId) {
        validateUserExists(creatorId);
        var project = Project.create(name, description, creatorId);
        projects.put(project.id(), project);
        return project;
    }

    public void addTeamMember(UUID projectId, UUID userId, Role role, UUID requesterId) {
        var project = getProjectOrThrow(projectId);
        validateUserExists(userId);
        validateManagerPermission(project, requesterId);

        project.addTeamMember(userId, role);
    }

    public void assignTeamLeader(UUID projectId, UUID userId, UUID requesterId) {
        var project = getProjectOrThrow(projectId);
        validateUserExists(userId);
        validateManagerPermission(project, requesterId);

        project.setTeamLeader(userId);
    }

    public Milestone createMilestone(String name, String description, UUID projectId,
                                    LocalDate startDate, LocalDate endDate, UUID requesterId) {
        var project = getProjectOrThrow(projectId);
        validateManagerPermission(project, requesterId);

        var milestone = Milestone.create(name, description, projectId, startDate, endDate);
        milestones.put(milestone.id(), milestone);
        project.addMilestone(milestone.id());

        return milestone;
    }

    public void changeMilestoneStatus(UUID milestoneId, MilestoneStatus newStatus, UUID requesterId) {
        var milestone = getMilestoneOrThrow(milestoneId);
        var project = getProjectOrThrow(milestone.projectId());
        validateManagerPermission(project, requesterId);

        if (newStatus == MilestoneStatus.CLOSED) {
            var milestoneTickets = getTicketsByMilestone(milestoneId);
            if (!milestone.canClose(milestoneTickets)) {
                throw new IllegalStateException("Cannot close milestone - not all tickets are completed");
            }
        }

        milestone.changeStatus(newStatus);

        if (newStatus == MilestoneStatus.ACTIVE) {
            project.setActiveMilestone(milestoneId);
        }
    }

    public Ticket createTicket(String title, String description, UUID projectId,
                              UUID milestoneId, UUID requesterId) {
        var project = getProjectOrThrow(projectId);
        validateTicketCreationPermission(project, requesterId);

        if (milestoneId != null) {
            var milestone = getMilestoneOrThrow(milestoneId);
            if (!milestone.projectId().equals(projectId)) {
                throw new IllegalArgumentException("Milestone does not belong to the project");
            }
        }

        var ticket = Ticket.create(title, description, projectId, milestoneId);
        tickets.put(ticket.id(), ticket);

        if (milestoneId != null) {
            var milestone = milestones.get(milestoneId);
            milestone.addTicket(ticket.id());
        }

        return ticket;
    }

    public void assignDevelopersToTicket(UUID ticketId, Set<UUID> developerIds, UUID requesterId) {
        var ticket = getTicketOrThrow(ticketId);
        var project = getProjectOrThrow(ticket.projectId());
        validateTicketManagementPermission(project, requesterId);

        developerIds.forEach(this::validateUserExists);
        developerIds.forEach(devId -> {
            if (!project.hasRole(devId, Role.Developer.class) &&
                !project.hasRole(devId, Role.TeamLeader.class)) {
                throw new IllegalArgumentException(STR."User \{devId} is not a developer in this project");
            }
        });

        var updatedTicket = ticket.assignDevelopers(developerIds);
        tickets.put(ticketId, updatedTicket);
    }

    public void updateTicketStatus(UUID ticketId, TicketStatus newStatus, UUID requesterId) {
        var ticket = getTicketOrThrow(ticketId);
        var project = getProjectOrThrow(ticket.projectId());

        var canUpdate = switch (newStatus) {
            case ACCEPTED -> project.hasRole(requesterId, Role.Manager.class) ||
                           project.hasRole(requesterId, Role.TeamLeader.class);
            case IN_PROGRESS, COMPLETED -> ticket.assignedDevelopers().contains(requesterId) ||
                                          project.hasRole(requesterId, Role.TeamLeader.class);
            case NEW -> false;
        };

        if (!canUpdate) {
            throw new SecurityException("User does not have permission to update ticket status");
        }

        if (!ticket.status().canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    STR."Cannot transition ticket from \{ticket.status()} to \{newStatus}");
        }

        var updatedTicket = ticket.withStatus(newStatus);
        tickets.put(ticketId, updatedTicket);
    }

    public BugReport createBugReport(String title, String description, UUID projectId,
                                    UUID reporterId, String severity) {
        var project = getProjectOrThrow(projectId);
        validateUserExists(reporterId);

        if (!project.getUserRole(reporterId).isPresent()) {
            throw new IllegalArgumentException("User is not a member of this project");
        }

        var bugReport = BugReport.create(title, description, projectId, reporterId, severity);
        bugReports.put(bugReport.id(), bugReport);
        project.addBugReport(bugReport.id());

        return bugReport;
    }

    public void assignBugReport(UUID bugReportId, UUID developerId, UUID requesterId) {
        var bugReport = getBugReportOrThrow(bugReportId);
        var project = getProjectOrThrow(bugReport.projectId());
        validateManagerOrTeamLeaderPermission(project, requesterId);
        validateUserExists(developerId);

        if (!project.hasRole(developerId, Role.Developer.class)) {
            throw new IllegalArgumentException("Assigned user must be a developer");
        }

        var updatedBugReport = bugReport.assignTo(developerId);
        bugReports.put(bugReportId, updatedBugReport);
    }

    public void updateBugReportStatus(UUID bugReportId, BugReportStatus newStatus, UUID requesterId) {
        var bugReport = getBugReportOrThrow(bugReportId);
        var project = getProjectOrThrow(bugReport.projectId());

        var canUpdate = switch (newStatus) {
            case FIXED -> bugReport.assignedTo() != null &&
                         bugReport.assignedTo().equals(requesterId);
            case TESTED -> project.hasRole(requesterId, Role.Tester.class);
            case CLOSED -> project.hasRole(requesterId, Role.Manager.class) ||
                          project.hasRole(requesterId, Role.TeamLeader.class);
            case NEW -> false;
        };

        if (!canUpdate) {
            throw new SecurityException("User does not have permission to update bug report status");
        }

        if (!bugReport.status().canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    STR."Cannot transition bug report from \{bugReport.status()} to \{newStatus}");
        }

        var updatedBugReport = bugReport.withStatus(newStatus);
        bugReports.put(bugReportId, updatedBugReport);
    }

    public List<Project> getUserProjects(UUID userId) {
        return projects.values().stream()
                .filter(p -> p.teamMembers().containsKey(userId))
                .collect(Collectors.toList());
    }

    public List<Ticket> getUserTickets(UUID userId) {
        return tickets.values().stream()
                .filter(t -> t.assignedDevelopers().contains(userId))
                .collect(Collectors.toList());
    }

    public List<BugReport> getUserBugReports(UUID userId) {
        return bugReports.values().stream()
                .filter(b -> userId.equals(b.assignedTo()))
                .collect(Collectors.toList());
    }

    public List<Ticket> getTicketsByMilestone(UUID milestoneId) {
        return tickets.values().stream()
                .filter(t -> milestoneId.equals(t.milestoneId()))
                .collect(Collectors.toList());
    }

    private void validateUserExists(UUID userId) {
        if (!users.containsKey(userId)) {
            throw new IllegalArgumentException(STR."User with id \{userId} does not exist");
        }
    }

    private void validateManagerPermission(Project project, UUID userId) {
        if (!project.hasRole(userId, Role.Manager.class)) {
            throw new SecurityException("Only project manager can perform this action");
        }
    }

    private void validateManagerOrTeamLeaderPermission(Project project, UUID userId) {
        if (!project.hasRole(userId, Role.Manager.class) &&
            !project.hasRole(userId, Role.TeamLeader.class)) {
            throw new SecurityException("Only project manager or team leader can perform this action");
        }
    }

    private void validateTicketCreationPermission(Project project, UUID userId) {
        if (!project.hasRole(userId, Role.Manager.class) &&
            !project.hasRole(userId, Role.TeamLeader.class)) {
            throw new SecurityException("Only manager or team leader can create tickets");
        }
    }

    private void validateTicketManagementPermission(Project project, UUID userId) {
        if (!project.hasRole(userId, Role.Manager.class) &&
            !project.hasRole(userId, Role.TeamLeader.class)) {
            throw new SecurityException("Only manager or team leader can assign developers to tickets");
        }
    }

    private Project getProjectOrThrow(UUID projectId) {
        return Optional.ofNullable(projects.get(projectId))
                .orElseThrow(() -> new IllegalArgumentException(STR."Project with id \{projectId} does not exist"));
    }

    private Milestone getMilestoneOrThrow(UUID milestoneId) {
        return Optional.ofNullable(milestones.get(milestoneId))
                .orElseThrow(() -> new IllegalArgumentException(STR."Milestone with id \{milestoneId} does not exist"));
    }

    private Ticket getTicketOrThrow(UUID ticketId) {
        return Optional.ofNullable(tickets.get(ticketId))
                .orElseThrow(() -> new IllegalArgumentException(STR."Ticket with id \{ticketId} does not exist"));
    }

    private BugReport getBugReportOrThrow(UUID bugReportId) {
        return Optional.ofNullable(bugReports.get(bugReportId))
                .orElseThrow(() -> new IllegalArgumentException(STR."Bug report with id \{bugReportId} does not exist"));
    }

    public Optional<User> getUser(UUID userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public Optional<Project> getProject(UUID projectId) {
        return Optional.ofNullable(projects.get(projectId));
    }

    public Optional<Milestone> getMilestone(UUID milestoneId) {
        return Optional.ofNullable(milestones.get(milestoneId));
    }

    public Optional<Ticket> getTicket(UUID ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    public Optional<BugReport> getBugReport(UUID bugReportId) {
        return Optional.ofNullable(bugReports.get(bugReportId));
    }
}
