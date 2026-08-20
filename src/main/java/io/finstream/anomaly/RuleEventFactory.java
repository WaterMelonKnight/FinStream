package io.finstream.anomaly;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

final class RuleEventFactory {
    private RuleEventFactory() {}

    static FinancialEvent create(
            MarketEvent event,
            String type,
            double score,
            String summary,
            Map<String, Object> metrics) {
        return new FinancialEvent(
                UUID.randomUUID(),
                event.source(),
                event.symbol(),
                type,
                event.eventTime(),
                Clock.systemUTC().instant(),
                score >= 2 ? "HIGH" : "MEDIUM",
                score,
                summary,
                metrics,
                Map.of("lastPrice", event.price()));
    }
}
