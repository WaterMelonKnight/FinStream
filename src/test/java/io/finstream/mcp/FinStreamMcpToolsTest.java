package io.finstream.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.finstream.query.FinancialEventQueryService;
import io.finstream.query.MarketQueryService;
import io.finstream.query.QueryException;
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
    void allFourToolsDelegateAndReturnStructuredResults() {
        UUID id = UUID.randomUUID();
        when(markets.getMarketState("BTCUSDT")).thenReturn(null);
        when(events.getRecentEvents("BTCUSDT", null, 10)).thenReturn(List.of());
        when(events.getEventDetail(id)).thenReturn(null);
        when(events.getAbnormalEvents(Instant.EPOCH, 1.5, "BTCUSDT", 20)).thenReturn(List.of());
        FinStreamMcpTools tools = new FinStreamMcpTools(markets, events);

        assertThat(tools.getMarketState("BTCUSDT").success()).isTrue();
        assertThat(tools.getRecentEvents("BTCUSDT", null, 10).success()).isTrue();
        assertThat(tools.getEventDetail(id.toString()).success()).isTrue();
        assertThat(tools.getAbnormalEvents(Instant.EPOCH.toString(), 1.5, "BTCUSDT", 20).success()).isTrue();
    }

    @Test
    void toolsReturnValidationAndNotFoundErrorsInsteadOfThrowing() {
        when(markets.getMarketState("NONE")).thenThrow(
                new QueryException("MARKET_STATE_NOT_FOUND", "missing", true));
        FinStreamMcpTools tools = new FinStreamMcpTools(markets, events);

        assertThat(tools.getMarketState("NONE").error().code()).isEqualTo("MARKET_STATE_NOT_FOUND");
        assertThat(tools.getEventDetail("bad").error().code()).isEqualTo("INVALID_EVENT_ID");
        assertThat(tools.getAbnormalEvents("bad", null, null, null).error().code())
                .isEqualTo("INVALID_SINCE");
    }
}
