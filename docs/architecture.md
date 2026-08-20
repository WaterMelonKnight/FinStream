# FinStream V0.1 architecture

## Runtime flow

```text
Binance public WebSocket
  -> MarketDataConnector
  -> canonical MarketEvent
  -> Rolling Market State
  -> AnomalyRule implementations
  -> cooldown / deduplication
  -> FinancialEvent
  -> PostgreSQL
```

The connector boundary prevents Binance's wire DTO/JSON from entering the domain. `MarketStateStore` provides a replaceable state boundary; its V0.1 implementation keeps a synchronized, bounded deque per symbol and derives 1-, 5-, and 30-minute measurements. Independent `AnomalyRule` implementations consume the same snapshot. Only noteworthy `FinancialEvent` objects cross the persistence boundary; raw trades remain transient.

Cooldown is keyed by `symbol + eventType`, so a condition that remains true cannot create an event on every trade. Configuration properties bind sources, symbols, thresholds, windows, and cooldown centrally. The Binance adapter reconnects with capped exponential backoff and treats malformed messages as isolated input errors.

## Deliberate constraints

Kafka, Flink, RisingWave, Redis, and ClickHouse would add operational cost without helping validate this small three-symbol loop. The in-process state is intentionally simple: it is lost on restart and is not horizontally coordinated. These are accepted V0.1 trade-offs, not hidden durability guarantees.

Future stream-processing implementations should replace the connector/state infrastructure behind existing boundaries. They must preserve canonical `MarketEvent`, `FinancialEvent`, and upper API semantics, so downstream agents are insulated from the processing technology.
