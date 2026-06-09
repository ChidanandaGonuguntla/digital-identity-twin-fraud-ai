package com.citizens.digital.twin.api.dto;

public record ChampionChallengerSummaryResponse(
    String championVersion,
    String challengerVersion,
    long scoredEvents,
    double avgChampionScore,
    double avgChallengerScore,
    double avgScoreDelta,
    double agreementRate) {}
