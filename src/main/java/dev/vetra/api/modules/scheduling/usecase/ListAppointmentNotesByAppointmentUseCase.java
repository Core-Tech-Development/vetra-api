package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.AppointmentNote;
import dev.vetra.api.modules.scheduling.repository.AppointmentNoteRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListAppointmentNotesByAppointmentUseCase {

    private final AppointmentNoteRepository appointmentNoteRepository;

    @Inject
    public ListAppointmentNotesByAppointmentUseCase(AppointmentNoteRepository appointmentNoteRepository) {
        this.appointmentNoteRepository = appointmentNoteRepository;
    }

    public Uni<List<AppointmentNote>> execute(UUID appointmentId) {
        return appointmentNoteRepository.findByAppointmentId(appointmentId);
    }
}
