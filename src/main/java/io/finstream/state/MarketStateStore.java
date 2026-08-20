package io.finstream.state;

import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketState;
import java.util.Optional;

public interface MarketStateStore {
    MarketState update(MarketEvent event);

    default Optional<MarketState> get(String symbol) {
        return Optional.empty();
    }
}
