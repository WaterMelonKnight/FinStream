package io.finstream.query;

import io.finstream.state.MarketStateStore;
import org.springframework.stereotype.Service;

@Service
public class MarketQueryService {
    private final MarketStateStore states;

    public MarketQueryService(MarketStateStore states) { this.states = states; }

    public MarketStateResponse getMarketState(String symbol) {
        String normalized = QueryParameters.symbol(symbol, true);
        return states.get(normalized).map(MarketStateResponse::from).orElseThrow(() ->
                new QueryException("MARKET_STATE_NOT_FOUND",
                        "No current market state for " + normalized, true));
    }
}
