package io.finstream.connector;

import io.finstream.domain.MarketEvent;
import reactor.core.publisher.Flux;

public interface MarketDataConnector { Flux<MarketEvent> events(); }
