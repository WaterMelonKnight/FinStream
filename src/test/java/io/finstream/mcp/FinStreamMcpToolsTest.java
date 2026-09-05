package io.finstream.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.finstream.query.FinancialEventQueryService;
import io.finstream.query.FundingRateStateResponse;
import io.finstream.query.MarketQueryService;
import io.finstream.query.OpenInterestStateResponse;
import io.finstream.query.QueryException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinStreamMcpToolsTest {
    @Mock MarketQueryService markets;
    @Mock FinancialEventQueryService events;

    @Test
    void allSixToolsDelegateAndReturnStructuredResults() {
        UUID id = UUID.randomUUID();
        when(markets.getMarketState("BTCUSDT")).thenReturn(null);
        FundingRateStateResponse funding = new FundingRateStateResponse(
                "BINANCE", "BTCUSDT", new BigDecimal("0.001"), new BigDecimal("0.1"),
                BigDecimal.TEN, BigDecimal.ONE, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH);
        when(markets.getFundingRateState("BTCUSDT")).thenReturn(funding);
        OpenInterestStateResponse openInterest = new OpenInterestStateResponse(
                "BINANCE", "BTCUSDT", new BigDecimal("123.45"),
                Instant.EPOCH, Instant.EPOCH);
        when(markets.getOpenInterestState("BTCUSDT")).thenReturn(openInterest);
        when(events.getRecentEvents("BTCUSDT", null, 10)).thenReturn(List.of());
        when(events.getEventDetail(id)).thenReturn(null);
        when(events.getAbnormalEvents(Instant.EPOCH, 1.5, "BTCUSDT", 20)).thenReturn(List.of());
        FinStreamMcpTools tools = new FinStreamMcpTools(markets, events);

        assertThat(tools.getMarketState("BTCUSDT").success()).isTrue();
        assertThat(tools.getFundingRateState("BTCUSDT").data()).isEqualTo(funding);
        assertThat(tools.getOpenInterestState("BTCUSDT").data()).isEqualTo(openInterest);
        assertThat(tools.getRecentEvents("BTCUSDT", null, 10).success()).isTrue();
        assertThat(tools.getEventDetail(id.toString()).success()).isTrue();
        assertThat(tools.getAbnormalEvents(Instant.EPOCH.toString(), 1.5, "BTCUSDT", 20).success()).isTrue();
    }

    @Test
    void toolsReturnValidationAndNotFoundErrorsInsteadOfThrowing() {
        when(markets.getMarketState("NONE")).thenThrow(
                new QueryException("MARKET_STATE_NOT_FOUND", "missing", true));
        when(markets.getFundingRateState("NONE")).thenThrow(
                new QueryException("FUNDING_RATE_STATE_NOT_FOUND", "missing funding", true));
        when(markets.getOpenInterestState("NONE")).thenThrow(
                new QueryException("OPEN_INTEREST_STATE_NOT_FOUND", "missing OI", true));
        FinStreamMcpTools tools = new FinStreamMcpTools(markets, events);

        assertThat(tools.getMarketState("NONE").error().code()).isEqualTo("MARKET_STATE_NOT_FOUND");
        assertThat(tools.getFundingRateState("NONE").error().code())
                .isEqualTo("FUNDING_RATE_STATE_NOT_FOUND");
        assertThat(tools.getOpenInterestState("NONE").error().code())
                .isEqualTo("OPEN_INTEREST_STATE_NOT_FOUND");
        assertThat(tools.getEventDetail("bad").error().code()).isEqualTo("INVALID_EVENT_ID");
        assertThat(tools.getAbnormalEvents("bad", null, null, null).error().code())
                .isEqualTo("INVALID_SINCE");
    }
}
