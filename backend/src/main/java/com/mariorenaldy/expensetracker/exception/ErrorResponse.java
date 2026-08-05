package com.mariorenaldy.expensetracker.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(Instant timestamp, int status, String message, List<FieldError> errors) {
    public record FieldError(String field, String message) {}
}

