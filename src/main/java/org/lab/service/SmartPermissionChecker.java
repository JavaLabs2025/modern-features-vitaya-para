package org.lab.service;

import org.lab.model.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class SmartPermissionChecker {

    /**
     * Smart permission checking with context-aware guards
     * Demonstrates pattern matching with when clauses for conditional logic
     */
    public static boolean canModifyTicket(User user, Role role, Ticket ticket) {
        return switch (role) {
            case Role.Manager() -> true;

            case Role.TeamLeader() when ticket.status() != TicketStatus.COMPLETED ->
                true;

            case Role.Developer() when ticket.assignedDevelopers().contains(user.id()) &&
                                      ticket.status() != TicketStatus.COMPLETED ->
                true;

            default -> false;
        };
    }

    /**
     * Smart bug priority calculation with guard patterns
     * Uses when clauses to evaluate context-aware conditions
     */
    public static String calculatePriority(BugReport bug) {
        return switch (bug.severity().toLowerCase()) {
            case String s when s.equals("critical") && bug.status() == BugReportStatus.NEW ->
                "URGENT - Immediate attention needed";

            case String s when s.equals("critical") ->
                "HIGH - Critical but already being worked on";

            case String s when s.equals("high") ->
                "MEDIUM-HIGH - Should fix soon";

            case String s when s.equals("low") &&
                              Duration.between(bug.createdAt(), LocalDateTime.now()).toDays() > 30 ->
                "MEDIUM - Old low priority, escalating";

            default -> "NORMAL";
        };
    }

    /**
     * Check if milestone can be activated with business rules
     * Uses when guards for context-aware validation
     */
    public static MilestoneCheckResult canActivateMilestone(Milestone milestone, int teamSize) {
        if (milestone.status() != MilestoneStatus.OPEN) {
            return switch (milestone.status()) {
                case ACTIVE -> new MilestoneCheckResult(false, "Milestone already active");
                case CLOSED -> new MilestoneCheckResult(false, "Milestone is closed");
                default -> new MilestoneCheckResult(false, "Invalid status");
            };
        }

        // OPEN status - use when guards for conditions
        if (teamSize < 3) {
            return new MilestoneCheckResult(false, "Team too small to activate milestone");
        }

        if (milestone.ticketIds().isEmpty()) {
            return new MilestoneCheckResult(false, "No tickets assigned to milestone");
        }

        return new MilestoneCheckResult(true, "Milestone ready to activate");
    }

    public record MilestoneCheckResult(boolean canActivate, String reason) {}
}
