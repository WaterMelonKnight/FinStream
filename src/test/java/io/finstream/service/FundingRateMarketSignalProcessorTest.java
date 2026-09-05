package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.TradePayload;
import io.finstream.state.InMemoryFundingRateStateStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FundingRateMarketSignalProcessorTest {
    @Test
    void updatesStateWhenRateIsNotExtreme() {
        InMemoryFundingRateStateStore store = new InMemoryFundingRateStateStore();
        FundingRateMarketSignalProcessor processor = new FundingRateMarketSignalProcessor(store, List.of());
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
    void updatesStateBeforeReturningExtremeEvent() {
        InMemoryFundingRateStateStore store = new InMemoryFundingRateStateStore();
        FinancialEvent anomaly = org.mockito.Mockito.mock(FinancialEvent.class);
        var rule = org.mockito.Mockito.mock(io.finstream.anomaly.FundingRateAnomalyRule.class);
        org.mockito.Mockito.when(rule.evaluate(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
                    assertThat(store.get("BTCUSDT")).isPresent();
                    return Optional.of(anomaly);
                });
        FundingRateMarketSignalProcessor processor =
                new FundingRateMarketSignalProcessor(store, List.of(rule));
        MarketEvent event = fundingEvent("0.002");

        assertThat(processor.process(event)).containsExactly(anomaly);
        assertThat(store.get("BTCUSDT").orElseThrow().fundingRate()).isEqualByComparingTo("0.002");
    }

    @Test
    void rejectsWrongPayloadType() {
        FundingRateMarketSignalProcessor processor =
                new FundingRateMarketSignalProcessor(new InMemoryFundingRateStateStore(), List.of());
        MarketEvent event = new MarketEvent("BINANCE", "BTCUSDT",
                MarketSignalType.FUNDING_RATE, Instant.EPOCH, Instant.EPOCH,
                new TradePayload(BigDecimal.ONE, BigDecimal.ONE));

        assertThatThrownBy(() -> processor.process(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FundingRatePayload");
    }

    private MarketEvent fundingEvent(String rate) {
        return new MarketEvent("BINANCE", "BTCUSDT", MarketSignalType.FUNDING_RATE,
                Instant.EPOCH, Instant.EPOCH,
                new FundingRatePayload(new BigDecimal(rate), BigDecimal.TEN,
                        BigDecimal.ONE, Instant.EPOCH.plusSeconds(3600)));
    }
}
