package dev.vetra.api.modules.imaging.usecase;

import dev.vetra.api.modules.imaging.domain.ExamFile;
import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import dev.vetra.api.modules.imaging.service.MinioStorageService;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class DeleteExamFileUseCaseTest {

    private ExamFileRepository examFileRepository;
    private MinioStorageService storageService;
    private DeleteExamFileUseCase useCase;

    private static final UUID FILE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examFileRepository = mock(ExamFileRepository.class);
        storageService = mock(MinioStorageService.class);
        useCase = new DeleteExamFileUseCase(examFileRepository, storageService);
    }

    @Test
    void shouldFailWhenFileNotFound() {
        when(examFileRepository.findById(FILE_ID))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        useCase.execute(FILE_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(NotFoundException.class);
    }

    @Test
    void shouldDeleteFileFromStorageAndDatabase() throws Exception {
        var file = ExamFile.restore(FILE_ID, UUID.randomUUID(), "xray.jpg", "RADIOGRAPHY",
                "image/jpeg", "exams/apt-1/uuid_xray.jpg", 1024L, "specialist-1", Instant.now());

        when(examFileRepository.findById(FILE_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(file)));
        doNothing().when(storageService).deleteFile("exams/apt-1/uuid_xray.jpg");
        when(examFileRepository.deleteById(FILE_ID))
                .thenReturn(Uni.createFrom().item(true));

        useCase.execute(FILE_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(5));

        verify(storageService).deleteFile("exams/apt-1/uuid_xray.jpg");
        verify(examFileRepository).deleteById(FILE_ID);
    }

    @Test
    void shouldFailWhenStorageDeleteFails() throws Exception {
        var file = ExamFile.restore(FILE_ID, UUID.randomUUID(), "xray.jpg", "RADIOGRAPHY",
                "image/jpeg", "exams/apt-1/uuid_xray.jpg", 1024L, "specialist-1", Instant.now());

        when(examFileRepository.findById(FILE_ID))
                .thenReturn(Uni.createFrom().item(Optional.of(file)));
        doThrow(new RuntimeException("MinIO unavailable"))
                .when(storageService).deleteFile("exams/apt-1/uuid_xray.jpg");

        useCase.execute(FILE_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(RuntimeException.class);
    }
}
