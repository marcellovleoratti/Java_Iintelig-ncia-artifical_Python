package com.example.aml.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnalysisRequest(
        @NotBlank String customerId,
        @NotEmpty List<@Valid Transaction> transactions) {
}
