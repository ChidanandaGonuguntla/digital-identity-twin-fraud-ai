package com.citizens.dti.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized scoring weights and thresholds so risk tuning never requires a redeploy. Bound from
 * the {@code dti.scoring.*} section of application.yml.
 *
 * @param minHistory transactions required before the twin is trusted to score
 * @param amountWeight points contributed by an anomalous amount
 * @param geoVelocityWeight points contributed by impossible-travel geo-velocity
 * @param newDeviceWeight points contributed by an unseen device fingerprint
 * @param unusualHourWeight points contributed by an out-of-pattern transaction hour
 * @param newCategoryWeight points contributed by an unseen merchant category
 * @param challengeThreshold score at/above which step-up auth is required
 * @param blockThreshold score at/above which the transaction is rejected
 * @param impossibleSpeedKmh implied travel speed treated as physically impossible
 */
@ConfigurationProperties(prefix = "dti.scoring")
public record ScoringProperties(
    long minHistory,
    double amountWeight,
    double geoVelocityWeight,
    double newDeviceWeight,
    double unusualHourWeight,
    double newCategoryWeight,
    double challengeThreshold,
    double blockThreshold,
    double impossibleSpeedKmh,
    double categoryVelocityWeight,
    double highRiskCategoryWeight) {}
