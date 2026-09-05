package io.finstream.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.config.FinStreamProperties;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.OpenInterestPayload;
import io.finstream.domain.OpenInterestState;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OpenInterestSurgeRuleTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:15:00Z");

    @Test
    void emitsAtThresholdWithExpectedContractAndSeverity() {
        var rule = new OpenInterestSurgeRule(
                new FinStreamProperties.OpenInterestSurge(true, new BigDecimal("5")),
                Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC));
        var result = rule.evaluate(event(), state("105", "5", "100"));

        assertThat(result).hasValueSatisfying(value -> {
            assertThat(value.getEventType()).isEqualTo("OPEN_INTEREST_SURGE");
            assertThat(value.getSeverity()).isEqualTo("MEDIUM");
            assertThat(value.getAnomalyScore()).isEqualTo(1.0);
            assertThat(value.getMetrics()).containsEntry("windowMinutes", 15)
                    .containsEntry("referenceOpenInterest", new BigDecimal("100"));
            assertThat(value.getSummary()).doesNotContain("bullish", "long", "buy");
        });
    }

    @Test
    void requiresReadyPositiveChangeAndHonorsDisabledRule() {
        var enabled = new OpenInterestSurgeRule(
                new FinStreamProperties.OpenInterestSurge(true, new BigDecimal("5")), Clock.systemUTC());
        assertThat(enabled.evaluate(event(), state("100", null, null))).isEmpty();
        assertThat(enabled.evaluate(event(), state("90", "-10", "100"))).isEmpty();
        var disabled = new OpenInterestSurgeRule(
                new FinStreamProperties.OpenInterestSurge(false, new BigDecimal("5")), Clock.systemUTC());
        assertThat(disabled.evaluate(event(), state("110", "10", "100"))).isEmpty();
    }

    private MarketEvent event() {
        return new MarketEvent("BINANCE", "BTCUSDT", MarketSignalType.OPEN_INTEREST, NOW, NOW,
                new OpenInterestPayload(new BigDecimal("105")));
    }

    private OpenInterestState state(String current, String change, String reference) {
        return new OpenInterestState("BINANCE", "BTCUSDT", new BigDecimal(current), null,
                change == null ? null : new BigDecimal(change), null,
                reference == null ? null : new BigDecimal(reference),
                reference == null ? null : NOW.minusSeconds(900), NOW, NOW);
    }
}
