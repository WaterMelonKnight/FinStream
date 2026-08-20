# Roadmap (non-binding)

FinStream will evolve according to measured use and scale; these stages are directions, not delivery commitments.

- **V0.1 — Real-time events:** Binance public trades through rolling state and anomaly detection into PostgreSQL `FinancialEvent` records.
- **Future rule refinement:** consider configurable return windows only if real usage requires them.
- **V0.2 — Agent access (current):** read-only REST API and Streamable HTTP MCP server exposing `get_market_state`, `get_recent_events`, `get_event_detail`, and `get_abnormal_events` through one application query layer.
- **V0.3 — RoastLens integration:** FinStream remains the event source; RoastLens remains an independent content agent.
- **V0.4 — Evaluation:** historical replay and event-quality evaluation.
- **V0.5 — Streaming database:** evaluate RisingWave based on observed state and scale needs.
- **V0.6 — More sources:** liquidation, funding rate, open interest, news, and US equities.
- **V1.0 — Scale-informed infrastructure:** decide from production evidence whether Kafka, Flink, and ClickHouse are warranted.
