package io.finstream.service;

import io.finstream.anomaly.FundingRateAnomalyRule;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.FundingRatePayload;
import io.finstream.domain.FundingRateState;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.state.FundingRateStateStore;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FundingRateMarketSignalProcessor implements MarketSignalProcessor {
    private final FundingRateStateStore states;
    private final List<FundingRateAnomalyRule> rules;

    public FundingRateMarketSignalProcessor(
            FundingRateStateStore states, List<FundingRateAnomalyRule> rules) {
        this.states = states;
        this.rules = List.copyOf(rules);
    }

    @Override
    public MarketSignalType signalType() {
        return MarketSignalType.FUNDING_RATE;
    }

    @Override
    public List<FinancialEvent> process(MarketEvent event) {
        if (event.signalType() != MarketSignalType.FUNDING_RATE
                || !(event.payload() instanceof FundingRatePayload payload)) {
            throw new IllegalArgumentException("FUNDING_RATE processor requires a FundingRatePayload");
        }
        FundingRateState state = states.update(new FundingRateState(
                event.symbol(), event.source(), payload.fundingRate(), payload.markPrice(),
                payload.indexPrice(), payload.nextFundingTime(), event.eventTime(),
                event.receivedAt()));
        return rules.stream().map(rule -> rule.evaluate(event, state))
                .flatMap(java.util.Optional::stream).toList();
    }
}
