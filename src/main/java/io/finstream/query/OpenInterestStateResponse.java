package io.finstream.query;

import io.finstream.domain.OpenInterestState;
import java.math.BigDecimal;
import java.time.Instant;

public record OpenInterestStateResponse(
        String source, String symbol, BigDecimal openInterest,
        Instant eventTime, Instant receivedAt) {
    public static OpenInterestStateResponse from(OpenInterestState state) {
        return new OpenInterestStateResponse(
                state.source(), state.symbol(), state.openInterest(),
                state.eventTime(), state.receivedAt());
    }
}
