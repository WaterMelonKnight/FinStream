package io.finstream.query;

import io.finstream.domain.OpenInterestState;
import java.math.BigDecimal;
import java.time.Instant;

public record OpenInterestStateResponse(
        String source, String symbol, BigDecimal openInterest,
        BigDecimal change5mPercent, BigDecimal change15mPercent, BigDecimal change30mPercent,
        Instant eventTime, Instant receivedAt) {
    public OpenInterestStateResponse(
            String source, String symbol, BigDecimal openInterest,
            Instant eventTime, Instant receivedAt) {
        this(source, symbol, openInterest, null, null, null, eventTime, receivedAt);
    }
    public static OpenInterestStateResponse from(OpenInterestState state) {
        return new OpenInterestStateResponse(
                state.source(), state.symbol(), state.openInterest(), state.change5mPercent(),
                state.change15mPercent(), state.change30mPercent(),
                state.eventTime(), state.receivedAt());
    }
}
