package org.lab.model;

public enum TicketStatus {
    NEW("New"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean canTransitionTo(TicketStatus newStatus) {
        return switch (this) {
            case NEW -> newStatus == ACCEPTED;
            case ACCEPTED -> newStatus == IN_PROGRESS;
            case IN_PROGRESS -> newStatus == COMPLETED;
            case COMPLETED -> false;
        };
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
