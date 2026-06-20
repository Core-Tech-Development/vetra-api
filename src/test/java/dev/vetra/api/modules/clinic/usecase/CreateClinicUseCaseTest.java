package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.domain.ClinicStatus;
import dev.vetra.api.modules.clinic.dto.CreateClinicRequest;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.shared.exception.DuplicateException;
import dev.vetra.api.shared.security.KeycloakAdminService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreateClinicUseCaseTest {

    private ClinicRepository clinicRepository;
    private KeycloakAdminService keycloakAdminService;
    private CreateClinicUseCase useCase;

    @BeforeEach
    void setUp() {
        clinicRepository = mock(ClinicRepository.class);
        keycloakAdminService = mock(KeycloakAdminService.class);
        useCase = new CreateClinicUseCase(clinicRepository, keycloakAdminService);
    }

    @Test
    void shouldFailWhenDocumentAlreadyExists() {
        var request = new CreateClinicRequest("Clinica Test", "12345678000199",
                "test@test.com", "11999999999", "Rua A", "SP", "SP", "password123");
        var existing = Clinic.restore(UUID.randomUUID(), "Existing", "12345678000199",
                "existing@test.com", "11888888888", "Rua B", "SP", "SP",
                ClinicStatus.ACTIVE, Instant.now(), Instant.now());

        when(clinicRepository.findByDocument("12345678000199"))
                .thenReturn(Uni.createFrom().item(Optional.of(existing)));

        useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(DuplicateException.class);
    }

    @Test
    void shouldCreateClinicWithPendingApprovalStatus() {
        var request = new CreateClinicRequest("Clinica Nova", "98765432000199",
                "nova@test.com", "11999999999", "Rua C", "SP", "SP", "password123");

        when(clinicRepository.findByDocument("98765432000199"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(clinicRepository.save(any(Clinic.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Clinic) inv.getArgument(0)));
        when(keycloakAdminService.createUser(eq("nova@test.com"), eq("password123"), eq("Clinica Nova"), anyList()))
                .thenReturn(Uni.createFrom().item("keycloak-user-id"));

        var result = useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.name()).isEqualTo("Clinica Nova");
        assertThat(result.document()).isEqualTo("98765432000199");
        assertThat(result.status()).isEqualTo(ClinicStatus.PENDING_APPROVAL);
        verify(clinicRepository).save(any(Clinic.class));
        verify(keycloakAdminService).createUser(eq("nova@test.com"), eq("password123"), eq("Clinica Nova"), anyList());
    }

    @Test
    void shouldCreateClinicEvenIfKeycloakFails() {
        var request = new CreateClinicRequest("Clinica Fallback", "11222333000144",
                "fallback@test.com", "11999999999", null, "SP", "SP", "password123");

        when(clinicRepository.findByDocument("11222333000144"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        when(clinicRepository.save(any(Clinic.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Clinic) inv.getArgument(0)));
        when(keycloakAdminService.createUser(any(), any(), any(), anyList()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Keycloak unavailable")));

        var result = useCase.execute(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .getItem();

        assertThat(result.name()).isEqualTo("Clinica Fallback");
        assertThat(result.status()).isEqualTo(ClinicStatus.PENDING_APPROVAL);
    }
}
