package io.finstream.state;

import io.finstream.domain.FundingRateState;
import java.util.Optional;

public interface FundingRateStateStore {
    FundingRateState update(FundingRateState state);

    Optional<FundingRateState> get(String symbol);
}
