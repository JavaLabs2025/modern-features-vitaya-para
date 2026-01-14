package org.lab.service;

import org.lab.model.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

public class ProjectAnalyticsService {

    private final ProjectManagementService projectService;

    public ProjectAnalyticsService(ProjectManagementService projectService) {
        this.projectService = projectService;
    }

    public record ProjectAnalytics(
            Project project,
            List<Ticket> tickets,
            List<BugReport> bugReports,
            List<Milestone> milestones,
            ProjectStats stats
    ) {}

    public record ProjectStats(
            int totalTickets,
            int completedTickets,
            int openBugs,
            int criticalBugs,
            int activeMilestones,
            double completionPercentage
    ) {}

    public record HealthCheckResult(boolean healthy, String issue) {}

    public ProjectAnalytics getProjectAnalytics(UUID projectId) throws Exception {
        var project = projectService.getProject(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        STR."Project \{projectId} not found"));

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var ticketsFuture = scope.fork(() -> loadTicketsForProject(project));
            var bugsFuture = scope.fork(() -> loadBugReportsForProject(project));
            var milestonesFuture = scope.fork(() -> loadMilestonesForProject(project));

            scope.join().throwIfFailed();

            var tickets = ticketsFuture.get();
            var bugs = bugsFuture.get();
            var milestones = milestonesFuture.get();
            var stats = calculateStats(tickets, bugs, milestones);

            return new ProjectAnalytics(project, tickets, bugs, milestones, stats);
        }
    }

    public List<ProjectAnalytics> getMultipleProjectsAnalytics(List<UUID> projectIds) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var futures = projectIds.stream()
                    .map(id -> scope.fork(() -> getProjectAnalytics(id)))
                    .toList();

            scope.join().throwIfFailed();

            return futures.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
        }
    }

    public HealthCheckResult quickHealthCheck(UUID projectId) throws Exception {
        var project = projectService.getProject(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        STR."Project \{projectId} not found"));

        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<HealthCheckResult>()) {
            scope.fork(() -> checkForCriticalBugs(project));
            scope.fork(() -> checkForOverdueTickets(project));
            scope.fork(() -> checkForStuckMilestones(project));

            scope.join();

            try {
                return scope.result();
            } catch (Exception e) {
                return new HealthCheckResult(true, "All checks passed");
            }
        }
    }

    private List<Ticket> loadTicketsForProject(Project project) {
        simulateLatency();
        return projectService.getTicketsByProject(project.id());
    }

    private List<BugReport> loadBugReportsForProject(Project project) {
        simulateLatency();
        return projectService.getBugReportsByProject(project.id());
    }

    private List<Milestone> loadMilestonesForProject(Project project) {
        simulateLatency();
        return projectService.getMilestonesByProject(project.id());
    }

    private HealthCheckResult checkForCriticalBugs(Project project) throws InterruptedException {
        var bugs = projectService.getBugReportsByProject(project.id());
        var criticalBug = bugs.stream()
                .filter(b -> "critical".equalsIgnoreCase(b.severity()))
                .filter(b -> b.status() == BugReportStatus.NEW)
                .findFirst();

        if (criticalBug.isPresent()) {
            return new HealthCheckResult(false,
                    STR."Critical bug found: \{criticalBug.get().title()}");
        }
        Thread.sleep(100);
        throw new RuntimeException("No critical bugs");
    }

    private HealthCheckResult checkForOverdueTickets(Project project) throws InterruptedException {
        var tickets = projectService.getTicketsByProject(project.id());
        var stuckTickets = tickets.stream()
                .filter(t -> t.status() == TicketStatus.IN_PROGRESS)
                .filter(t -> t.updatedAt().isBefore(java.time.LocalDateTime.now().minusDays(7)))
                .count();

        if (stuckTickets > 0) {
            return new HealthCheckResult(false,
                    STR."Found \{stuckTickets} tickets stuck in progress for over a week");
        }
        Thread.sleep(100);
        throw new RuntimeException("No overdue tickets");
    }

    private HealthCheckResult checkForStuckMilestones(Project project) throws InterruptedException {
        var milestones = projectService.getMilestonesByProject(project.id());
        var overdue = milestones.stream()
                .filter(m -> m.status() == MilestoneStatus.ACTIVE)
                .filter(m -> m.endDate().isBefore(java.time.LocalDate.now()))
                .findFirst();

        if (overdue.isPresent()) {
            return new HealthCheckResult(false,
                    STR."Milestone '\{overdue.get().name()}' is overdue");
        }
        Thread.sleep(100);
        throw new RuntimeException("No stuck milestones");
    }

    private ProjectStats calculateStats(List<Ticket> tickets, List<BugReport> bugs, List<Milestone> milestones) {
        int totalTickets = tickets.size();
        int completedTickets = (int) tickets.stream()
                .filter(t -> t.status() == TicketStatus.COMPLETED)
                .count();

        int openBugs = (int) bugs.stream()
                .filter(b -> b.status() == BugReportStatus.NEW)
                .count();

        int criticalBugs = (int) bugs.stream()
                .filter(b -> "critical".equalsIgnoreCase(b.severity()))
                .filter(b -> b.status() != BugReportStatus.CLOSED)
                .count();

        int activeMilestones = (int) milestones.stream()
                .filter(m -> m.status() == MilestoneStatus.ACTIVE)
                .count();

        double completionPercentage = totalTickets > 0
                ? (double) completedTickets / totalTickets * 100
                : 0.0;

        return new ProjectStats(totalTickets, completedTickets, openBugs,
                criticalBugs, activeMilestones, completionPercentage);
    }

    private void simulateLatency() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
