package dev.vetra.api.modules.scheduling.usecase;

import dev.vetra.api.modules.scheduling.domain.AvailabilitySlot;
import dev.vetra.api.modules.scheduling.dto.CreateBulkSlotsRequest;
import dev.vetra.api.modules.scheduling.repository.AvailabilitySlotRepository;
import dev.vetra.api.shared.exception.BusinessException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates multiple availability slots in bulk for a specialist based on
 * a recurrence pattern (date range, days of week, time window, timezone).
 * Optionally splits each day's time window into individual slots of a given duration.
 * All slots share a common recurrenceGroupId and are saved atomically.
 */
@ApplicationScoped
public class CreateBulkSlotsUseCase {

    private static final Logger LOG = Logger.getLogger(CreateBulkSlotsUseCase.class);
    private static final long MAX_RANGE_DAYS = 90;
    private static final int MIN_SLOT_DURATION_MINUTES = 15;
    private static final int MAX_SLOT_DURATION_MINUTES = 480;

    private final AvailabilitySlotRepository slotRepository;

    @Inject
    public CreateBulkSlotsUseCase(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public Uni<List<AvailabilitySlot>> execute(UUID specialistId, CreateBulkSlotsRequest request) {
        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(request.startTime());
            endTime = LocalTime.parse(request.endTime());
        } catch (DateTimeParseException e) {
            return Uni.createFrom().failure(
                    new BusinessException("INVALID_TIME_FORMAT", "Start time and end time must be in HH:mm format")
            );
        }

        if (!endTime.isAfter(startTime)) {
            return Uni.createFrom().failure(
                    new BusinessException("INVALID_SLOT_TIMES", "End time must be after start time")
            );
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(request.timezone());
        } catch (Exception e) {
            return Uni.createFrom().failure(
                    new BusinessException("INVALID_TIMEZONE", "Invalid timezone: " + request.timezone())
            );
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(request.startDate());
            endDate = LocalDate.parse(request.endDate());
        } catch (DateTimeParseException e) {
            return Uni.createFrom().failure(
                    new BusinessException("INVALID_DATE_FORMAT", "Dates must be in YYYY-MM-DD format")
            );
        }

        if (endDate.isBefore(startDate)) {
            return Uni.createFrom().failure(
                    new BusinessException("INVALID_DATE_RANGE", "End date must be on or after start date")
            );
        }

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > MAX_RANGE_DAYS) {
            return Uni.createFrom().failure(
                    new BusinessException("DATE_RANGE_TOO_LARGE",
                            "Date range cannot exceed " + MAX_RANGE_DAYS + " days")
            );
        }

        Integer durationMinutes = request.slotDurationMinutes();
        if (durationMinutes != null) {
            if (durationMinutes < MIN_SLOT_DURATION_MINUTES) {
                return Uni.createFrom().failure(
                        new BusinessException("INVALID_SLOT_DURATION",
                                "Slot duration must be at least " + MIN_SLOT_DURATION_MINUTES + " minutes")
                );
            }
            if (durationMinutes > MAX_SLOT_DURATION_MINUTES) {
                return Uni.createFrom().failure(
                        new BusinessException("INVALID_SLOT_DURATION",
                                "Slot duration cannot exceed " + MAX_SLOT_DURATION_MINUTES + " minutes")
                );
            }
            long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
            if (durationMinutes > totalMinutes) {
                return Uni.createFrom().failure(
                        new BusinessException("SLOT_DURATION_TOO_LARGE",
                                "Slot duration (" + durationMinutes + "min) exceeds the time window (" + totalMinutes + "min)")
                );
            }
        }

        Set<DayOfWeek> targetDays;
        try {
            targetDays = request.daysOfWeek().stream()
                    .map(day -> DayOfWeek.valueOf(day.toUpperCase()))
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().failure(
                    new BusinessException("INVALID_DAY_OF_WEEK",
                            "Invalid day of week. Use: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY")
            );
        }

        UUID recurrenceGroupId = UUID.randomUUID();
        List<AvailabilitySlot> slots = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (targetDays.contains(current.getDayOfWeek())) {
                if (durationMinutes != null) {
                    // Split into individual slots of the given duration
                    LocalTime slotStart = startTime;
                    while (true) {
                        LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);
                        if (slotEnd.isAfter(endTime)) {
                            break;
                        }

                        ZonedDateTime zonedStart = current.atTime(slotStart).atZone(zoneId);
                        ZonedDateTime zonedEnd = current.atTime(slotEnd).atZone(zoneId);

                        slots.add(AvailabilitySlot.create(
                                specialistId, zonedStart.toInstant(), zonedEnd.toInstant(),
                                request.label(), recurrenceGroupId
                        ));

                        slotStart = slotEnd;
                    }
                } else {
                    // Single slot for the entire time window
                    ZonedDateTime zonedStart = current.atTime(startTime).atZone(zoneId);
                    ZonedDateTime zonedEnd = current.atTime(endTime).atZone(zoneId);

                    Instant slotStartAt = zonedStart.toInstant();
                    Instant slotEndAt = zonedEnd.toInstant();

                    slots.add(AvailabilitySlot.create(
                            specialistId, slotStartAt, slotEndAt, request.label(), recurrenceGroupId
                    ));
                }
            }
            current = current.plusDays(1);
        }

        if (slots.isEmpty()) {
            return Uni.createFrom().failure(
                    new BusinessException("NO_SLOTS_GENERATED",
                            "No slots match the given days of week within the date range")
            );
        }

        LOG.infof("Creating bulk slots: specialistId=%s, count=%d, recurrenceGroupId=%s, durationMinutes=%s",
                specialistId, slots.size(), recurrenceGroupId,
                durationMinutes != null ? durationMinutes : "full-window");

        return slotRepository.saveBatch(slots);
    }
}
