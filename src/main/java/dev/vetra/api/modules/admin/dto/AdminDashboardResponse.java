package dev.vetra.api.modules.admin.dto;

public record AdminDashboardResponse(
        long totalClinics,
        long totalSpecialists,
        long pendingClinicApprovals,
        long pendingSpecialistApprovals,
        long totalAppointmentsToday,
        long totalActiveAppointments
) {
}
