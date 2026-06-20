package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class DeleteLaudoUseCaseTest {

    private LaudoRepository laudoRepository;
    private ExamFileRepository examFileRepository;
    private DeleteLaudoUseCase useCase;

    private static final UUID LAUDO_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        laudoRepository = mock(LaudoRepository.class);
        examFileRepository = mock(ExamFileRepository.class);
        useCase = new DeleteLaudoUseCase(laudoRepository, examFileRepository);
    }

    @Test
    void shouldFailWhenLaudoNotFound() {
        when(laudoRepository.findById(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(LAUDO_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldFailWhenLaudoHasExamFiles() {
        var laudo = Laudo.restore(LAUDO_ID, UUID.randomUUID(), UUID.randomUUID(),
                LaudoStatus.DRAFT, null, null, null, null,
                null, Instant.now(), Instant.now());

        when(laudoRepository.findById(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(laudo)));
        when(examFileRepository.countByLaudoId(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(3L));

        useCase.execute(LAUDO_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(BusinessException.class);
    }

    @Test
    void shouldDeleteLaudoWhenNoExamFiles() {
        var laudo = Laudo.restore(LAUDO_ID, UUID.randomUUID(), UUID.randomUUID(),
                LaudoStatus.DRAFT, null, null, null, null,
                null, Instant.now(), Instant.now());

        when(laudoRepository.findById(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(laudo)));
        when(examFileRepository.countByLaudoId(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(0L));
        when(laudoRepository.deleteById(LAUDO_ID))
                .thenReturn(Uni.createFrom().item(true));

        useCase.execute(LAUDO_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        verify(laudoRepository).deleteById(LAUDO_ID);
    }
}
