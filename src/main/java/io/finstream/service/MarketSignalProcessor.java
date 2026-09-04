package io.finstream.service;

import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketSignalType;
import java.util.List;

/** Processes one kind of canonical market signal and emits zero or more anomalies. */
public interface MarketSignalProcessor {
    MarketSignalType signalType();

    List<FinancialEvent> process(MarketEvent event);
}
