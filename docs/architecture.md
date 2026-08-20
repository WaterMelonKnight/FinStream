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

The connector boundary prevents Binance's wire DTO/JSON from entering the domain. `MarketStateStore` provides a replaceable state boundary; its V0.1 implementation keeps a synchronized, bounded deque per symbol and derives 1-, 5-, and 30-minute measurements. RAPID_DROP and RAPID_PUMP intentionally use the fixed 5-minute return in V0.1; configurable windows are deferred. Independent `AnomalyRule` implementations consume the same snapshot. Only noteworthy `FinancialEvent` objects cross the persistence boundary; raw trades remain transient.

ABNORMAL_VOLUME compares the current one-minute volume with the per-minute average of the preceding four minutes. It remains in warm-up until the in-memory state has five minutes of history, so a restart cannot immediately produce a volume anomaly from a partial baseline. This simple baseline is intentionally not seasonality-aware.

State calculation stays on the streaming path, while blocking JPA saves are scheduled on Reactor's shared bounded-elastic scheduler. A cooldown key is reserved while a save is in flight and committed only after persistence succeeds; failures release the reservation for a later trade to retry.

Cooldown is keyed by `symbol + eventType`, so a condition that remains true cannot create an event on every trade. Configuration properties bind sources, symbols, thresholds, and cooldown centrally. The Binance adapter reconnects with capped exponential backoff and treats malformed messages as isolated input errors.

## Deliberate constraints

Kafka, Flink, RisingWave, Redis, and ClickHouse would add operational cost without helping validate this small three-symbol loop. The in-process state is intentionally simple: it is lost on restart and is not horizontally coordinated. These are accepted V0.1 trade-offs, not hidden durability guarantees.

Future stream-processing implementations should replace the connector/state infrastructure behind existing boundaries. They must preserve canonical `MarketEvent`, `FinancialEvent`, and upper API semantics, so downstream agents are insulated from the processing technology.
