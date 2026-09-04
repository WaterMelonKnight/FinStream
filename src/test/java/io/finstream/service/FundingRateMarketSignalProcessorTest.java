package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.TradePayload;
import io.finstream.state.InMemoryFundingRateStateStore;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FundingRateMarketSignalProcessorTest {
    @Test
    void updatesStateAndProducesNoFinancialEvents() {
        InMemoryFundingRateStateStore store = new InMemoryFundingRateStateStore();
        FundingRateMarketSignalProcessor processor = new FundingRateMarketSignalProcessor(store);
        Instant eventTime = Instant.parse("2026-09-04T10:00:00Z");
        Instant receivedAt = eventTime.plusMillis(25);
        FundingRatePayload payload = new FundingRatePayload(
                new BigDecimal("0.0001"), new BigDecimal("100000"),
                new BigDecimal("99990"), eventTime.plusSeconds(3600));
        MarketEvent event = new MarketEvent(
                "BINANCE", "BTCUSDT", MarketSignalType.FUNDING_RATE,
                eventTime, receivedAt, payload);

        assertThat(processor.signalType()).isEqualTo(MarketSignalType.FUNDING_RATE);
        assertThat(processor.process(event)).isEmpty();
        assertThat(store.get("BTCUSDT")).hasValueSatisfying(state -> {
            assertThat(state.source()).isEqualTo("BINANCE");
            assertThat(state.fundingRate()).isEqualByComparingTo("0.0001");
            assertThat(state.eventTime()).isEqualTo(eventTime);
            assertThat(state.receivedAt()).isEqualTo(receivedAt);
        });
    }

    @Test
    void rejectsWrongPayloadType() {
        FundingRateMarketSignalProcessor processor =
                new FundingRateMarketSignalProcessor(new InMemoryFundingRateStateStore());
        MarketEvent event = new MarketEvent("BINANCE", "BTCUSDT",
                MarketSignalType.FUNDING_RATE, Instant.EPOCH, Instant.EPOCH,
                new TradePayload(BigDecimal.ONE, BigDecimal.ONE));

        assertThatThrownBy(() -> processor.process(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FundingRatePayload");
    }
}
