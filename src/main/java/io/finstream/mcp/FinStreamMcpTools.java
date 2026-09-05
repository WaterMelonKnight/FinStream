package io.finstream.mcp;

import io.finstream.query.FinancialEventQueryService;
import io.finstream.query.MarketQueryService;
import io.finstream.query.QueryException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class FinStreamMcpTools {
    private final MarketQueryService markets;
    private final FinancialEventQueryService events;

    public FinStreamMcpTools(MarketQueryService markets, FinancialEventQueryService events) {
        this.markets = markets;
        this.events = events;
    }

    @Tool(name = "get_market_state", description = "Get the latest in-memory market state snapshot for a symbol. Read-only.")
    public McpToolResult getMarketState(@ToolParam(description = "Market symbol, for example BTCUSDT") String symbol) {
        return execute(() -> markets.getMarketState(symbol));
    }

    @Tool(name = "get_funding_rate_state", description = "Get the latest in-memory funding-rate snapshot for a symbol. Read-only. fundingRate is the decimal rate; fundingRatePercent is its percent representation.")
    public McpToolResult getFundingRateState(
            @ToolParam(description = "Market symbol, for example BTCUSDT") String symbol) {
        return execute(() -> markets.getFundingRateState(symbol));
    }

    @Tool(name = "get_open_interest_state", description = "Get the latest in-memory Open Interest state and nullable 5, 15, and 30 minute rolling changes for a symbol. Read-only; raw history is not exposed.")
    public McpToolResult getOpenInterestState(
            @ToolParam(description = "Market symbol, for example BTCUSDT") String symbol) {
        return execute(() -> markets.getOpenInterestState(symbol));
    }

    @Tool(name = "get_recent_events", description = "Get recent anomaly events, newest first. All filters are optional. Read-only.")
    public McpToolResult getRecentEvents(
            @ToolParam(description = "Optional market symbol", required = false) String symbol,
            @ToolParam(description = "Optional event type: RAPID_DROP, RAPID_PUMP, ABNORMAL_VOLUME, FUNDING_EXTREME, or OPEN_INTEREST_SURGE", required = false) String eventType,
            @ToolParam(description = "Optional result limit; defaults to 50 and is capped at 200", required = false) Integer limit) {
        return execute(() -> events.getRecentEvents(symbol, eventType, limit));
    }

    @Tool(name = "get_event_detail", description = "Get the complete evidence and metrics for one anomaly event by UUID. Read-only.")
    public McpToolResult getEventDetail(@ToolParam(description = "Financial event UUID") String eventId) {
        try {
            return execute(() -> events.getEventDetail(UUID.fromString(eventId)));
        } catch (IllegalArgumentException error) {
            return McpToolResult.error("INVALID_EVENT_ID", "eventId must be a UUID");
        }
    }

    @Tool(name = "get_abnormal_events", description = "Find anomaly events at or above a score, optionally since an ISO-8601 timestamp and for one symbol. Read-only.")
    public McpToolResult getAbnormalEvents(
            @ToolParam(description = "Optional ISO-8601 timestamp, for example 2026-08-20T00:00:00Z", required = false) String since,
            @ToolParam(description = "Optional minimum anomaly score; defaults to 1.0", required = false) Double minScore,
            @ToolParam(description = "Optional market symbol", required = false) String symbol,
            @ToolParam(description = "Optional result limit; defaults to 50 and is capped at 200", required = false) Integer limit) {
        try {
            Instant parsed = since == null || since.isBlank() ? null : Instant.parse(since);
            return execute(() -> events.getAbnormalEvents(parsed, minScore, symbol, limit));
        } catch (DateTimeParseException error) {
            return McpToolResult.error("INVALID_SINCE", "since must be an ISO-8601 timestamp");
        }
    }

    private McpToolResult execute(java.util.concurrent.Callable<?> query) {
        try {
            Object value = Mono.fromCallable(query).subscribeOn(Schedulers.boundedElastic()).block();
            return McpToolResult.found(value);
        } catch (QueryException error) {
            return McpToolResult.error(error.code(), error.getMessage());
        }
    }
}
