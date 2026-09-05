package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.OpenInterestPayload;
import io.finstream.state.InMemoryOpenInterestStateStore;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OpenInterestMarketSignalProcessorTest {
    @Test
    void updatesStateAndProducesNoFinancialEvents() {
        var store = new InMemoryOpenInterestStateStore();
        var processor = new OpenInterestMarketSignalProcessor(store);
        Instant eventTime = Instant.EPOCH;
        Instant receivedAt = Instant.EPOCH.plusSeconds(1);
        var events = processor.process(new MarketEvent(
                "BINANCE", "BTCUSDT", MarketSignalType.OPEN_INTEREST, eventTime, receivedAt,
                new OpenInterestPayload(new BigDecimal("123.456"))));

        assertThat(processor.signalType()).isEqualTo(MarketSignalType.OPEN_INTEREST);
        assertThat(events).isEmpty();
        assertThat(store.get("BTCUSDT")).hasValueSatisfying(state -> {
            assertThat(state.openInterest()).isEqualByComparingTo("123.456");
            assertThat(state.source()).isEqualTo("BINANCE");
            assertThat(state.eventTime()).isEqualTo(eventTime);
            assertThat(state.receivedAt()).isEqualTo(receivedAt);
        });
    }

    @Test
    void wrongPayloadFailsFast() {
        var processor = new OpenInterestMarketSignalProcessor(new InMemoryOpenInterestStateStore());
        var event = new MarketEvent("BINANCE", "BTCUSDT", MarketSignalType.OPEN_INTEREST,
                Instant.EPOCH, Instant.EPOCH, new FundingRatePayload(
                        BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, Instant.EPOCH));
        assertThatThrownBy(() -> processor.process(event)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OpenInterestPayload");
    }
}
