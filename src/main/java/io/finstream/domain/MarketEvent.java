package io.finstream.domain;

import java.time.Instant;
import java.util.Objects;

public record MarketEvent(String source, String symbol, MarketSignalType signalType,
                          Instant eventTime, Instant receivedAt, MarketSignalPayload payload) {
    public MarketEvent {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(symbol, "symbol");
        Objects.requireNonNull(signalType, "signalType");
        Objects.requireNonNull(eventTime, "eventTime");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(payload, "payload");
    }
}
