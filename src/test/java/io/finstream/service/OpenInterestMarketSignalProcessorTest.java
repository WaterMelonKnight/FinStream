package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.finstream.anomaly.OpenInterestAnomalyRule;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.OpenInterestPayload;
import io.finstream.state.InMemoryOpenInterestStateStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    @Test
    void updatesRollingStateBeforeRuleEvaluationAndReturnsRuleEvent() {
        var store = new InMemoryOpenInterestStateStore();
        OpenInterestAnomalyRule rule = mock(OpenInterestAnomalyRule.class);
        var processor = new OpenInterestMarketSignalProcessor(store, List.of(rule));
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        when(rule.evaluate(any(), any())).thenReturn(Optional.empty());
        processor.process(event(start, "100"));
        reset(rule);

        FinancialEvent expected = financialEvent(start.plusSeconds(15 * 60));
        when(rule.evaluate(any(), any())).thenAnswer(invocation -> {
            var state = invocation.getArgument(1, io.finstream.domain.OpenInterestState.class);
            assertThat(store.get("BTCUSDT")).contains(state);
            assertThat(state.change15mPercent()).isEqualByComparingTo("10");
            return Optional.of(expected);
        });

        assertThat(processor.process(event(start.plusSeconds(15 * 60), "110")))
                .containsExactly(expected);
        verify(rule).evaluate(any(), any());
    }

    @Test
    void returnsEmptyListWhenRuleDoesNotEmit() {
        var store = new InMemoryOpenInterestStateStore();
        OpenInterestAnomalyRule rule = mock(OpenInterestAnomalyRule.class);
        when(rule.evaluate(any(), any())).thenReturn(Optional.empty());
        var processor = new OpenInterestMarketSignalProcessor(store, List.of(rule));

        assertThat(processor.process(event(Instant.EPOCH, "100"))).isEmpty();
        verify(rule).evaluate(any(), any());
    }

    private MarketEvent event(Instant time, String openInterest) {
        return new MarketEvent("BINANCE", "BTCUSDT", MarketSignalType.OPEN_INTEREST, time, time,
                new OpenInterestPayload(new BigDecimal(openInterest)));
    }

    private FinancialEvent financialEvent(Instant time) {
        return new FinancialEvent(UUID.randomUUID(), "BINANCE", "BTCUSDT",
                "OPEN_INTEREST_SURGE", time, time, "MEDIUM", 2, "summary", Map.of(), Map.of());
    }
}
