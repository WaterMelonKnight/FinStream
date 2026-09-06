package io.finstream.anomaly;

import io.finstream.config.FinStreamProperties;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.OpenInterestState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenInterestSurgeRule implements OpenInterestAnomalyRule {
    public static final String EVENT_TYPE = "OPEN_INTEREST_SURGE";
    private static final int WINDOW_MINUTES = 15;
    private final FinStreamProperties.OpenInterestSurge config;
    private final Clock clock;

    @Autowired
    public OpenInterestSurgeRule(FinStreamProperties properties) {
        this(properties.anomaly().openInterestSurge(), Clock.systemUTC());
    }

    OpenInterestSurgeRule(FinStreamProperties.OpenInterestSurge config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    @Override
    public Optional<FinancialEvent> evaluate(MarketEvent event, OpenInterestState state) {
        BigDecimal change = state.change15mPercent();
        if (!config.enabled() || change == null || change.signum() < 0
                || change.compareTo(config.thresholdPercent()) < 0) return Optional.empty();

        double score = change.divide(config.thresholdPercent(), 12, RoundingMode.HALF_UP).doubleValue();
        if (!Double.isFinite(score)) score = Double.MAX_VALUE;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("openInterest", state.openInterest());
        metrics.put("referenceOpenInterest", state.referenceOpenInterest15m());
        metrics.put("change15mPercent", change);
        metrics.put("thresholdPercent", config.thresholdPercent());
        metrics.put("windowMinutes", WINDOW_MINUTES);
        String summary = String.format(Locale.ROOT,
                "%s open interest increased %s%% over approximately 15 minutes, meeting or exceeding the configured %s%% threshold.",
                event.symbol(), change.stripTrailingZeros().toPlainString(),
                config.thresholdPercent().stripTrailingZeros().toPlainString());
        return Optional.of(new FinancialEvent(UUID.randomUUID(), event.source(), event.symbol(),
                EVENT_TYPE, event.eventTime(), clock.instant(), score >= 2 ? "HIGH" : "MEDIUM",
                score, summary, metrics, Map.of(
                        "referenceEventTime", state.referenceEventTime15m().toString(),
                        "currentEventTime", state.eventTime().toString(),
                        "receivedAt", state.receivedAt().toString())));
    }
}
