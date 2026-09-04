package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.finstream.anomaly.AnomalyRule;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.MarketState;
import io.finstream.state.MarketStateStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TradeMarketSignalProcessorTest {
    @Test
    void updatesTradeStateAndEvaluatesEveryTradeRule() {
        MarketStateStore states = mock(MarketStateStore.class);
        MarketState state = mock(MarketState.class);
        AnomalyRule drop = mock(AnomalyRule.class);
        AnomalyRule pump = mock(AnomalyRule.class);
        AnomalyRule volume = mock(AnomalyRule.class);
        FinancialEvent dropEvent = mock(FinancialEvent.class);
        FinancialEvent volumeEvent = mock(FinancialEvent.class);
        MarketEvent trade = trade();
        when(states.update(trade)).thenReturn(state);
        when(drop.evaluate(trade, state)).thenReturn(Optional.of(dropEvent));
        when(pump.evaluate(trade, state)).thenReturn(Optional.empty());
        when(volume.evaluate(trade, state)).thenReturn(Optional.of(volumeEvent));

        TradeMarketSignalProcessor processor =
                new TradeMarketSignalProcessor(states, List.of(drop, pump, volume));

        assertThat(processor.signalType()).isEqualTo(MarketSignalType.TRADE);
        assertThat(processor.process(trade)).containsExactly(dropEvent, volumeEvent);
        verify(states).update(trade);
        verify(drop).evaluate(trade, state);
        verify(pump).evaluate(trade, state);
        verify(volume).evaluate(trade, state);
    }

    private MarketEvent trade() {
        return new MarketEvent(
                "BINANCE", "BTCUSDT", MarketSignalType.TRADE, Instant.EPOCH, Instant.EPOCH,
                BigDecimal.TEN, BigDecimal.ONE);
    }
}
