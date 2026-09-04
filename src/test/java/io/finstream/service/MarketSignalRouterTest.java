package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketSignalRouterTest {
    @Test
    void tradeIsSentToTradeProcessor() {
        MarketSignalProcessor processor = mock(MarketSignalProcessor.class);
        FinancialEvent anomaly = mock(FinancialEvent.class);
        MarketEvent trade = event(MarketSignalType.TRADE);
        when(processor.signalType()).thenReturn(MarketSignalType.TRADE);
        when(processor.process(trade)).thenReturn(List.of(anomaly));

        assertThat(new MarketSignalRouter(List.of(processor)).route(trade)).containsExactly(anomaly);
        verify(processor).process(trade);
    }

    @Test
    void futureSignalWithoutProcessorDoesNotEnterTradeProcessor() {
        MarketSignalProcessor tradeProcessor = mock(MarketSignalProcessor.class);
        when(tradeProcessor.signalType()).thenReturn(MarketSignalType.TRADE);

        assertThat(new MarketSignalRouter(List.of(tradeProcessor))
                        .route(event(MarketSignalType.FUNDING_RATE)))
                .isEmpty();
        verify(tradeProcessor).signalType();
        verifyNoMoreInteractions(tradeProcessor);
    }

    private MarketEvent event(MarketSignalType signalType) {
        return new MarketEvent(
                "BINANCE", "BTCUSDT", signalType, Instant.EPOCH, Instant.EPOCH,
                BigDecimal.TEN, BigDecimal.ONE);
    }
}
