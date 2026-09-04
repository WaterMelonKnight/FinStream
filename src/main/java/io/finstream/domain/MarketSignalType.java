package io.finstream.domain;

/** The kind of canonical market input, independent of its exchange or anomaly output. */
public enum MarketSignalType {
    TRADE,
    FUNDING_RATE,
    OPEN_INTEREST,
    LIQUIDATION
}
