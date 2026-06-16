package dev.vetra.api.shared.cache;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class CacheTtl {

    @ConfigProperty(name = "vetra.cache.ttl.exam-type-pricing-seconds", defaultValue = "21600")
    long examTypePricingSeconds;

    @ConfigProperty(name = "vetra.cache.ttl.specialist-pricing-seconds", defaultValue = "3600")
    long specialistPricingSeconds;

    @ConfigProperty(name = "vetra.cache.ttl.specialist-profile-seconds", defaultValue = "43200")
    long specialistProfileSeconds;

    @ConfigProperty(name = "vetra.cache.ttl.clinic-profile-seconds", defaultValue = "43200")
    long clinicProfileSeconds;

    @ConfigProperty(name = "vetra.cache.ttl.dashboard-seconds", defaultValue = "300")
    long dashboardSeconds;

    @ConfigProperty(name = "vetra.cache.ttl.keycloak-token-seconds", defaultValue = "240")
    long keycloakTokenSeconds;

    public Duration examTypePricing() { return Duration.ofSeconds(examTypePricingSeconds); }
    public Duration specialistPricing() { return Duration.ofSeconds(specialistPricingSeconds); }
    public Duration specialistProfile() { return Duration.ofSeconds(specialistProfileSeconds); }
    public Duration clinicProfile() { return Duration.ofSeconds(clinicProfileSeconds); }
    public Duration dashboard() { return Duration.ofSeconds(dashboardSeconds); }
    public Duration keycloakToken() { return Duration.ofSeconds(keycloakTokenSeconds); }
}
