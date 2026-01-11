package org.lab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.lab.model.*;
import org.lab.service.ProjectManagementService;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProjectManagementServiceTest {

    private ProjectManagementService service;
    private User manager;
    private User developer;
    private User tester;
    private User teamLeader;

    @BeforeEach
    void setUp() {
        service = new ProjectManagementService();
        manager = service.registerUser("manager1", "manager@test.com", "Project Manager");
        developer = service.registerUser("dev1", "dev@test.com", "John Developer");
        tester = service.registerUser("tester1", "tester@test.com", "Jane Tester");
        teamLeader = service.registerUser("lead1", "lead@test.com", "Tech Lead");
    }

    @Test
    @DisplayName("Should create project with manager role")
    void testCreateProject() {
        var project = service.createProject("Test Project", "A test project", manager.id());

        assertNotNull(project);
        assertEquals("Test Project", project.name());
        assertTrue(project.hasRole(manager.id(), Role.Manager.class));
    }

    @Test
    @DisplayName("Should add team members to project")
    void testAddTeamMembers() {
        var project = service.createProject("Test Project", "Description", manager.id());

        service.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());
        service.addTeamMember(project.id(), tester.id(), new Role.Tester(), manager.id());

        assertTrue(project.hasRole(developer.id(), Role.Developer.class));
        assertTrue(project.hasRole(tester.id(), Role.Tester.class));
    }

    @Test
    @DisplayName("Should not allow non-manager to add team members")
    void testAddTeamMembersPermissionDenied() {
        var project = service.createProject("Test Project", "Description", manager.id());
        service.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());

        assertThrows(SecurityException.class, () ->
                service.addTeamMember(project.id(), tester.id(), new Role.Tester(), developer.id())
        );
    }

    @Test
    @DisplayName("Should assign team leader")
    void testAssignTeamLeader() {
        var project = service.createProject("Test Project", "Description", manager.id());
        service.addTeamMember(project.id(), teamLeader.id(), new Role.Developer(), manager.id());

        service.assignTeamLeader(project.id(), teamLeader.id(), manager.id());

        assertTrue(project.hasRole(teamLeader.id(), Role.TeamLeader.class));
        assertEquals(teamLeader.id(), project.teamLeaderId());
    }

    @Test
    @DisplayName("Should create milestone")
    void testCreateMilestone() {
        var project = service.createProject("Test Project", "Description", manager.id());
        var startDate = LocalDate.now();
        var endDate = startDate.plusDays(30);

        var milestone = service.createMilestone(
                "Sprint 1", "First sprint",
                project.id(), startDate, endDate, manager.id()
        );

        assertNotNull(milestone);
        assertEquals("Sprint 1", milestone.name());
        assertEquals(MilestoneStatus.OPEN, milestone.status());
    }

    @Test
    @DisplayName("Should change milestone status to active")
    void testChangeMilestoneStatus() {
        var project = service.createProject("Test Project", "Description", manager.id());
        var milestone = service.createMilestone(
                "Sprint 1", "First sprint",
                project.id(), LocalDate.now(), LocalDate.now().plusDays(30),
                manager.id()
        );

        service.changeMilestoneStatus(milestone.id(), MilestoneStatus.ACTIVE, manager.id());

        assertEquals(MilestoneStatus.ACTIVE, milestone.status());
        assertEquals(milestone.id(), project.activeMilestoneId());
    }

    @Test
    @DisplayName("Should not close milestone with incomplete tickets")
    void testCannotCloseMilestoneWithIncompleteTickets() {
        var project = service.createProject("Test Project", "Description", manager.id());
        service.addTeamMember(project.id(), teamLeader.id(), new Role.TeamLeader(), manager.id());

        var milestone = service.createMilestone(
                "Sprint 1", "First sprint",
                project.id(), LocalDate.now(), LocalDate.now().plusDays(30),
                manager.id()
        );

        service.changeMilestoneStatus(milestone.id(), MilestoneStatus.ACTIVE, manager.id());

        service.createTicket("Task 1", "Description",
                project.id(), milestone.id(), teamLeader.id());

        assertThrows(IllegalStateException.class, () ->
                service.changeMilestoneStatus(milestone.id(), MilestoneStatus.CLOSED, manager.id())
        );
    }

    @Test
    @DisplayName("Should create and manage tickets")
    void testTicketWorkflow() {
        var project = service.createProject("Test Project", "Description", manager.id());
        service.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());
        service.addTeamMember(project.id(), teamLeader.id(), new Role.TeamLeader(), manager.id());

        var milestone = service.createMilestone(
                "Sprint 1", "First sprint",
                project.id(), LocalDate.now(), LocalDate.now().plusDays(30),
                manager.id()
        );

        var ticket = service.createTicket(
                "Implement feature X", "Description",
                project.id(), milestone.id(), teamLeader.id()
        );

        assertEquals(TicketStatus.NEW, ticket.status());

        service.assignDevelopersToTicket(ticket.id(), Set.of(developer.id()), teamLeader.id());

        service.updateTicketStatus(ticket.id(), TicketStatus.ACCEPTED, teamLeader.id());
        ticket = service.getTicket(ticket.id()).orElseThrow();
        assertEquals(TicketStatus.ACCEPTED, ticket.status());

        service.updateTicketStatus(ticket.id(), TicketStatus.IN_PROGRESS, developer.id());
        ticket = service.getTicket(ticket.id()).orElseThrow();
        assertEquals(TicketStatus.IN_PROGRESS, ticket.status());

        service.updateTicketStatus(ticket.id(), TicketStatus.COMPLETED, developer.id());
        ticket = service.getTicket(ticket.id()).orElseThrow();
        assertEquals(TicketStatus.COMPLETED, ticket.status());
    }

    @Test
    @DisplayName("Should create and manage bug reports")
    void testBugReportWorkflow() {
        var project = service.createProject("Test Project", "Description", manager.id());
        service.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());
        service.addTeamMember(project.id(), tester.id(), new Role.Tester(), manager.id());

        var bugReport = service.createBugReport(
                "Login fails", "Users cannot login",
                project.id(), tester.id(), "high"
        );

        assertEquals(BugReportStatus.NEW, bugReport.status());

        service.assignBugReport(bugReport.id(), developer.id(), manager.id());

        service.updateBugReportStatus(bugReport.id(), BugReportStatus.FIXED, developer.id());
        bugReport = service.getBugReport(bugReport.id()).orElseThrow();
        assertEquals(BugReportStatus.FIXED, bugReport.status());

        service.updateBugReportStatus(bugReport.id(), BugReportStatus.TESTED, tester.id());
        bugReport = service.getBugReport(bugReport.id()).orElseThrow();
        assertEquals(BugReportStatus.TESTED, bugReport.status());

        service.updateBugReportStatus(bugReport.id(), BugReportStatus.CLOSED, manager.id());
        bugReport = service.getBugReport(bugReport.id()).orElseThrow();
        assertEquals(BugReportStatus.CLOSED, bugReport.status());
    }

    @Test
    @DisplayName("Should retrieve user projects")
    void testGetUserProjects() {
        var project1 = service.createProject("Project 1", "Description", manager.id());
        var project2 = service.createProject("Project 2", "Description", manager.id());
        service.addTeamMember(project1.id(), developer.id(), new Role.Developer(), manager.id());

        var managerProjects = service.getUserProjects(manager.id());
        assertEquals(2, managerProjects.size());

        var developerProjects = service.getUserProjects(developer.id());
        assertEquals(1, developerProjects.size());
    }

    @Test
    @DisplayName("Should retrieve user tickets")
    void testGetUserTickets() {
        var project = service.createProject("Test Project", "Description", manager.id());
        service.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());

        var ticket1 = service.createTicket("Task 1", "Desc", project.id(), null, manager.id());
        var ticket2 = service.createTicket("Task 2", "Desc", project.id(), null, manager.id());

        service.assignDevelopersToTicket(ticket1.id(), Set.of(developer.id()), manager.id());
        service.assignDevelopersToTicket(ticket2.id(), Set.of(developer.id()), manager.id());

        var userTickets = service.getUserTickets(developer.id());
        assertEquals(2, userTickets.size());
    }

    @Test
    @DisplayName("Should validate status transitions")
    void testStatusTransitions() {
        var project = service.createProject("Test Project", "Description", manager.id());
        service.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());

        var ticket = service.createTicket("Task", "Desc", project.id(), null, manager.id());
        service.assignDevelopersToTicket(ticket.id(), Set.of(developer.id()), manager.id());

        assertThrows(IllegalStateException.class, () ->
                service.updateTicketStatus(ticket.id(), TicketStatus.COMPLETED, developer.id())
        );
    }

    @Test
    @DisplayName("Should use string templates in records")
    void testStringTemplates() {
        var user = User.create("testuser", "test@test.com", "Test User");
        String shortInfo = user.shortInfo();

        assertTrue(shortInfo.contains("testuser"));
        assertTrue(shortInfo.contains("test@test.com"));
    }

    @Test
    @DisplayName("Should use sealed classes for roles")
    void testSealedRoles() {
        Role managerRole = new Role.Manager();
        Role devRole = new Role.Developer();

        assertEquals("Manager", managerRole.displayName());
        assertEquals("Developer", devRole.displayName());

        assertTrue(managerRole instanceof Role.Manager);
        assertFalse(devRole instanceof Role.Manager);
    }
}
