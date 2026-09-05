package io.finstream.service;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import io.finstream.domain.OpenInterestPayload;
import io.finstream.domain.OpenInterestState;
import io.finstream.state.OpenInterestStateStore;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpenInterestMarketSignalProcessor implements MarketSignalProcessor {
    private final OpenInterestStateStore states;

    public OpenInterestMarketSignalProcessor(OpenInterestStateStore states) {
        this.states = states;
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
        states.update(new OpenInterestState(
                event.source(), event.symbol(), payload.openInterest(),
                event.eventTime(), event.receivedAt()));
        return List.of();
    }
}
