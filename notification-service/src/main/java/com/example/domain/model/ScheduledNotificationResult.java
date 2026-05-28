package com.example.domain.model;

public record ScheduledNotificationResult(
        int candidates,
        int sent,
        int skipped,
        int failed) {
    public static ScheduledNotificationResult empty() {
        return new ScheduledNotificationResult(0, 0, 0, 0);
    }
}
