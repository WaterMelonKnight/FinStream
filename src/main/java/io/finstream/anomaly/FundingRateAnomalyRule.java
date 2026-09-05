package io.finstream.anomaly;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.FundingRateState;
import io.finstream.domain.MarketEvent;
import java.util.Optional;

public interface FundingRateAnomalyRule {
    Optional<FinancialEvent> evaluate(MarketEvent event, FundingRateState state);
}
