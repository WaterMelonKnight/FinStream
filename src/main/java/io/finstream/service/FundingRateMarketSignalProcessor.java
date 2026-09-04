package io.finstream.service;

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

    public FundingRateMarketSignalProcessor(FundingRateStateStore states) {
        this.states = states;
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
        states.update(new FundingRateState(
                event.symbol(), event.source(), payload.fundingRate(), payload.markPrice(),
                payload.indexPrice(), payload.nextFundingTime(), event.eventTime(),
                event.receivedAt()));
        return List.of();
    }
}
