package io.finstream.api;

import static org.mockito.Mockito.when;

import io.finstream.query.FinancialEventQueryService;
import io.finstream.query.FinancialEventResponse;
import io.finstream.query.FundingRateStateResponse;
import io.finstream.query.MarketContextResponse;
import io.finstream.query.MarketQueryService;
import io.finstream.query.MarketStateResponse;
import io.finstream.query.OpenInterestStateResponse;
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
    void fundingRateStateSuccessAndNotFound() {
        when(markets.getFundingRateState("BTCUSDT")).thenReturn(fundingRateState());
        client.get().uri("/api/v1/market/BTCUSDT/funding-rate").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath("$.source").isEqualTo("BINANCE")
                .jsonPath("$.symbol").isEqualTo("BTCUSDT")
                .jsonPath("$.fundingRate").isEqualTo(0.001)
                .jsonPath("$.fundingRatePercent").isEqualTo(0.1)
                .jsonPath("$.markPrice").isEqualTo(111234.50)
                .jsonPath("$.indexPrice").isEqualTo(111200.25);

        when(markets.getFundingRateState("MISSING")).thenThrow(new QueryException(
                "FUNDING_RATE_STATE_NOT_FOUND", "missing", true));
        client.get().uri("/api/v1/market/MISSING/funding-rate").exchange()
                .expectStatus().isNotFound().expectBody()
                .jsonPath("$.code").isEqualTo("FUNDING_RATE_STATE_NOT_FOUND");
    }

    @Test
    void openInterestStateSuccessAndNotFound() {
        when(markets.getOpenInterestState("BTCUSDT")).thenReturn(new OpenInterestStateResponse(
                "BINANCE", "BTCUSDT", new BigDecimal("12345.67890123456789"),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1)));
        client.get().uri("/api/v1/market/BTCUSDT/open-interest").exchange()
                .expectStatus().isOk().expectBody()
                .jsonPath("$.source").isEqualTo("BINANCE")
                .jsonPath("$.symbol").isEqualTo("BTCUSDT")
                .jsonPath("$.openInterest").isEqualTo(12345.67890123456789);

        when(markets.getOpenInterestState("MISSING")).thenThrow(new QueryException(
                "OPEN_INTEREST_STATE_NOT_FOUND", "missing", true));
        client.get().uri("/api/v1/market/MISSING/open-interest").exchange()
                .expectStatus().isNotFound().expectBody()
                .jsonPath("$.code").isEqualTo("OPEN_INTEREST_STATE_NOT_FOUND");
    }

    @Test
    void marketContextHasStablePartialContract() {
        Instant generatedAt = Instant.parse("2026-09-13T12:00:00Z");
        var context = new MarketContextResponse("BTCUSDT", generatedAt,
                new MarketContextResponse.MarketContextSection<>(true, "SPOT", 2L,
                        generatedAt.minusSeconds(2), state()),
                new MarketContextResponse.MarketContextSection<>(false, "USD_M_FUTURES", null,
                        null, null),
                new MarketContextResponse.MarketContextSection<>(true, "USD_M_FUTURES", 15L,
                        generatedAt.minusSeconds(15), new OpenInterestStateResponse(
                                "BINANCE", "BTCUSDT", new BigDecimal("123"),
                                Instant.EPOCH, generatedAt.minusSeconds(15))));
        when(markets.getMarketContext("btcusdt")).thenReturn(context);

        client.get().uri("/api/v1/market/btcusdt/context").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.symbol").isEqualTo("BTCUSDT")
                .jsonPath("$.market.available").isEqualTo(true)
                .jsonPath("$.market.marketType").isEqualTo("SPOT")
                .jsonPath("$.market.ageSeconds").isEqualTo(2)
                .jsonPath("$.funding.available").isEqualTo(false)
                .jsonPath("$.funding.data").doesNotExist()
                .jsonPath("$.openInterest.marketType").isEqualTo("USD_M_FUTURES");
    }

    @Test
    void marketContextAllMissingUsesContextError() {
        when(markets.getMarketContext("MISSING")).thenThrow(new QueryException(
                "MARKET_CONTEXT_NOT_FOUND", "missing context", true));

        client.get().uri("/api/v1/market/MISSING/context").exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("MARKET_CONTEXT_NOT_FOUND")
                .jsonPath("$.message").isEqualTo("missing context").jsonPath("$.timestamp").exists();
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

    private FundingRateStateResponse fundingRateState() {
        return new FundingRateStateResponse(
                "BINANCE", "BTCUSDT", new BigDecimal("0.001"), new BigDecimal("0.1"),
                new BigDecimal("111234.50"), new BigDecimal("111200.25"),
                Instant.EPOCH.plusSeconds(3600), Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
    }

    private FinancialEventResponse event() {
        return new FinancialEventResponse(id, "BINANCE", "BTCUSDT", "RAPID_DROP",
                Instant.EPOCH, Instant.EPOCH, "HIGH", 2, "summary",
                Map.of("return5m", -4), Map.of("price", 100));
    }
}
