package io.finstream.state;

import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketState;

public interface MarketStateStore {
    MarketState update(MarketEvent event);
}
