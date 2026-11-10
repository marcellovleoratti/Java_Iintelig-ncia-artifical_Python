package com.example.aml.service;

import com.example.aml.model.AnalysisRequest;
import com.example.aml.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final RuleEngine rules;
    private final AiAnalyzer ai;
    private final int flagThreshold;

    public AnalysisService(RuleEngine rules, AiAnalyzer ai, @Value("${aml.flag-threshold}") int flagThreshold) {
        this.rules = rules;
        this.ai = ai;
        this.flagThreshold = flagThreshold;
    }

    public AnalysisResult analyze(AnalysisRequest req) {
        RuleEngine.Outcome o = rules.evaluate(req.transactions());
        AiAnalyzer.Verdict v = o.count() > 0 ? ai.review(o)
                : new AiAnalyzer.Verdict(0, "Sem movimentos na faixa monitorada.", "rules");
        return new AnalysisResult(req.customerId(), o.count() > 0, v.score() >= flagThreshold,
                v.score(), o.count(), o.total(), o.reasons(), v.explanation(), v.engine());
    }
}
