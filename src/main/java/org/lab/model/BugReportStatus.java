package org.lab.model;

public enum BugReportStatus {
    NEW("New"),
    FIXED("Fixed"),
    TESTED("Tested"),
    CLOSED("Closed");

    private final String displayName;

    BugReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean canTransitionTo(BugReportStatus newStatus) {
        return switch (this) {
            case NEW -> newStatus == FIXED;
            case FIXED -> newStatus == TESTED;
            case TESTED -> newStatus == CLOSED;
            case CLOSED -> false;
        };
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
