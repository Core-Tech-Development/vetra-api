package dev.vetra.api.modules.scheduling.domain;

import java.util.Map;
import java.util.Set;

public enum AppointmentStatus {
    CREATED,
    WAITING_SPECIALIST_ACCEPTANCE,
    ACCEPTED,
    SCHEDULED,
    IN_TRANSIT,
    IN_SERVICE,
    EXAM_DONE,
    WAITING_REPORT,
    REPORT_ISSUED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> VALID_TRANSITIONS = Map.of(
            WAITING_SPECIALIST_ACCEPTANCE, Set.of(ACCEPTED, CANCELLED),
            ACCEPTED, Set.of(IN_TRANSIT, CANCELLED),
            IN_TRANSIT, Set.of(IN_SERVICE, CANCELLED),
            IN_SERVICE, Set.of(WAITING_REPORT, CANCELLED),
            WAITING_REPORT, Set.of(REPORT_ISSUED, CANCELLED),
            REPORT_ISSUED, Set.of(COMPLETED, CANCELLED)
    );

    public boolean canTransitionTo(AppointmentStatus target) {
        Set<AppointmentStatus> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
