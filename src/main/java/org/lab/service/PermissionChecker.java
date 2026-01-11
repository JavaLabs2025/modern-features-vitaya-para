package org.lab.service;

import org.lab.model.*;

import java.util.Optional;

public class PermissionChecker {

    public sealed interface Permission permits
            Permission.CanManageUsers,
            Permission.CanManageMilestones,
            Permission.CanManageTickets,
            Permission.CanCreateTickets,
            Permission.CanWorkOnTickets,
            Permission.CanCreateBugReports,
            Permission.CanFixBugReports,
            Permission.CanTestBugReports {

        record CanManageUsers() implements Permission {}
        record CanManageMilestones() implements Permission {}
        record CanManageTickets() implements Permission {}
        record CanCreateTickets() implements Permission {}
        record CanWorkOnTickets() implements Permission {}
        record CanCreateBugReports() implements Permission {}
        record CanFixBugReports() implements Permission {}
        record CanTestBugReports() implements Permission {}
    }

    public static boolean hasPermission(Role role, Permission permission) {
        return switch (permission) {
            case Permission.CanManageUsers() -> switch (role) {
                case Role.Manager() -> true;
                default -> false;
            };

            case Permission.CanManageMilestones() -> switch (role) {
                case Role.Manager() -> true;
                default -> false;
            };

            case Permission.CanManageTickets() -> switch (role) {
                case Role.Manager(), Role.TeamLeader() -> true;
                default -> false;
            };

            case Permission.CanCreateTickets() -> switch (role) {
                case Role.Manager(), Role.TeamLeader() -> true;
                default -> false;
            };

            case Permission.CanWorkOnTickets() -> switch (role) {
                case Role.Developer(), Role.TeamLeader() -> true;
                default -> false;
            };

            case Permission.CanCreateBugReports() -> switch (role) {
                case Role.Developer(), Role.Tester() -> true;
                default -> false;
            };

            case Permission.CanFixBugReports() -> switch (role) {
                case Role.Developer() -> true;
                default -> false;
            };

            case Permission.CanTestBugReports() -> switch (role) {
                case Role.Tester() -> true;
                default -> false;
            };
        };
    }

    public static String getPermissionDescription(Permission permission) {
        return switch (permission) {
            case Permission.CanManageUsers() ->
                    "Add/remove team members and assign roles";
            case Permission.CanManageMilestones() ->
                    "Create and manage project milestones";
            case Permission.CanManageTickets() ->
                    "Create tickets and assign developers";
            case Permission.CanCreateTickets() ->
                    "Create new tickets for the project";
            case Permission.CanWorkOnTickets() ->
                    "Work on assigned tickets and update their status";
            case Permission.CanCreateBugReports() ->
                    "Report bugs found in the project";
            case Permission.CanFixBugReports() ->
                    "Fix reported bugs";
            case Permission.CanTestBugReports() ->
                    "Test and verify bug fixes";
        };
    }

    public static String getRoleDescription(Role role) {
        return switch (role) {
            case Role.Manager() -> STR."""
                Manager: Full project control
                - Manage team members
                - Create and manage milestones
                - Create and assign tickets
                - Oversee all project activities
                """;

            case Role.TeamLeader() -> STR."""
                Team Leader: Technical leadership
                - Create and assign tickets
                - Work on tickets
                - Guide development team
                - Support project manager
                """;

            case Role.Developer() -> STR."""
                Developer: Development work
                - Work on assigned tickets
                - Report and fix bugs
                - Collaborate with team
                """;

            case Role.Tester() -> STR."""
                Tester: Quality assurance
                - Test project functionality
                - Report bugs
                - Verify bug fixes
                """;
        };
    }

    public static Optional<String> validateStatusTransition(Object currentStatus, Object newStatus) {
        return switch (currentStatus) {
            case TicketStatus ts when newStatus instanceof TicketStatus newTs -> {
                if (!ts.canTransitionTo(newTs)) {
                    yield Optional.of(STR."Invalid ticket status transition: \{ts} -> \{newTs}");
                }
                yield Optional.empty();
            }

            case MilestoneStatus ms when newStatus instanceof MilestoneStatus newMs -> {
                if (!ms.canTransitionTo(newMs)) {
                    yield Optional.of(STR."Invalid milestone status transition: \{ms} -> \{newMs}");
                }
                yield Optional.empty();
            }

            case BugReportStatus bs when newStatus instanceof BugReportStatus newBs -> {
                if (!bs.canTransitionTo(newBs)) {
                    yield Optional.of(STR."Invalid bug report status transition: \{bs} -> \{newBs}");
                }
                yield Optional.empty();
            }

            default -> Optional.of("Unknown status type");
        };
    }
}
