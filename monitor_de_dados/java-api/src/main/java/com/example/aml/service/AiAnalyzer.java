package com.example.aml.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Segunda opinião via LLM. Recebe apenas um resumo agregado (sem dados pessoais). */
@Component
public class AiAnalyzer {

    public record Verdict(int score, String explanation, String engine) {}

    private static final Logger log = LoggerFactory.getLogger(AiAnalyzer.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String apiKey;
    private final String model;

    public AiAnalyzer(@Value("${aml.ai.api-key:}") String apiKey, @Value("${aml.ai.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public Verdict review(RuleEngine.Outcome o) {
        if (apiKey == null || apiKey.isBlank()) {
            return new Verdict(o.score(), "IA desabilitada (ANTHROPIC_API_KEY ausente); apenas regras.", "rules");
        }
        try {
            String prompt = """
                    Você é analista de prevenção à lavagem de dinheiro. Dados agregados de um cliente:
                    - movimentos entre 3.000 e 10.000 EUR: %d
                    - volume total nessa faixa: %s EUR
                    - score das regras (0-100): %d
                    - alertas: %s
                    Ajuste o score (0-100) se julgar necessário e explique em até 2 frases, em português.
                    Responda SOMENTE com JSON: {"adjustedScore": <int>, "explanation": "<texto>"}
                    """.formatted(o.count(), o.total(), o.score(), o.reasons());

            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", 400,
                    "messages", List.of(Map.of("role", "user", "content", prompt))));

            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .timeout(Duration.ofSeconds(30))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new IllegalStateException("HTTP " + res.statusCode());

            String text = mapper.readTree(res.body()).path("content").path(0).path("text").asText();
            text = text.replaceAll("```json|```", "").trim();
            JsonNode j = mapper.readTree(text);
            int adjusted = Math.max(0, Math.min(100, j.path("adjustedScore").asInt(o.score())));
            return new Verdict(adjusted, j.path("explanation").asText(""), "rules+llm");
        } catch (Exception e) {
            log.warn("Falha na análise por IA, usando só regras: {}", e.toString());
            return new Verdict(o.score(), "Falha ao consultar a IA; resultado baseado em regras.", "rules");
        }
    }
}
