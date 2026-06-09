package com.citizens.digital.twin.api.dto;

public record ScoreAttributionResponse(
    double rulePoints,
    double twinPoints,
    double mlPoints,
    double graphPoints,
    double finalScore,
    double mlProbability) {}
