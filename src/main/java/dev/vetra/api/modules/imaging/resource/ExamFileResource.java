package dev.vetra.api.modules.imaging.resource;

import dev.vetra.api.modules.imaging.dto.ExamFileMapper;
import dev.vetra.api.modules.imaging.dto.ExamFileResponse;
import dev.vetra.api.modules.imaging.usecase.DeleteExamFileUseCase;
import dev.vetra.api.modules.imaging.usecase.GetFileDownloadUrlUseCase;
import dev.vetra.api.modules.imaging.usecase.ListExamFilesUseCase;
import dev.vetra.api.modules.imaging.usecase.UploadExamFileUseCase;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Exam Files", description = "Exam file upload and management endpoints")
public class ExamFileResource {

    private final UploadExamFileUseCase uploadExamFileUseCase;
    private final ListExamFilesUseCase listExamFilesUseCase;
    private final GetFileDownloadUrlUseCase getFileDownloadUrlUseCase;
    private final DeleteExamFileUseCase deleteExamFileUseCase;

    @Inject
    public ExamFileResource(UploadExamFileUseCase uploadExamFileUseCase,
                            ListExamFilesUseCase listExamFilesUseCase,
                            GetFileDownloadUrlUseCase getFileDownloadUrlUseCase,
                            DeleteExamFileUseCase deleteExamFileUseCase) {
        this.uploadExamFileUseCase = uploadExamFileUseCase;
        this.listExamFilesUseCase = listExamFilesUseCase;
        this.getFileDownloadUrlUseCase = getFileDownloadUrlUseCase;
        this.deleteExamFileUseCase = deleteExamFileUseCase;
    }

    @POST
    @Path("/appointments/{appointmentId}/files")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload exam file", description = "Uploads an exam file (image, video, PDF) for an appointment")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "File uploaded",
                    content = @Content(schema = @Schema(implementation = ExamFileResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid file")
    })
    public Uni<Response> upload(
            @PathParam("appointmentId")
            @Parameter(description = "Appointment UUID") UUID appointmentId,
            @RestForm("file") FileUpload file) {

        String fileName = file.fileName();
        String contentType = file.contentType();
        long size = file.size();
        String fileType = resolveFileType(contentType);

        try {
            FileInputStream inputStream = new FileInputStream(file.uploadedFile().toFile());
            // uploadedBy should come from SecurityContext in production;
            // using a placeholder for now
            String uploadedBy = "system";

            return uploadExamFileUseCase.execute(appointmentId, fileName, fileType, contentType,
                            inputStream, size, uploadedBy)
                    .map(examFile -> {
                        ExamFileResponse body = ExamFileMapper.toResponse(examFile);
                        return Response.created(URI.create("/api/v1/files/" + examFile.id()))
                                .entity(body)
                                .build();
                    });
        } catch (IOException e) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Failed to read uploaded file")
                            .build()
            );
        }
    }

    @GET
    @Path("/appointments/{appointmentId}/files")
    @Operation(summary = "List exam files", description = "Lists all exam files for an appointment")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Files listed")
    })
    public Uni<Response> listByAppointment(
            @PathParam("appointmentId")
            @Parameter(description = "Appointment UUID") UUID appointmentId) {
        return listExamFilesUseCase.execute(appointmentId)
                .map(files -> {
                    List<ExamFileResponse> responses = files.stream()
                            .map(ExamFileMapper::toResponse)
                            .toList();
                    return Response.ok(responses).build();
                });
    }

    @GET
    @Path("/files/{id}/download-url")
    @Operation(summary = "Get file download URL", description = "Generates a presigned download URL for an exam file (valid for 2 hours)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Download URL generated",
                    content = @Content(schema = @Schema(implementation = ExamFileResponse.class))),
            @APIResponse(responseCode = "404", description = "File not found")
    })
    public Uni<Response> getDownloadUrl(
            @PathParam("id")
            @Parameter(description = "Exam file UUID") UUID id) {
        return getFileDownloadUrlUseCase.execute(id)
                .map(response -> Response.ok(response).build());
    }

    @DELETE
    @Path("/files/{id}")
    @Operation(summary = "Delete exam file", description = "Deletes an exam file from storage and database")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "File deleted"),
            @APIResponse(responseCode = "404", description = "File not found")
    })
    public Uni<Response> delete(
            @PathParam("id")
            @Parameter(description = "Exam file UUID") UUID id) {
        return deleteExamFileUseCase.execute(id)
                .map(ignored -> Response.noContent().build());
    }

    private String resolveFileType(String contentType) {
        if (contentType == null) {
            return "OTHER";
        }
        if (contentType.startsWith("image/")) {
            return "IMAGE";
        }
        if (contentType.startsWith("video/")) {
            return "VIDEO";
        }
        if (contentType.equals("application/pdf")) {
            return "PDF";
        }
        return "OTHER";
    }
}
