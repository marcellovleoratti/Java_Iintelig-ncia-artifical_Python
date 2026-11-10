package com.example.aml.web;

import com.example.aml.model.AnalysisRequest;
import com.example.aml.model.AnalysisResult;
import com.example.aml.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping("/analysis")
    public AnalysisResult analyze(@Valid @RequestBody AnalysisRequest request) {
        return service.analyze(request);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
