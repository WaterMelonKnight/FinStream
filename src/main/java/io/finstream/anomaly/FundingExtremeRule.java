package io.finstream.anomaly;

import io.finstream.config.FinStreamProperties;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.FundingRateState;
import io.finstream.domain.MarketEvent;
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
public class FundingExtremeRule implements FundingRateAnomalyRule {
    public static final String EVENT_TYPE = "FUNDING_EXTREME";

    private final FinStreamProperties.FundingExtreme config;
    private final Clock clock;

    @Autowired
    public FundingExtremeRule(FinStreamProperties properties) {
        this(properties.anomaly().fundingExtreme(), Clock.systemUTC());
    }

    FundingExtremeRule(FinStreamProperties.FundingExtreme config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    @Override
    public Optional<FinancialEvent> evaluate(MarketEvent event, FundingRateState state) {
        BigDecimal absoluteRate = state.fundingRate().abs();
        if (!config.enabled() || absoluteRate.compareTo(config.threshold()) < 0) {
            return Optional.empty();
        }

        double score = absoluteRate.divide(config.threshold(), 12, RoundingMode.HALF_UP).doubleValue();
        if (!Double.isFinite(score)) score = Double.MAX_VALUE;
        String direction = state.fundingRate().signum() < 0 ? "NEGATIVE" : "POSITIVE";
        BigDecimal ratePercent = state.fundingRate().movePointRight(2);
        BigDecimal thresholdPercent = config.threshold().movePointRight(2);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("fundingRate", state.fundingRate());
        metrics.put("fundingRatePercent", ratePercent);
        metrics.put("absoluteFundingRate", absoluteRate);
        metrics.put("threshold", config.threshold());
        metrics.put("thresholdPercent", thresholdPercent);
        metrics.put("markPrice", state.markPrice());
        metrics.put("indexPrice", state.indexPrice());
        metrics.put("direction", direction);
        if (state.nextFundingTime() != null) metrics.put("nextFundingTime", state.nextFundingTime().toString());

        String summary = String.format(Locale.ROOT,
                "%s funding rate reached %s%%, exceeding the configured absolute %s%% threshold.",
                event.symbol(), signed(ratePercent), thresholdPercent.stripTrailingZeros().toPlainString());
        return Optional.of(new FinancialEvent(
                UUID.randomUUID(), event.source(), event.symbol(), EVENT_TYPE, event.eventTime(),
                clock.instant(), score >= 2 ? "HIGH" : "MEDIUM", score, summary, metrics,
                Map.of("direction", direction, "receivedAt", state.receivedAt().toString())));
    }

    private String signed(BigDecimal value) {
        String formatted = value.stripTrailingZeros().toPlainString();
        return value.signum() >= 0 ? "+" + formatted : formatted;
    }
}
