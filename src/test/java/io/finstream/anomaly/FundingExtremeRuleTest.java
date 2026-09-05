package io.finstream.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.config.FinStreamProperties;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.FundingRateState;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FundingExtremeRuleTest {
    private static final Instant TIME = Instant.parse("2026-09-04T10:00:00Z");
    private final FundingExtremeRule rule = new FundingExtremeRule(
            new FinStreamProperties.FundingExtreme(true, new BigDecimal("0.001")),
            Clock.fixed(TIME.plusSeconds(1), ZoneOffset.UTC));

    @Test
    void belowThresholdProducesNoEvent() {
        assertThat(rule.evaluate(event("0.000999"), state("0.000999"))).isEmpty();
    }

    @Test
    void positiveExtremeAtEqualityProducesExpectedEventAndMetrics() {
        FinancialEvent result = rule.evaluate(event("0.001"), state("0.001")).orElseThrow();

        assertThat(result.getEventType()).isEqualTo("FUNDING_EXTREME");
        assertThat(result.getSource()).isEqualTo("BINANCE");
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getEventTime()).isEqualTo(TIME);
        assertThat(result.getDetectedAt()).isEqualTo(TIME.plusSeconds(1));
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
        assertThat(result.getAnomalyScore()).isEqualTo(1.0);
        assertThat(result.getMetrics()).containsEntry("fundingRate", new BigDecimal("0.001"))
                .containsEntry("fundingRatePercent", new BigDecimal("0.1"))
                .containsEntry("absoluteFundingRate", new BigDecimal("0.001"))
                .containsEntry("threshold", new BigDecimal("0.001"))
                .containsEntry("thresholdPercent", new BigDecimal("0.1"))
                .containsEntry("markPrice", new BigDecimal("100000"))
                .containsEntry("indexPrice", new BigDecimal("99990"))
                .containsEntry("direction", "POSITIVE");
        assertThat(result.getEvidence()).containsEntry("direction", "POSITIVE");
        assertThat(result.getSummary()).contains("+0.1%", "meeting or exceeding", "0.1% threshold")
                .doesNotContain("buy", "sell", "long", "short", "should trade");
    }

    @Test
    void negativeExtremeUsesAbsoluteScoreAndExposesDirection() {
        FinancialEvent result = rule.evaluate(event("-0.0025"), state("-0.0025")).orElseThrow();

        assertThat(result.getAnomalyScore()).isEqualTo(2.5);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getMetrics()).containsEntry("fundingRatePercent", new BigDecimal("-0.25"))
                .containsEntry("absoluteFundingRate", new BigDecimal("0.0025"))
                .containsEntry("direction", "NEGATIVE");
        assertThat(result.getSummary()).contains("-0.25%");
    }

    @Test
    void disabledRuleProducesNoEvent() {
        var disabled = new FundingExtremeRule(
                new FinStreamProperties.FundingExtreme(false, new BigDecimal("0.001")),
                Clock.systemUTC());
        assertThat(disabled.evaluate(event("0.01"), state("0.01"))).isEmpty();
    }

    private MarketEvent event(String rate) {
        return new MarketEvent("BINANCE", "BTCUSDT", MarketSignalType.FUNDING_RATE, TIME, TIME,
                new FundingRatePayload(new BigDecimal(rate), new BigDecimal("100000"),
                        new BigDecimal("99990"), TIME.plusSeconds(3600)));
    }

    private FundingRateState state(String rate) {
        return new FundingRateState("BTCUSDT", "BINANCE", new BigDecimal(rate),
                new BigDecimal("100000"), new BigDecimal("99990"), TIME.plusSeconds(3600), TIME, TIME);
    }
}
