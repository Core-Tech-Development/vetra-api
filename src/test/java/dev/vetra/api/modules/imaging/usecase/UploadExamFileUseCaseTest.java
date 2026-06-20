package dev.vetra.api.modules.imaging.usecase;

import dev.vetra.api.modules.imaging.domain.ExamFile;
import dev.vetra.api.modules.imaging.repository.ExamFileRepository;
import dev.vetra.api.modules.imaging.service.MinioStorageService;
import dev.vetra.api.modules.audit.usecase.LogAuditEventUseCase;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UploadExamFileUseCaseTest {

    private ExamFileRepository examFileRepository;
    private MinioStorageService storageService;
    private LogAuditEventUseCase auditUseCase;
    private UploadExamFileUseCase useCase;

    private static final UUID APPOINTMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examFileRepository = mock(ExamFileRepository.class);
        storageService = mock(MinioStorageService.class);
        auditUseCase = mock(LogAuditEventUseCase.class);
        useCase = new UploadExamFileUseCase(examFileRepository, storageService, auditUseCase);
    }

    @Test
    void shouldUploadFileAndSaveMetadata() throws Exception {
        var inputStream = new ByteArrayInputStream("file-content".getBytes());

        doNothing().when(storageService).uploadFile(anyString(), any(), anyLong(), anyString());
        when(examFileRepository.save(any(ExamFile.class)))
                .thenAnswer(inv -> Uni.createFrom().item((ExamFile) inv.getArgument(0)));

        var result = useCase.execute(APPOINTMENT_ID, "xray.jpg", "RADIOGRAPHY",
                        "image/jpeg", inputStream, 1024L, "specialist-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(result.appointmentId()).isEqualTo(APPOINTMENT_ID);
        assertThat(result.fileName()).isEqualTo("xray.jpg");
        assertThat(result.fileType()).isEqualTo("RADIOGRAPHY");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.sizeBytes()).isEqualTo(1024L);
        assertThat(result.uploadedBy()).isEqualTo("specialist-1");
        assertThat(result.storageKey()).startsWith("exams/" + APPOINTMENT_ID + "/");
        verify(storageService).uploadFile(anyString(), any(), eq(1024L), eq("image/jpeg"));
        verify(examFileRepository).save(any(ExamFile.class));
    }

    @Test
    void shouldFailWhenStorageUploadFails() throws Exception {
        var inputStream = new ByteArrayInputStream("file-content".getBytes());

        doThrow(new RuntimeException("MinIO unavailable"))
                .when(storageService).uploadFile(anyString(), any(), anyLong(), anyString());

        useCase.execute(APPOINTMENT_ID, "xray.jpg", "RADIOGRAPHY",
                        "image/jpeg", inputStream, 1024L, "specialist-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure(Duration.ofSeconds(5))
                .assertFailedWith(RuntimeException.class);
    }
}
