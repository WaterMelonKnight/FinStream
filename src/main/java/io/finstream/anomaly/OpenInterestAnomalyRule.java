package io.finstream.anomaly;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.OpenInterestState;
import java.util.Optional;

public interface OpenInterestAnomalyRule {
    Optional<FinancialEvent> evaluate(MarketEvent event, OpenInterestState state);
}
