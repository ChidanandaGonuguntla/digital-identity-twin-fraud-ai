package com.citizens.digital.twin.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    String result, String message, List<String> details, String timestamp, String path) {

  public static ApiErrorResponse of(
      ApiResult result, String message, List<String> details, String path) {
    return new ApiErrorResponse(
        result.name(),
        message,
        details == null ? List.of() : List.copyOf(details),
        Instant.now().toString(),
        path);
  }
}
