package org.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.lab.model.*;
import org.lab.service.PermissionChecker;

import static org.junit.jupiter.api.Assertions.*;

class PermissionCheckerTest {

    @Test
    @DisplayName("Should check manager permissions")
    void testManagerPermissions() {
        Role manager = new Role.Manager();

        assertTrue(PermissionChecker.hasPermission(manager,
                new PermissionChecker.Permission.CanManageUsers()));
        assertTrue(PermissionChecker.hasPermission(manager,
                new PermissionChecker.Permission.CanManageMilestones()));
        assertTrue(PermissionChecker.hasPermission(manager,
                new PermissionChecker.Permission.CanManageTickets()));
        assertFalse(PermissionChecker.hasPermission(manager,
                new PermissionChecker.Permission.CanWorkOnTickets()));
    }

    @Test
    @DisplayName("Should check team leader permissions")
    void testTeamLeaderPermissions() {
        Role teamLeader = new Role.TeamLeader();

        assertFalse(PermissionChecker.hasPermission(teamLeader,
                new PermissionChecker.Permission.CanManageUsers()));
        assertTrue(PermissionChecker.hasPermission(teamLeader,
                new PermissionChecker.Permission.CanManageTickets()));
        assertTrue(PermissionChecker.hasPermission(teamLeader,
                new PermissionChecker.Permission.CanWorkOnTickets()));
    }

    @Test
    @DisplayName("Should check developer permissions")
    void testDeveloperPermissions() {
        Role developer = new Role.Developer();

        assertFalse(PermissionChecker.hasPermission(developer,
                new PermissionChecker.Permission.CanManageTickets()));
        assertTrue(PermissionChecker.hasPermission(developer,
                new PermissionChecker.Permission.CanWorkOnTickets()));
        assertTrue(PermissionChecker.hasPermission(developer,
                new PermissionChecker.Permission.CanCreateBugReports()));
        assertTrue(PermissionChecker.hasPermission(developer,
                new PermissionChecker.Permission.CanFixBugReports()));
    }

    @Test
    @DisplayName("Should check tester permissions")
    void testTesterPermissions() {
        Role tester = new Role.Tester();

        assertFalse(PermissionChecker.hasPermission(tester,
                new PermissionChecker.Permission.CanWorkOnTickets()));
        assertTrue(PermissionChecker.hasPermission(tester,
                new PermissionChecker.Permission.CanCreateBugReports()));
        assertTrue(PermissionChecker.hasPermission(tester,
                new PermissionChecker.Permission.CanTestBugReports()));
        assertFalse(PermissionChecker.hasPermission(tester,
                new PermissionChecker.Permission.CanFixBugReports()));
    }

    @Test
    @DisplayName("Should validate ticket status transitions")
    void testTicketStatusTransitions() {
        var result1 = PermissionChecker.validateStatusTransition(
                TicketStatus.NEW, TicketStatus.ACCEPTED);
        assertTrue(result1.isEmpty());

        var result2 = PermissionChecker.validateStatusTransition(
                TicketStatus.NEW, TicketStatus.COMPLETED);
        assertTrue(result2.isPresent());
    }

    @Test
    @DisplayName("Should validate milestone status transitions")
    void testMilestoneStatusTransitions() {
        var result1 = PermissionChecker.validateStatusTransition(
                MilestoneStatus.OPEN, MilestoneStatus.ACTIVE);
        assertTrue(result1.isEmpty());

        var result2 = PermissionChecker.validateStatusTransition(
                MilestoneStatus.OPEN, MilestoneStatus.CLOSED);
        assertTrue(result2.isPresent());
    }

    @Test
    @DisplayName("Should validate bug report status transitions")
    void testBugReportStatusTransitions() {
        var result1 = PermissionChecker.validateStatusTransition(
                BugReportStatus.NEW, BugReportStatus.FIXED);
        assertTrue(result1.isEmpty());

        var result2 = PermissionChecker.validateStatusTransition(
                BugReportStatus.NEW, BugReportStatus.CLOSED);
        assertTrue(result2.isPresent());
    }

    @Test
    @DisplayName("Should get permission descriptions")
    void testPermissionDescriptions() {
        var desc = PermissionChecker.getPermissionDescription(
                new PermissionChecker.Permission.CanManageUsers());

        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    @DisplayName("Should get role descriptions using pattern matching")
    void testRoleDescriptions() {
        var managerDesc = PermissionChecker.getRoleDescription(new Role.Manager());
        var devDesc = PermissionChecker.getRoleDescription(new Role.Developer());

        assertTrue(managerDesc.contains("Manager"));
        assertTrue(devDesc.contains("Developer"));
    }

    @Test
    @DisplayName("Should use pattern matching with when guards")
    void testPatternMatchingWithGuards() {
        var currentStatus = TicketStatus.IN_PROGRESS;
        var newStatus = TicketStatus.COMPLETED;

        var result = PermissionChecker.validateStatusTransition(currentStatus, newStatus);

        assertTrue(result.isEmpty());
    }
}
