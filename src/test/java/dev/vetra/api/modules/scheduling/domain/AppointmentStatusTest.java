package dev.vetra.api.modules.scheduling.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    @Test
    void shouldAllowTransitionFromWaitingToAccepted() {
        assertThat(AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE.canTransitionTo(AppointmentStatus.ACCEPTED)).isTrue();
    }

    @Test
    void shouldAllowTransitionFromWaitingToCancelled() {
        assertThat(AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
    }

    @Test
    void shouldAllowTransitionFromAcceptedToInTransit() {
        assertThat(AppointmentStatus.ACCEPTED.canTransitionTo(AppointmentStatus.IN_TRANSIT)).isTrue();
    }

    @Test
    void shouldAllowTransitionFromInTransitToInService() {
        assertThat(AppointmentStatus.IN_TRANSIT.canTransitionTo(AppointmentStatus.IN_SERVICE)).isTrue();
    }

    @Test
    void shouldAllowTransitionFromInServiceToWaitingReport() {
        assertThat(AppointmentStatus.IN_SERVICE.canTransitionTo(AppointmentStatus.WAITING_REPORT)).isTrue();
    }

    @Test
    void shouldAllowTransitionFromWaitingReportToReportIssued() {
        assertThat(AppointmentStatus.WAITING_REPORT.canTransitionTo(AppointmentStatus.REPORT_ISSUED)).isTrue();
    }

    @Test
    void shouldAllowTransitionFromReportIssuedToCompleted() {
        assertThat(AppointmentStatus.REPORT_ISSUED.canTransitionTo(AppointmentStatus.COMPLETED)).isTrue();
    }

    @Test
    void shouldAllowCancellationFromAnyActiveStatus() {
        assertThat(AppointmentStatus.ACCEPTED.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
        assertThat(AppointmentStatus.IN_TRANSIT.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
        assertThat(AppointmentStatus.IN_SERVICE.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
        assertThat(AppointmentStatus.WAITING_REPORT.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
        assertThat(AppointmentStatus.REPORT_ISSUED.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
    }

    @Test
    void shouldNotAllowTransitionFromWaitingToInTransit() {
        assertThat(AppointmentStatus.WAITING_SPECIALIST_ACCEPTANCE.canTransitionTo(AppointmentStatus.IN_TRANSIT)).isFalse();
    }

    @Test
    void shouldNotAllowTransitionFromAcceptedToInService() {
        assertThat(AppointmentStatus.ACCEPTED.canTransitionTo(AppointmentStatus.IN_SERVICE)).isFalse();
    }

    @Test
    void shouldNotAllowTransitionFromTerminalStatuses() {
        assertThat(AppointmentStatus.COMPLETED.canTransitionTo(AppointmentStatus.CANCELLED)).isFalse();
        assertThat(AppointmentStatus.CANCELLED.canTransitionTo(AppointmentStatus.ACCEPTED)).isFalse();
        assertThat(AppointmentStatus.NO_SHOW.canTransitionTo(AppointmentStatus.CANCELLED)).isFalse();
    }

    @Test
    void shouldIdentifyTerminalStatuses() {
        assertThat(AppointmentStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(AppointmentStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(AppointmentStatus.NO_SHOW.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"WAITING_SPECIALIST_ACCEPTANCE", "ACCEPTED", "IN_TRANSIT", "IN_SERVICE", "WAITING_REPORT", "REPORT_ISSUED"})
    void shouldNotBeTerminalForActiveStatuses(AppointmentStatus status) {
        assertThat(status.isTerminal()).isFalse();
    }
}
