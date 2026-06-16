package dev.vetra.api.shared.cache;

import java.util.UUID;

/**
 * Centralized cache key definitions.
 * All keys follow pattern: {module}:{entity}:{identifier}
 */
public final class CacheKeys {

    private CacheKeys() {}

    // Tier 1: Pricing
    public static String examTypePricing(String examType) {
        return "pricing:exam-type:" + examType;
    }

    public static String examTypePricingAllActive() {
        return "pricing:exam-type:all-active";
    }

    public static String specialistPricing(UUID specialistId, String examType) {
        return "pricing:specialist:" + specialistId + ":" + examType;
    }

    // Tier 2: Entity lookups
    public static String specialist(UUID id) {
        return "specialist:" + id;
    }

    public static String clinic(UUID id) {
        return "clinic:" + id;
    }

    // Tier 3: Dashboards
    public static String adminDashboard() {
        return "dashboard:admin";
    }

    public static String billingDashboard() {
        return "dashboard:billing";
    }

    // Tier 4: Keycloak
    public static String keycloakAdminToken() {
        return "keycloak:admin-token";
    }

    // Invalidation patterns
    public static String allExamTypePricing() {
        return "pricing:exam-type:*";
    }

    public static String allSpecialistPricing(UUID specialistId) {
        return "pricing:specialist:" + specialistId + ":*";
    }

    public static String allDashboards() {
        return "dashboard:*";
    }
}
