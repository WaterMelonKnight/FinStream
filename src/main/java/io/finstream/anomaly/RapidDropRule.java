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
public class RapidDropRule implements AnomalyRule {
    private final FinStreamProperties.Rule config;

    public RapidDropRule(FinStreamProperties properties) {
        config = properties.anomaly().rapidDrop();
    }

    @Override
    public String eventType() {
        return "RAPID_DROP";
    }

    @Override
    public Optional<FinancialEvent> evaluate(MarketEvent event, MarketState state) {
        double return5m = state.return5m();
        if (!config.enabled() || return5m > -config.thresholdPercent()) {
            return Optional.empty();
        }
        double score = Math.abs(return5m) / config.thresholdPercent();
        return Optional.of(RuleEventFactory.create(
                event,
                eventType(),
                score,
                event.symbol()
                        + " dropped "
                        + String.format(Locale.ROOT, "%.2f%%", Math.abs(return5m))
                        + " in 5m",
                Map.of("return5m", return5m)));
    }
}
