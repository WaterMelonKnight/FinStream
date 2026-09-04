package io.finstream.domain;

public sealed interface MarketSignalPayload permits TradePayload, FundingRatePayload {}
