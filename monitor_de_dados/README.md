# Monitor de movimentações 3.000–10.000 EUR

- `java-api/`: Spring Boot 3 / Java 17. `POST /api/v1/analysis` aplica regras (faixa, rajada, fracionamento em 24h)
  e, se houver `ANTHROPIC_API_KEY`, pede uma segunda opinião ao LLM (só dados agregados, sem dados pessoais).
- `python-app/`: orquestrador (`orchestrator.py`) + dashboard Streamlit com gráficos Seaborn (`app.py`).

## Executar
```bash
# 1) API
cd java-api
export ANTHROPIC_API_KEY=sk-ant-...   # opcional
mvn spring-boot:run

# 2) Dashboard (outro terminal)
cd python-app
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
streamlit run app.py
```

## Teste rápido da API
```bash
curl -s localhost:8080/api/v1/analysis -H 'content-type: application/json' -d '{
 "customerId":"C1","transactions":[
  {"id":"1","amount":4500,"currency":"EUR","timestamp":"2026-11-08T10:00:00Z"},
  {"id":"2","amount":5200,"currency":"EUR","timestamp":"2026-11-08T15:00:00Z"},
  {"id":"3","amount":4800,"currency":"EUR","timestamp":"2026-11-09T09:00:00Z"}]}'
```
