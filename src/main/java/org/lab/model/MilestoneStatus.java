package org.lab.model;

public enum MilestoneStatus {
    OPEN("Open"),
    ACTIVE("Active"),
    CLOSED("Closed");

    private final String displayName;

    MilestoneStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean canTransitionTo(MilestoneStatus newStatus) {
        return switch (this) {
            case OPEN -> newStatus == ACTIVE;
            case ACTIVE -> newStatus == CLOSED;
            case CLOSED -> false;
        };
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
