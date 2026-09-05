package io.finstream.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record OpenInterestPayload(BigDecimal openInterest) implements MarketSignalPayload {
    public OpenInterestPayload {
        Objects.requireNonNull(openInterest, "openInterest");
    }
}
