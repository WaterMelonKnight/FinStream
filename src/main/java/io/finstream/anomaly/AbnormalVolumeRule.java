package io.finstream.anomaly;

import io.finstream.config.FinStreamProperties;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketState;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AbnormalVolumeRule implements AnomalyRule {
    private final FinStreamProperties.VolumeRule config;

    public AbnormalVolumeRule(FinStreamProperties properties) {
        config = properties.anomaly().abnormalVolume();
    }

    @Override
    public String eventType() {
        return "ABNORMAL_VOLUME";
    }

    @Override
    public Optional<FinancialEvent> evaluate(MarketEvent event, MarketState state) {
        double ratio = state.volumeRatio();
        if (!config.enabled() || !state.volumeBaselineReady() || ratio < config.ratio()) {
            return Optional.empty();
        }
        return Optional.of(RuleEventFactory.create(
                event,
                eventType(),
                ratio / config.ratio(),
                event.symbol()
                        + " volume is "
                        + String.format(Locale.ROOT, "%.2fx", ratio)
                        + " baseline",
                Map.of("volume1m", state.volume1m(), "volumeRatio", ratio)));
    }
}
