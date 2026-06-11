package dev.vetra.api.modules.exam.domain;

public enum ExamRequestStatus {
    CREATED,
    PENDING_SPECIALIST,
    SPECIALIST_ASSIGNED,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
