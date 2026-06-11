package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.AppointmentNote;
import dev.vetra.api.modules.scheduling.repository.AppointmentNoteRepository;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class CreateAppointmentNoteUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentNoteRepository appointmentNoteRepository;

    @Inject
    public CreateAppointmentNoteUseCase(AppointmentRepository appointmentRepository,
                                         AppointmentNoteRepository appointmentNoteRepository) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentNoteRepository = appointmentNoteRepository;
    }

    public Uni<AppointmentNote> execute(UUID appointmentId, String authorUserId, String title, String content) {
        return appointmentRepository.findById(appointmentId)
                .flatMap(optAppointment -> {
                    if (optAppointment.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("APPOINTMENT_NOT_FOUND", "Appointment not found"));
                    }
                    AppointmentNote note = AppointmentNote.create(appointmentId, authorUserId, title, content);
                    return appointmentNoteRepository.save(note);
                });
    }
}
