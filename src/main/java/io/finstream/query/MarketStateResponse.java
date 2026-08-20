package io.finstream.query;

import io.finstream.domain.MarketState;
import java.math.BigDecimal;
import java.time.Instant;

public record MarketStateResponse(
        String symbol,
        Instant asOf,
        BigDecimal lastPrice,
        double return1m,
        double return5m,
        double return30m,
        double volume1m,
        double volume5m,
        BigDecimal high5m,
        BigDecimal low5m,
        double volumeRatio,
        boolean volumeBaselineReady) {
    public static MarketStateResponse from(MarketState state) {
        return new MarketStateResponse(state.symbol(), state.asOf(), state.lastPrice(),
                state.return1m(), state.return5m(), state.return30m(), state.volume1m(),
                state.volume5m(), state.high5m(), state.low5m(), state.volumeRatio(),
                state.volumeBaselineReady());
    }
}
