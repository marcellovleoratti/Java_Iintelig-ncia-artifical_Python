package com.example.aml.model;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisResult(
        String customerId,
        boolean movedInBand,
        boolean flagged,
        int riskScore,
        int transactionsInBand,
        BigDecimal totalInBand,
        List<String> reasons,
        String aiExplanation,
        String engine) {
}
