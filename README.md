# FinStream

**FinStream is an open-source real-time financial event and context engine for AI agents.** It senses unusual market behavior and records it as structured `FinancialEvent` data. FinStream is **not a trading bot**, does **not execute trades**, and does **not provide investment advice**.

## V0.2 capabilities

- Streams Binance public aggregate trades, maintains bounded 30-minute in-memory state, and detects rapid price moves and abnormal volume.
- Optionally polls Binance's public USDⓈ-M premium-index endpoint for funding rates, keeps the latest snapshot per configured symbol in memory, and detects extreme absolute funding rates.
- Persists only anomaly events to PostgreSQL JSONB while keeping the V0.1 real-time pipeline unchanged.
- Exposes stable, read-only REST response contracts and four MCP tools through one application query layer.
- Moves blocking JPA persistence and queries away from Reactor Netty event-loop threads.

**FinStream MCP provides read-only market context and anomaly event access.** It has no order, account, wallet, or other trading tools.

## Quick Start

Clone the repository and start both PostgreSQL and FinStream. Docker Compose builds the
application image, so Java and Maven do not need to be installed on the host.

```bash
docker compose up -d
```

Check container status and follow the application logs:

```bash
docker compose ps
docker compose logs -f finstream
```

Check application health and call the REST API:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/events
```

The MCP endpoint is available at `http://localhost:8080/mcp`.

Binance real-time WebSocket ingestion is disabled by default. Enable it for a Compose run with:

```bash
BINANCE_ENABLED=true docker compose up -d
```

Funding-rate ingestion is independently disabled by default. Enable its keyless public REST
poller (60-second default interval) without changing trade ingestion with:

```bash
BINANCE_FUNDING_ENABLED=true docker compose up -d
```

The poller reuses `finstream.market.symbols`. Override its interval, if needed, with
`BINANCE_FUNDING_POLL_INTERVAL` (for example, `30s`). Funding snapshots are currently
in-memory only and are not exposed through REST or MCP.

Funding anomaly detection is enabled by default once funding ingestion is active. Its
`finstream.anomaly.funding-extreme.threshold` is a **decimal rate**, with no implicit percent
conversion: the default `0.001` means `0.1%`, while `0.0001` means `0.01%`. Override the rule
with `FUNDING_EXTREME_ENABLED` and `FUNDING_EXTREME_THRESHOLD`. Positive and negative rates are
compared by absolute value and both produce the unified `FUNDING_EXTREME` event type; direction
and both decimal/percent values are retained in event metrics.

Stop the containers while retaining PostgreSQL data:

```bash
docker compose down
```

To stop the containers and completely delete the PostgreSQL data volume, run:

```bash
docker compose down -v
```

**Warning:** `docker compose down -v` permanently deletes the PostgreSQL data stored in the
Compose volume.

## Run locally

Requirements: Java 21, Maven 3.9+, and Docker Compose.

FinStream defaults to the Debian-based `postgres:16` Docker image for broader deployment compatibility.

```bash
docker compose up -d postgres
mvn spring-boot:run
```

The trade WebSocket is off by default. Enable public real-time trade data (no API key required) with:

```bash
BINANCE_ENABLED=true mvn spring-boot:run
```

Likewise, `BINANCE_FUNDING_ENABLED=true mvn spring-boot:run` enables only the public
funding-rate poller. The two ingestion switches are independent.

`DB_URL`, `DB_USER`, and `DB_PASSWORD` configure PostgreSQL. After restart, persisted events remain available, but in-memory market state returns 404 until new trades arrive.

## REST API

List limits default to 50, reject values below 1, and are capped at 200. Symbols and event types are normalized to uppercase. `since` is an ISO-8601 instant.

```bash
curl http://localhost:8080/api/v1/market/BTCUSDT/state
curl "http://localhost:8080/api/v1/events?symbol=BTCUSDT&limit=10"
curl "http://localhost:8080/api/v1/events?eventType=RAPID_DROP&limit=20"
curl "http://localhost:8080/api/v1/events?eventType=FUNDING_EXTREME&limit=20"
curl http://localhost:8080/api/v1/events/00000000-0000-0000-0000-000000000000
curl "http://localhost:8080/api/v1/events/abnormal?since=2026-08-20T00:00:00Z&minScore=1.5&symbol=BTCUSDT&limit=50"
```

The endpoints are:

- `GET /api/v1/market/{symbol}/state`
- `GET /api/v1/events`
- `GET /api/v1/events/{eventId}`
- `GET /api/v1/events/abnormal`

Invalid input and missing resources use stable JSON errors with `code`, `message`, and `timestamp`.

## MCP server

FinStream embeds the official Spring AI `spring-ai-starter-mcp-server-webflux` 1.1.2 and uses remote-friendly **Streamable HTTP** at `http://localhost:8080/mcp`. Starting the application starts REST and MCP together. MCP invokes the application query services directly; it never calls REST.

Available tools:

- `get_market_state(symbol)`
- `get_recent_events(symbol?, eventType?, limit?)`
- `get_event_detail(eventId)`
- `get_abnormal_events(since?, minScore?, symbol?, limit?)`

A representative client configuration (the outer field names can vary by client) is:

```json
{
  "mcpServers": {
    "finstream": {
      "type": "streamable-http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

No API key is needed in V0.2. Do not expose this unauthenticated development server publicly.

Run the network-independent tests with `mvn test` or the complete CI check with `mvn --batch-mode verify`. See [architecture](docs/architecture.md) and the non-binding [roadmap](docs/roadmap.md).
