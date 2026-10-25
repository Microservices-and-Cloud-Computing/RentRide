package com.rentride.vehicleservice.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rentride.vehicleservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handling advice for REST controllers in the Vehicle Service.
 * Intercepts validation errors, resource conflicts, authorization rejections, and not found conditions.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation errors resulting from {@code @Valid} request payload bindings.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Validation failure on path '{}': {}", request.getRequestURI(), errors);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Validation failed for one or more fields")
                .path(request.getRequestURI())
                .errors(errors)
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles constraint violation exceptions triggered on query/path parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                    : propertyPath;
            errors.put(fieldName, violation.getMessage());
        }

        log.warn("Constraint violation on path '{}': {}", request.getRequestURI(), errors);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Validation failed for one or more parameters")
                .path(request.getRequestURI())
                .errors(errors)
                .validationErrors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles resource not found exceptions (HTTP 404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found on path '{}': {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles duplicate unique resource conflicts (HTTP 409).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.warn("Duplicate resource conflict on path '{}': {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles invalid or illegal vehicle operational status transitions (HTTP 409 Conflict).
     */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException(
            InvalidStateTransitionException ex,
            HttpServletRequest request) {

        log.warn("Invalid vehicle state transition on path '{}': {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles unauthorized access and resource ownership violations (HTTP 403).
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex,
            HttpServletRequest request) {

        log.warn("Unauthorized access on path '{}': {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handles missing required HTTP request headers (e.g. X-User-Id).
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex,
            HttpServletRequest request) {

        log.warn("Missing required header '{}' on path '{}'", ex.getHeaderName(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Required request header '" + ex.getHeaderName() + "' is missing")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles malformed or unreadable JSON request bodies (e.g., invalid enum values or syntax errors).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String userFriendlyMessage = "Malformed or unreadable JSON request body";

        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().isEmpty() ? "field" : ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String allowedValues = Arrays.toString(targetType.getEnumConstants());
                userFriendlyMessage = String.format("Invalid value '%s' for %s. Accepted values: %s",
                        ife.getValue(), fieldName, allowedValues);
            } else {
                userFriendlyMessage = String.format("Invalid value '%s' for field '%s'", ife.getValue(), fieldName);
            }
        }

        log.warn("Malformed HTTP request body on path '{}': {}", request.getRequestURI(), userFriendlyMessage);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(userFriendlyMessage)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles unsupported HTTP methods on endpoints (e.g. POST instead of GET).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not allowed on path '{}': {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handles requests to non-existent URLs/resources (404 Not Found).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found on path '{}': {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message("The requested endpoint was not found: " + request.getRequestURI())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Catch-all handler for unhandled runtime exceptions (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception processing path '{}': ", request.getRequestURI(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected internal error occurred")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
