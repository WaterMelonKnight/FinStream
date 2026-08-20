package io.finstream.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketEvent(String source, String symbol, EventType eventType, Instant eventTime,
                          Instant receivedAt, BigDecimal price, BigDecimal quantity) {
    public enum EventType { TRADE }
}
