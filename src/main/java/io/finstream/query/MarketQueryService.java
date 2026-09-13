package io.finstream.query;

import io.finstream.state.FundingRateStateStore;
import io.finstream.state.MarketStateStore;
import io.finstream.state.OpenInterestStateStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketQueryService {
    private static final String SPOT = "SPOT";
    private static final String USD_M_FUTURES = "USD_M_FUTURES";

    private final MarketStateStore states;
    private final FundingRateStateStore fundingRateStates;
    private final OpenInterestStateStore openInterestStates;
    private final Clock clock;

    @Autowired
    public MarketQueryService(MarketStateStore states, FundingRateStateStore fundingRateStates,
            OpenInterestStateStore openInterestStates, Clock clock) {
        this.states = states;
        this.fundingRateStates = fundingRateStates;
        this.openInterestStates = openInterestStates;
        this.clock = clock;
    }

    public MarketQueryService(MarketStateStore states, FundingRateStateStore fundingRateStates,
            OpenInterestStateStore openInterestStates) {
        this(states, fundingRateStates, openInterestStates, Clock.systemUTC());
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

    public MarketContextResponse getMarketContext(String symbol) {
        String normalized = QueryParameters.symbol(symbol, true);
        Instant generatedAt = clock.instant();
        var market = states.get(normalized)
                .map(state -> MarketContextResponse.MarketContextSection.available(
                        SPOT, ageSeconds(state.receivedAt(), generatedAt), state.receivedAt(),
                        MarketStateResponse.from(state)))
                .orElseGet(() -> MarketContextResponse.MarketContextSection.missing(SPOT));
        var funding = fundingRateStates.get(normalized)
                .map(state -> MarketContextResponse.MarketContextSection.available(
                        USD_M_FUTURES, ageSeconds(state.receivedAt(), generatedAt), state.receivedAt(),
                        FundingRateStateResponse.from(state)))
                .orElseGet(() -> MarketContextResponse.MarketContextSection.missing(USD_M_FUTURES));
        var openInterest = openInterestStates.get(normalized)
                .map(state -> MarketContextResponse.MarketContextSection.available(
                        USD_M_FUTURES, ageSeconds(state.receivedAt(), generatedAt), state.receivedAt(),
                        OpenInterestStateResponse.from(state)))
                .orElseGet(() -> MarketContextResponse.MarketContextSection.missing(USD_M_FUTURES));

        if (!market.available() && !funding.available() && !openInterest.available()) {
            throw new QueryException("MARKET_CONTEXT_NOT_FOUND",
                    "No current market context for " + normalized, true);
        }
        return new MarketContextResponse(normalized, generatedAt, market, funding, openInterest);
    }

    private long ageSeconds(Instant receivedAt, Instant generatedAt) {
        return Math.max(0, Duration.between(receivedAt, generatedAt).getSeconds());
    }
}
