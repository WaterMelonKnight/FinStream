package io.finstream.state;

import io.finstream.domain.FundingRateState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryFundingRateStateStore implements FundingRateStateStore {
    private final Map<String, FundingRateState> latest = new ConcurrentHashMap<>();

    @Override
    public FundingRateState update(FundingRateState state) {
        latest.put(state.symbol(), state);
        return state;
    }

    @Override
    public Optional<FundingRateState> get(String symbol) {
        return Optional.ofNullable(latest.get(symbol));
    }
}
