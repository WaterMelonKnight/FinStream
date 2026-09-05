package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OpenInterestState(
        String source, String symbol, BigDecimal openInterest,
        Instant eventTime, Instant receivedAt) {}
