package dev.vetra.api.shared.config;

import io.quarkus.runtime.annotations.RegisterForReflection;

import dev.vetra.api.shared.pagination.PageResponse;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.exception.ErrorResponse;
import dev.vetra.api.modules.clinic.dto.ClinicResponse;
import dev.vetra.api.modules.clinic.dto.CreateClinicRequest;
import dev.vetra.api.modules.specialist.dto.SpecialistResponse;
import dev.vetra.api.modules.specialist.dto.CreateSpecialistRequest;
import dev.vetra.api.modules.specialist.dto.UpdateSpecialistRequest;
import dev.vetra.api.modules.specialist.dto.CoverageAreaResponse;
import dev.vetra.api.modules.specialist.dto.AddCoverageAreaRequest;
import dev.vetra.api.modules.tutor.dto.TutorResponse;
import dev.vetra.api.modules.tutor.dto.CreateTutorRequest;
import dev.vetra.api.modules.tutor.dto.UpdateTutorRequest;
import dev.vetra.api.modules.patient.dto.PatientResponse;
import dev.vetra.api.modules.patient.dto.CreatePatientRequest;
import dev.vetra.api.modules.patient.dto.UpdatePatientRequest;
import dev.vetra.api.modules.scheduling.dto.SlotResponse;
import dev.vetra.api.modules.scheduling.dto.CreateSlotRequest;
import dev.vetra.api.modules.scheduling.dto.AppointmentResponse;
import dev.vetra.api.modules.scheduling.dto.CreateAppointmentRequest;
import dev.vetra.api.modules.scheduling.dto.CancelAppointmentRequest;
import dev.vetra.api.modules.exam.dto.ExamRequestResponse;
import dev.vetra.api.modules.exam.dto.CreateExamRequestRequest;
import dev.vetra.api.modules.exam.dto.UpdateExamRequestRequest;
import dev.vetra.api.modules.imaging.dto.ExamFileResponse;
import dev.vetra.api.modules.laudo.dto.LaudoResponse;
import dev.vetra.api.modules.laudo.dto.CreateLaudoRequest;
import dev.vetra.api.modules.laudo.dto.UpdateLaudoRequest;
import dev.vetra.api.modules.scheduling.dto.AppointmentNoteResponse;
import dev.vetra.api.modules.scheduling.dto.CreateAppointmentNoteRequest;
import dev.vetra.api.modules.admin.dto.AdminDashboardResponse;
import dev.vetra.api.modules.audit.dto.AuditLogResponse;
import dev.vetra.api.modules.notification.dto.NotificationResponse;

/**
 * Registers all DTO/record classes for reflection in GraalVM native image.
 * Jackson needs reflection metadata to serialize/deserialize these types.
 */
@RegisterForReflection(targets = {
        PageResponse.class,
        PageRequest.class,
        ErrorResponse.class,
        ErrorResponse.FieldError.class,
        ClinicResponse.class,
        CreateClinicRequest.class,
        SpecialistResponse.class,
        CreateSpecialistRequest.class,
        UpdateSpecialistRequest.class,
        CoverageAreaResponse.class,
        AddCoverageAreaRequest.class,
        TutorResponse.class,
        CreateTutorRequest.class,
        UpdateTutorRequest.class,
        PatientResponse.class,
        CreatePatientRequest.class,
        UpdatePatientRequest.class,
        SlotResponse.class,
        CreateSlotRequest.class,
        AppointmentResponse.class,
        CreateAppointmentRequest.class,
        CancelAppointmentRequest.class,
        ExamRequestResponse.class,
        CreateExamRequestRequest.class,
        UpdateExamRequestRequest.class,
        ExamFileResponse.class,
        LaudoResponse.class,
        CreateLaudoRequest.class,
        UpdateLaudoRequest.class,
        AppointmentNoteResponse.class,
        CreateAppointmentNoteRequest.class,
        AdminDashboardResponse.class,
        AuditLogResponse.class,
        NotificationResponse.class,
})
public class NativeReflectionConfig {
}
