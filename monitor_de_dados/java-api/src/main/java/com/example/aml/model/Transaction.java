package com.example.aml.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String id,
        @NotNull @Positive BigDecimal amount,
        @NotNull String currency,
        @NotNull Instant timestamp) {
}
