package org.lab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.lab.model.*;
import org.lab.service.ProjectAnalyticsService;
import org.lab.service.ProjectManagementService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectAnalyticsServiceTest {

    private ProjectManagementService projectService;
    private ProjectAnalyticsService analyticsService;
    private User manager;
    private User developer;
    private Project project;

    @BeforeEach
    void setUp() {
        projectService = new ProjectManagementService();
        analyticsService = new ProjectAnalyticsService(projectService);

        manager = projectService.registerUser("manager", "manager@test.com", "Test Manager");
        developer = projectService.registerUser("dev", "dev@test.com", "Test Developer");
        project = projectService.createProject("Test Project", "Description", manager.id());

        projectService.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());
    }

    @Test
    @DisplayName("Parallel loading of project analytics")
    void getProjectAnalytics_loadsAllDataInParallel() throws Exception {
        var ticket = projectService.createTicket("Test Ticket", "Description",
                project.id(), null, manager.id());
        projectService.createBugReport("Test Bug", "Bug description",
                project.id(), developer.id(), "high");
        projectService.createMilestone("Sprint 1", "First sprint", project.id(),
                LocalDate.now(), LocalDate.now().plusWeeks(2), manager.id());

        var analytics = analyticsService.getProjectAnalytics(project.id());

        assertNotNull(analytics);
        assertEquals(project.id(), analytics.project().id());
        assertEquals(1, analytics.tickets().size());
        assertEquals(1, analytics.bugReports().size());
        assertEquals(1, analytics.milestones().size());
    }

    @Test
    @DisplayName("Stats are calculated correctly")
    void getProjectAnalytics_calculatesStatsCorrectly() throws Exception {
        var ticket1 = projectService.createTicket("Ticket 1", "Desc", project.id(), null, manager.id());
        var ticket2 = projectService.createTicket("Ticket 2", "Desc", project.id(), null, manager.id());

        projectService.assignDevelopersToTicket(ticket1.id(), Set.of(developer.id()), manager.id());
        projectService.updateTicketStatus(ticket1.id(), TicketStatus.ACCEPTED, manager.id());
        projectService.updateTicketStatus(ticket1.id(), TicketStatus.IN_PROGRESS, developer.id());
        projectService.updateTicketStatus(ticket1.id(), TicketStatus.COMPLETED, developer.id());

        projectService.createBugReport("Critical Bug", "Desc", project.id(), developer.id(), "critical");

        var analytics = analyticsService.getProjectAnalytics(project.id());

        assertEquals(2, analytics.stats().totalTickets());
        assertEquals(1, analytics.stats().completedTickets());
        assertEquals(1, analytics.stats().openBugs());
        assertEquals(1, analytics.stats().criticalBugs());
        assertEquals(50.0, analytics.stats().completionPercentage(), 0.01);
    }

    @Test
    @DisplayName("Loading analytics for non-existent project throws exception")
    void getProjectAnalytics_throwsForNonExistentProject() {
        var fakeId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                analyticsService.getProjectAnalytics(fakeId));
    }

    @Test
    @DisplayName("Parallel loading of multiple projects")
    void getMultipleProjectsAnalytics_loadsMultipleProjectsInParallel() throws Exception {
        var project2 = projectService.createProject("Project 2", "Desc", manager.id());

        projectService.createTicket("Ticket P1", "Desc", project.id(), null, manager.id());
        projectService.createTicket("Ticket P2", "Desc", project2.id(), null, manager.id());

        var analyticsList = analyticsService.getMultipleProjectsAnalytics(
                List.of(project.id(), project2.id()));

        assertEquals(2, analyticsList.size());
        assertTrue(analyticsList.stream()
                .anyMatch(a -> a.project().name().equals("Test Project")));
        assertTrue(analyticsList.stream()
                .anyMatch(a -> a.project().name().equals("Project 2")));
    }

    @Test
    @DisplayName("Quick health check detects critical bugs")
    void quickHealthCheck_detectsCriticalBugs() throws Exception {
        projectService.createBugReport("System Crash", "Critical issue",
                project.id(), developer.id(), "critical");

        var result = analyticsService.quickHealthCheck(project.id());

        assertFalse(result.healthy());
        assertTrue(result.issue().contains("Critical bug"));
    }

    @Test
    @DisplayName("Empty project returns healthy status")
    void quickHealthCheck_returnsHealthyForEmptyProject() throws Exception {
        var result = analyticsService.quickHealthCheck(project.id());

        assertTrue(result.healthy());
        assertEquals("All checks passed", result.issue());
    }

    @Test
    @DisplayName("Parallel loading is faster than sequential")
    void getProjectAnalytics_fasterThanSequential() throws Exception {
        projectService.createTicket("Ticket", "Desc", project.id(), null, manager.id());
        projectService.createBugReport("Bug", "Desc", project.id(), developer.id(), "low");
        projectService.createMilestone("Sprint", "Desc", project.id(),
                LocalDate.now(), LocalDate.now().plusWeeks(1), manager.id());

        long start = System.currentTimeMillis();
        analyticsService.getProjectAnalytics(project.id());
        long parallelTime = System.currentTimeMillis() - start;

        assertTrue(parallelTime < 150,
                STR."Parallel execution should be faster: \{parallelTime}ms");
    }
}
