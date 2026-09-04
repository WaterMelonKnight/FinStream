package io.finstream.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record TradePayload(BigDecimal price, BigDecimal quantity) implements MarketSignalPayload {
    public TradePayload {
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(quantity, "quantity");
    }
}
