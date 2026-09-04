# FinStream V0.2 architecture

## Runtime flow

```text
MarketDataConnector(s)
  +-> Binance aggregate TRADE WebSocket
  +-> Binance FUNDING_RATE REST polling
  -> canonical MarketEvent with typed payload
  -> Market Signal Router
      +-> TRADE processor -> Rolling Market State -> AnomalyRule implementations
      |                    -> cooldown -> FinancialEvent -> PostgreSQL
      +-> FUNDING_RATE processor -> latest FundingRateState (no anomaly yet)
```

The connector boundary prevents Binance's wire DTO/JSON from entering the domain. The pipeline merges the canonical streams from its registered `MarketDataConnector` implementations, so a future signal connector can be added without changing pipeline control flow. Every canonical `MarketEvent` identifies its exchange in `source` (currently `BINANCE`) and its exchange-independent input kind in `signalType`. `MarketSignalType` contains `TRADE`, `FUNDING_RATE`, `OPEN_INTEREST`, and `LIQUIDATION`; these input types are deliberately separate from anomaly output types such as `RAPID_DROP` and `ABNORMAL_VOLUME` in `FinancialEvent.eventType`.

The router dispatches each input only to the processor registered for its signal type. The
`TRADE` processor owns the existing price/volume path: `MarketStateStore` provides a replaceable
state boundary; its implementation keeps a synchronized, bounded deque per symbol and derives
1-, 5-, and 30-minute measurements. RAPID_DROP and RAPID_PUMP intentionally use the fixed
5-minute return. Independent `AnomalyRule` implementations consume the same snapshot. Only
noteworthy `FinancialEvent` objects cross the persistence boundary; raw trades remain transient.

The `FUNDING_RATE` processor validates its typed payload and replaces the symbol's latest
`FundingRateState`. That snapshot contains source, symbol, funding rate, mark and index prices,
next funding time, Binance event time, and local receive time. It is deliberately in memory,
does not run anomaly rules, and returns no `FinancialEvent`. `FUNDING_EXTREME` is not yet
implemented, and funding state is not exposed through REST or MCP.

```text
MarketDataConnector
        |
        v
Canonical MarketEvent
        |
        v
Market Signal Router
        |
        +--> TRADE processor
        |      -> MarketState -> anomaly rules -> FinancialEvent
        +--> FUNDING_RATE processor
        |      -> FundingRateState -> no anomaly yet
        +--> OPEN_INTEREST processor (future)
        +--> LIQUIDATION processor (future)
        |
        v
FinancialEvent -> PostgreSQL -> REST / MCP
```

The canonical envelope contains source, symbol, signal type, exchange event time, local receive
time, and a small sealed `MarketSignalPayload`. Only the payloads actually ingested exist:
`TradePayload(price, quantity)` and `FundingRatePayload(fundingRate, markPrice, indexPrice,
nextFundingTime)`. Processors fail fast if their signal receives the wrong payload type. This
keeps signal-specific values out of nullable envelope fields without introducing serialization
annotations or speculative payload types.

ABNORMAL_VOLUME compares the current one-minute volume with the per-minute average of the preceding four minutes. It remains in warm-up until the in-memory state has five minutes of history, so a restart cannot immediately produce a volume anomaly from a partial baseline. This simple baseline is intentionally not seasonality-aware.

State calculation stays on the streaming path, while blocking JPA saves are scheduled on Reactor's shared bounded-elastic scheduler. A cooldown key is reserved while a save is in flight and committed only after persistence succeeds; failures release the reservation for a later trade to retry.

Cooldown is keyed by `symbol + eventType`, so a condition that remains true cannot create an
event on every trade. Configuration properties bind sources, shared symbols, thresholds, and
cooldown centrally. The Binance trade adapter reconnects with capped exponential backoff and
treats malformed messages as isolated input errors. The independent Binance funding adapter
polls the public USDⓈ-M `/fapi/v1/premiumIndex` endpoint once per configured symbol every 60
seconds by default. A request or normalization failure is isolated to that poll, so later polls
continue. Both connectors are disabled independently by default.

## Deliberate constraints

Kafka, Flink, RisingWave, Redis, and ClickHouse would add operational cost without helping validate this small three-symbol loop. The in-process state is intentionally simple: it is lost on restart and is not horizontally coordinated. These are accepted V0.1 trade-offs, not hidden durability guarantees.

Future stream-processing implementations should replace the connector/state infrastructure behind existing boundaries. They must preserve canonical `MarketEvent`, `FinancialEvent`, and upper API semantics, so downstream agents are insulated from the processing technology.

## V0.2 query and adapter boundaries

```text
Market Feed
  -> MarketState / FinancialEvent
  -> Application Query Layer
  -> REST adapter + MCP adapter
```

`MarketQueryService` and `FinancialEventQueryService` own normalization, validation, limit handling, repository specifications, not-found semantics, and mapping to stable response records. REST controllers and MCP tools are thin adapters over those same services; neither adapter calls the other and neither exposes the JPA entity. Dynamic JPA Specifications cover optional filters without a repository method for every combination. Results sort by `detectedAt`, then `eventTime`, descending.

WebFlux REST calls wrap service work on Reactor's bounded-elastic scheduler. MCP tool execution also moves its callable to bounded elastic before waiting for the structured tool result. Blocking JPA queries therefore do not execute on a Reactor Netty event-loop thread, while JPA remains the persistence technology.

The store maintains an immutable latest `MarketState` record per symbol alongside each protected rolling deque. `get` is a constant-time concurrent-map lookup: it neither exposes the deque nor rescans 30 minutes of ticks.

### In-memory state lifetime

After an application restart, historical `FinancialEvent` rows remain in PostgreSQL, but `MarketState` is lost. A market-state query returns not found until fresh live trades rebuild that symbol's state. FinStream does not persist `MarketState` in V0.2. RisingWave or Flink-backed state remains a future roadmap decision.
