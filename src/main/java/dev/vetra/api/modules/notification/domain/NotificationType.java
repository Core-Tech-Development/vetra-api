package dev.vetra.api.modules.notification.domain;

/**
 * Constants for notification types used across the application.
 * Pure Java class — no framework annotations.
 */
public final class NotificationType {

    private NotificationType() {
        // utility class
    }

    public static final String EXAM_REQUEST_CREATED = "EXAM_REQUEST_CREATED";
    public static final String APPOINTMENT_SCHEDULED = "APPOINTMENT_SCHEDULED";
    public static final String APPOINTMENT_ACCEPTED = "APPOINTMENT_ACCEPTED";
    public static final String APPOINTMENT_DECLINED = "APPOINTMENT_DECLINED";
    public static final String SPECIALIST_IN_TRANSIT = "SPECIALIST_IN_TRANSIT";
    public static final String EXAM_IN_SERVICE = "EXAM_IN_SERVICE";
    public static final String EXAM_COMPLETED = "EXAM_COMPLETED";
    public static final String LAUDO_ISSUED = "LAUDO_ISSUED";
    public static final String APPOINTMENT_COMPLETED = "APPOINTMENT_COMPLETED";
    public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
    public static final String APPOINTMENT_NO_SHOW = "APPOINTMENT_NO_SHOW";
    public static final String CLINIC_APPROVED = "CLINIC_APPROVED";
    public static final String SPECIALIST_APPROVED = "SPECIALIST_APPROVED";
    public static final String BILLING_RECORD_CREATED = "BILLING_RECORD_CREATED";
}
