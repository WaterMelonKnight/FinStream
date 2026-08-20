package io.finstream.anomaly;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketState;
import java.util.Optional;

public interface AnomalyRule {
    String eventType();

    Optional<FinancialEvent> evaluate(MarketEvent event, MarketState state);
}
