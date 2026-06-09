package com.citizens.digital.twin.shared.exception;

import com.citizens.digital.twin.shared.api.ApiErrorResponse;
import com.citizens.digital.twin.shared.api.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> validation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ":" + e.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                ApiResult.WARNING, "Validation failed", details, request.getRequestURI()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiErrorResponse> unreadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                ApiResult.WARNING, "Malformed request body", List.of(), request.getRequestURI()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiErrorResponse> notFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiErrorResponse.of(
                ApiResult.NOT_FOUND,
                ex.getMessage() == null ? "Resource not found" : ex.getMessage(),
                List.of(),
                request.getRequestURI()));
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiErrorResponse> business(BusinessException ex, HttpServletRequest request) {
    return ResponseEntity.status(ex.status())
        .body(
            ApiErrorResponse.of(
                ApiResult.WARNING,
                ex.getMessage() == null ? "Request rejected" : ex.getMessage(),
                List.of(),
                request.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> generic(Exception ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiErrorResponse.of(
                ApiResult.FATAL,
                ex.getMessage() == null ? "Unexpected error" : ex.getMessage(),
                List.of(),
                request.getRequestURI()));
  }
}
