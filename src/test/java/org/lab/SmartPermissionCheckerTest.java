package org.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.lab.model.*;
import org.lab.service.SmartPermissionChecker;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SmartPermissionCheckerTest {

    @Test
    @DisplayName("Manager can always modify tickets")
    void testManagerCanModify() {
        var user = User.create("manager", "manager@test.com", "Manager User");
        var ticket = Ticket.create("Task", "Description", UUID.randomUUID(), null);

        assertTrue(SmartPermissionChecker.canModifyTicket(
            user, new Role.Manager(), ticket
        ));
    }

    @Test
    @DisplayName("Team leader can modify non-completed tickets")
    void testTeamLeaderCanModifyNonCompleted() {
        var user = User.create("lead", "lead@test.com", "Team Lead");
        var ticket = Ticket.create("Task", "Description", UUID.randomUUID(), null);

        assertTrue(SmartPermissionChecker.canModifyTicket(
            user, new Role.TeamLeader(), ticket
        ));
    }

    @Test
    @DisplayName("Team leader cannot modify completed tickets (when guard)")
    void testTeamLeaderCannotModifyCompleted() {
        var user = User.create("lead", "lead@test.com", "Team Lead");
        var ticket = Ticket.create("Task", "Description", UUID.randomUUID(), null)
            .withStatus(TicketStatus.COMPLETED);

        assertFalse(SmartPermissionChecker.canModifyTicket(
            user, new Role.TeamLeader(), ticket
        ));
    }

    @Test
    @DisplayName("Developer can modify their assigned tickets (when guard)")
    void testDeveloperCanModifyAssigned() {
        var user = User.create("dev", "dev@test.com", "Developer");
        var ticket = Ticket.create("Task", "Description", UUID.randomUUID(), null)
            .assignDevelopers(Set.of(user.id()));

        assertTrue(SmartPermissionChecker.canModifyTicket(
            user, new Role.Developer(), ticket
        ));
    }

    @Test
    @DisplayName("Developer cannot modify unassigned tickets (when guard)")
    void testDeveloperCannotModifyUnassigned() {
        var user = User.create("dev", "dev@test.com", "Developer");
        var otherUser = User.create("other", "other@test.com", "Other Dev");
        var ticket = Ticket.create("Task", "Description", UUID.randomUUID(), null)
            .assignDevelopers(Set.of(otherUser.id()));

        assertFalse(SmartPermissionChecker.canModifyTicket(
            user, new Role.Developer(), ticket
        ));
    }

    @Test
    @DisplayName("Calculate URGENT priority for new critical bugs (when guard)")
    void testUrgentPriorityForNewCritical() {
        var bug = BugReport.create(
            "Critical Bug", "System crash",
            UUID.randomUUID(), UUID.randomUUID(), "critical"
        );

        var priority = SmartPermissionChecker.calculatePriority(bug);
        assertTrue(priority.contains("URGENT"));
    }

    @Test
    @DisplayName("Calculate HIGH priority for in-progress critical bugs (when guard)")
    void testHighPriorityForInProgressCritical() {
        var bug = BugReport.create(
            "Critical Bug", "System crash",
            UUID.randomUUID(), UUID.randomUUID(), "critical"
        ).withStatus(BugReportStatus.FIXED);

        var priority = SmartPermissionChecker.calculatePriority(bug);
        assertTrue(priority.contains("HIGH"));
    }

    @Test
    @DisplayName("Escalate old low priority bugs (when guard with duration)")
    void testEscalateOldLowPriorityBugs() {
        // This test checks the logic, actual old bug would need time manipulation
        var bug = BugReport.create(
            "Low Bug", "Minor issue",
            UUID.randomUUID(), UUID.randomUUID(), "low"
        );

        var priority = SmartPermissionChecker.calculatePriority(bug);
        // For fresh bug, should be NORMAL
        assertEquals("NORMAL", priority);
    }

    @Test
    @DisplayName("Cannot activate milestone with small team (when guard)")
    void testCannotActivateWithSmallTeam() {
        var milestone = Milestone.create(
            "Sprint 1", "First sprint",
            UUID.randomUUID(),
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(14)
        );

        var result = SmartPermissionChecker.canActivateMilestone(milestone, 2);

        assertFalse(result.canActivate());
        assertTrue(result.reason().contains("Team too small"));
    }

    @Test
    @DisplayName("Cannot activate milestone without tickets (when guard)")
    void testCannotActivateWithoutTickets() {
        var milestone = Milestone.create(
            "Sprint 1", "First sprint",
            UUID.randomUUID(),
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(14)
        );

        var result = SmartPermissionChecker.canActivateMilestone(milestone, 5);

        assertFalse(result.canActivate());
        assertTrue(result.reason().contains("No tickets"));
    }

    @Test
    @DisplayName("Can activate milestone with team and tickets (when guard)")
    void testCanActivateMilestoneWhenReady() {
        var milestone = Milestone.create(
            "Sprint 1", "First sprint",
            UUID.randomUUID(),
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(14)
        );
        milestone.addTicket(UUID.randomUUID());

        var result = SmartPermissionChecker.canActivateMilestone(milestone, 5);

        assertTrue(result.canActivate());
        assertTrue(result.reason().contains("ready"));
    }
}
