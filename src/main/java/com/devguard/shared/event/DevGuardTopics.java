package com.devguard.shared.event;

public final class DevGuardTopics {

    public static final String SCAN_REQUESTED = "devguard.scan.requested";
    public static final String SCAN_STARTED = "devguard.scan.started";
    public static final String SCAN_COMPLETED = "devguard.scan.completed";
    public static final String SCAN_FAILED= "devguard.scan.failed";
    public static final String FINDING_DETECTED = "devguard.finding.detected";
    public static final String FINDING_VALIDATED = "devguard.finding.validated";
    public static final String SECURITY_SCORE_CHANGED = "devguard.security-score.changed";
    public static final String REPOSITORY_CONNECTED = "devguard.repository.connected";
    public static final String SCAN_DLQ = "devguard.scan.dlq";

    private DevGuardTopics() {
    }
}
