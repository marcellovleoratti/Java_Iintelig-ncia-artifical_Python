package com.example.aml.service;

import com.example.aml.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Regras determinísticas: faixa 3.000–10.000 EUR, rajadas e fracionamento (structuring). */
@Component
public class RuleEngine {

    public record Outcome(int count, BigDecimal total, int score, List<String> reasons) {}

    private final BigDecimal min;
    private final BigDecimal max;

    public RuleEngine(@Value("${aml.band.min}") BigDecimal min,
                      @Value("${aml.band.max}") BigDecimal max) {
        this.min = min;
        this.max = max;
    }

    public Outcome evaluate(List<Transaction> all) {
        List<Transaction> band = all.stream()
                .filter(t -> "EUR".equalsIgnoreCase(t.currency()))
                .filter(t -> t.amount().compareTo(min) >= 0 && t.amount().compareTo(max) <= 0)
                .sorted(Comparator.comparing(Transaction::timestamp))
                .toList();

        BigDecimal total = band.stream().map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> reasons = new ArrayList<>();
        int score = Math.min(40, band.size() * 10);
        if (!band.isEmpty()) {
            reasons.add(band.size() + " movimento(s) entre " + min + " e " + max + " EUR");
        }

        boolean burst = false;
        boolean cumulative = false;
        for (int i = 0; i < band.size(); i++) {
            int inWeek = 0;
            BigDecimal inDay = BigDecimal.ZERO;
            for (int j = i; j < band.size(); j++) {
                Duration d = Duration.between(band.get(i).timestamp(), band.get(j).timestamp());
                if (d.compareTo(Duration.ofDays(7)) <= 0) inWeek++;
                if (d.compareTo(Duration.ofHours(24)) <= 0) inDay = inDay.add(band.get(j).amount());
            }
            if (inWeek >= 3) burst = true;
            if (inDay.compareTo(max) > 0) cumulative = true;
        }
        if (burst) {
            score += 25;
            reasons.add("3 ou mais movimentos na faixa em até 7 dias");
        }
        if (cumulative) {
            score += 25;
            reasons.add("Soma de movimentos na faixa em 24h acima de " + max + " EUR (possível fracionamento)");
        }
        return new Outcome(band.size(), total, Math.min(100, score), reasons);
    }
}
