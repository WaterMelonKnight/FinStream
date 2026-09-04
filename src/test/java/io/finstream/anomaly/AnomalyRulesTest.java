package io.finstream.anomaly;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.config.FinStreamProperties;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.TradePayload;
import io.finstream.domain.MarketState;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnomalyRulesTest {
    private final FinStreamProperties properties = new FinStreamProperties(
            new FinStreamProperties.Market(
                    List.of("BTCUSDT"), new FinStreamProperties.Binance(false, null)),
            new FinStreamProperties.Anomaly(
                    Duration.ofMinutes(5),
                    new FinStreamProperties.Rule(true, 3),
                    new FinStreamProperties.Rule(true, 3),
                    new FinStreamProperties.VolumeRule(true, 3)));

    private final MarketEvent event = new MarketEvent(
            "TEST",
            "BTCUSDT",
            MarketSignalType.TRADE,
            Instant.EPOCH,
            Instant.EPOCH,
            new TradePayload(BigDecimal.TEN, BigDecimal.ONE));

    @Test
    void rapidDropTriggers() {
        assertThat(new RapidDropRule(properties).evaluate(event, state(-3.1, 0, false)))
                .isPresent();
    }

    @Test
    void rapidPumpTriggers() {
        assertThat(new RapidPumpRule(properties).evaluate(event, state(3.1, 0, false)))
                .isPresent();
    }

    @Test
    void abnormalVolumeDoesNotTriggerBeforeBaselineWarmup() {
        assertThat(new AbnormalVolumeRule(properties).evaluate(event, state(0, 10, false)))
                .isEmpty();
    }

    @Test
    void abnormalVolumeTriggersAfterBaselineWarmup() {
        assertThat(new AbnormalVolumeRule(properties).evaluate(event, state(0, 3.1, true)))
                .isPresent();
    }

    @Test
    void rulesDoNotTriggerBelowThreshold() {
        assertThat(new RapidDropRule(properties).evaluate(event, state(-2.9, 2, true)))
                .isEmpty();
        assertThat(new RapidPumpRule(properties).evaluate(event, state(2.9, 2, true)))
                .isEmpty();
        assertThat(new AbnormalVolumeRule(properties).evaluate(event, state(0, 2.9, true)))
                .isEmpty();
    }

    private MarketState state(double return5m, double volumeRatio, boolean baselineReady) {
        return new MarketState(
                "BTCUSDT",
                Instant.EPOCH,
                BigDecimal.TEN,
                0,
                return5m,
                0,
                1,
                2,
                BigDecimal.TEN,
                BigDecimal.ONE,
                volumeRatio,
                baselineReady);
    }
}
