package com.devguard.scan;

import java.util.Set;

public enum ScanStatus {

    REQUESTED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    private static final Set<ScanStatus> TERMINAL =
            Set.of(COMPLETED, FAILED, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(ScanStatus next) {
        if (isTerminal()) {
            return false;
        }
        if (next == CANCELLED) {
            return true;
        }
        return switch (this) {
            case REQUESTED -> next == QUEUED || next == RUNNING || next == FAILED;
            case QUEUED -> next == RUNNING || next == FAILED;
            case RUNNING -> next == COMPLETED || next == FAILED;
            default -> false;
        };
    }
}
