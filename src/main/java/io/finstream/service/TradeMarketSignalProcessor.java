package io.finstream.service;

import io.finstream.anomaly.AnomalyRule;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.MarketState;
import io.finstream.state.MarketStateStore;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TradeMarketSignalProcessor implements MarketSignalProcessor {
    private final MarketStateStore states;
    private final List<AnomalyRule> rules;

    public TradeMarketSignalProcessor(MarketStateStore states, List<AnomalyRule> rules) {
        this.states = states;
        this.rules = List.copyOf(rules);
    }

    @Override
    public MarketSignalType signalType() {
        return MarketSignalType.TRADE;
    }

    @Override
    public List<FinancialEvent> process(MarketEvent event) {
        MarketState state = states.update(event);
        return rules.stream()
                .map(rule -> rule.evaluate(event, state))
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
