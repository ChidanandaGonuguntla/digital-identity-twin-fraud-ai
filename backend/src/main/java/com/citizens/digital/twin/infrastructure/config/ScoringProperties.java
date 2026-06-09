package com.citizens.digital.twin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dti.scoring")
public record ScoringProperties(
    int minHistory,
    double blockThreshold,
    double challengeThreshold,
    double driftThreshold,
    int challengeExpirationHours,
    double impossibleSpeedKmh,
    double amountWeight,
    double geoVelocityWeight,
    double newDeviceWeight,
    double newCountryWeight,
    double unusualHourWeight,
    double newCategoryWeight,
    double categoryVelocityWeight,
    double highRiskCategoryWeight) {}
