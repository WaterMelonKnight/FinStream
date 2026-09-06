package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OpenInterestState(
        String source, String symbol, BigDecimal openInterest,
        BigDecimal change5mPercent, BigDecimal change15mPercent, BigDecimal change30mPercent,
        BigDecimal referenceOpenInterest15m, Instant referenceEventTime15m,
        Instant eventTime, Instant receivedAt) {
    public OpenInterestState(
            String source, String symbol, BigDecimal openInterest,
            Instant eventTime, Instant receivedAt) {
        this(source, symbol, openInterest, null, null, null, null, null, eventTime, receivedAt);
    }
}
