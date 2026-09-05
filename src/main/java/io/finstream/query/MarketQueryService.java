package io.finstream.query;

import io.finstream.state.FundingRateStateStore;
import io.finstream.state.MarketStateStore;
import io.finstream.state.OpenInterestStateStore;
import org.springframework.stereotype.Service;

@Service
public class MarketQueryService {
    private final MarketStateStore states;
    private final FundingRateStateStore fundingRateStates;
    private final OpenInterestStateStore openInterestStates;

    public MarketQueryService(MarketStateStore states, FundingRateStateStore fundingRateStates,
            OpenInterestStateStore openInterestStates) {
        this.states = states;
        this.fundingRateStates = fundingRateStates;
        this.openInterestStates = openInterestStates;
    }

    public MarketStateResponse getMarketState(String symbol) {
        String normalized = QueryParameters.symbol(symbol, true);
        return states.get(normalized).map(MarketStateResponse::from).orElseThrow(() ->
                new QueryException("MARKET_STATE_NOT_FOUND",
                        "No current market state for " + normalized, true));
    }

    public FundingRateStateResponse getFundingRateState(String symbol) {
        String normalized = QueryParameters.symbol(symbol, true);
        return fundingRateStates.get(normalized).map(FundingRateStateResponse::from).orElseThrow(() ->
                new QueryException("FUNDING_RATE_STATE_NOT_FOUND",
                        "No current funding rate state for " + normalized, true));
    }

    public OpenInterestStateResponse getOpenInterestState(String symbol) {
        String normalized = QueryParameters.symbol(symbol, true);
        return openInterestStates.get(normalized).map(OpenInterestStateResponse::from)
                .orElseThrow(() -> new QueryException("OPEN_INTEREST_STATE_NOT_FOUND",
                        "No current open interest state for " + normalized, true));
    }
}
