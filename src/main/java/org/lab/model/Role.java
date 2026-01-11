package org.lab.model;

public sealed interface Role permits Role.Manager, Role.TeamLeader, Role.Developer, Role.Tester {

    String displayName();

    record Manager() implements Role {
        @Override
        public String displayName() {
            return "Manager";
        }
    }

    record TeamLeader() implements Role {
        @Override
        public String displayName() {
            return "Team Leader";
        }
    }

    record Developer() implements Role {
        @Override
        public String displayName() {
            return "Developer";
        }
    }

    record Tester() implements Role {
        @Override
        public String displayName() {
            return "Tester";
        }
    }

    static Role fromString(String role) {
        return switch (role.toLowerCase()) {
            case "manager" -> new Manager();
            case "teamleader", "team leader" -> new TeamLeader();
            case "developer" -> new Developer();
            case "tester" -> new Tester();
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
    }
}
