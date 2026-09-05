package io.finstream.service;

import io.finstream.anomaly.OpenInterestAnomalyRule;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.OpenInterestPayload;
import io.finstream.domain.OpenInterestState;
import io.finstream.state.OpenInterestStateStore;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenInterestMarketSignalProcessor implements MarketSignalProcessor {
    private final OpenInterestStateStore states;
    private final List<OpenInterestAnomalyRule> rules;

    @Autowired
    public OpenInterestMarketSignalProcessor(
            OpenInterestStateStore states, List<OpenInterestAnomalyRule> rules) {
        this.states = states;
        this.rules = List.copyOf(rules);
    }

    OpenInterestMarketSignalProcessor(OpenInterestStateStore states) {
        this(states, List.of());
    }

    @Override
    public MarketSignalType signalType() {
        return MarketSignalType.OPEN_INTEREST;
    }

    @Override
    public List<FinancialEvent> process(MarketEvent event) {
        if (event.signalType() != MarketSignalType.OPEN_INTEREST
                || !(event.payload() instanceof OpenInterestPayload payload)) {
            throw new IllegalArgumentException(
                    "OPEN_INTEREST processor requires an OpenInterestPayload");
        }
        OpenInterestState state = states.update(new OpenInterestState(
                event.source(), event.symbol(), payload.openInterest(),
                null, null, null, null, null,
                event.eventTime(), event.receivedAt()));
        // An ignored out-of-order input returns the existing latest state and must not re-evaluate it.
        if (!state.eventTime().equals(event.eventTime())) return List.of();
        return rules.stream().map(rule -> rule.evaluate(event, state))
                .flatMap(java.util.Optional::stream).toList();
    }
}
