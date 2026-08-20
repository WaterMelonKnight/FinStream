package io.finstream.api;

import static org.mockito.Mockito.when;

import io.finstream.query.FinancialEventQueryService;
import io.finstream.query.FinancialEventResponse;
import io.finstream.query.MarketQueryService;
import io.finstream.query.MarketStateResponse;
import io.finstream.query.QueryException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(MockitoExtension.class)
class QueryControllersTest {
    @Mock MarketQueryService markets;
    @Mock FinancialEventQueryService events;
    WebTestClient client;
    UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(
                        new MarketController(markets), new FinancialEventController(events))
                .controllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void stateSuccessAndNotFound() {
        when(markets.getMarketState("btcusdt")).thenReturn(state());
        client.get().uri("/api/v1/market/btcusdt/state").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.symbol").isEqualTo("BTCUSDT");

        when(markets.getMarketState("MISSING")).thenThrow(new QueryException(
                "MARKET_STATE_NOT_FOUND", "missing", true));
        client.get().uri("/api/v1/market/MISSING/state").exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("MARKET_STATE_NOT_FOUND");
    }

    @Test
    void recentFilterDetailAndAbnormalFilter() {
        var response = event();
        when(events.getRecentEvents("BTCUSDT", "RAPID_DROP", 20)).thenReturn(List.of(response));
        client.get().uri("/api/v1/events?symbol=BTCUSDT&eventType=RAPID_DROP&limit=20")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$[0].id")
                .isEqualTo(id.toString());

        when(events.getEventDetail(id)).thenReturn(response);
        client.get().uri("/api/v1/events/" + id).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.evidence.price").isEqualTo(100);

        Instant since = Instant.parse("2026-08-20T00:00:00Z");
        when(events.getAbnormalEvents(since, 1.5, "BTCUSDT", 50)).thenReturn(List.of(response));
        client.get().uri("/api/v1/events/abnormal?since=2026-08-20T00:00:00Z&minScore=1.5&symbol=BTCUSDT&limit=50")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$[0].anomalyScore")
                .isEqualTo(2.0);
    }

    @Test
    void invalidParametersHaveStableErrors() {
        client.get().uri("/api/v1/events/not-a-uuid").exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_EVENT_ID");
        client.get().uri("/api/v1/events/abnormal?since=yesterday").exchange()
                .expectStatus().isBadRequest().expectBody().jsonPath("$.code")
                .isEqualTo("INVALID_SINCE");
        when(events.getRecentEvents(null, null, 0)).thenThrow(
                new QueryException("INVALID_LIMIT", "limit must be at least 1", false));
        client.get().uri("/api/v1/events?limit=0").exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_LIMIT");
    }

    private MarketStateResponse state() {
        return new MarketStateResponse("BTCUSDT", Instant.EPOCH, BigDecimal.TEN,
                0, 0, 0, 1, 1, BigDecimal.TEN, BigDecimal.TEN, 1, true);
    }

    private FinancialEventResponse event() {
        return new FinancialEventResponse(id, "BINANCE", "BTCUSDT", "RAPID_DROP",
                Instant.EPOCH, Instant.EPOCH, "HIGH", 2, "summary",
                Map.of("return5m", -4), Map.of("price", 100));
    }
}
