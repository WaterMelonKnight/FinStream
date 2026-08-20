package io.finstream.connector.binance;

import io.finstream.config.FinStreamProperties;
import io.finstream.connector.MarketDataConnector;
import io.finstream.domain.MarketEvent;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import java.net.URI;

@Component
public class BinanceMarketDataConnector implements MarketDataConnector {
    private static final Logger log=LoggerFactory.getLogger(BinanceMarketDataConnector.class);
    private final Sinks.Many<MarketEvent> sink=Sinks.many().multicast().onBackpressureBuffer(10_000, false);
    public BinanceMarketDataConnector(FinStreamProperties properties, BinanceTradeNormalizer normalizer) {
        if (!properties.market().binance().enabled()) { log.info("Binance WebSocket disabled"); return; }
        String streams=properties.market().symbols().stream().map(s->s.toLowerCase(Locale.ROOT)+"@aggTrade").reduce((a,b)->a+"/"+b).orElseThrow();
        URI uri=URI.create(properties.market().binance().baseUrl()+streams);
        Flux.defer(() -> { log.info("Binance WebSocket connecting to {} symbols", properties.market().symbols());
            return new ReactorNettyWebSocketClient().execute(uri, session -> { log.info("Binance WebSocket connected");
                return session.receive().map(m->m.getPayloadAsText()).doOnNext(raw->{ try { MarketEvent event=normalizer.normalize(raw); sink.tryEmitNext(event); log.debug("Symbol data received: {}", event.symbol()); } catch(Exception e){ log.warn("Ignored malformed Binance message: {}", e.getMessage()); }}).then();
            }).doFinally(signal->log.warn("Binance WebSocket disconnected: {}", signal));
        }).retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2)).maxBackoff(Duration.ofMinutes(1)).doBeforeRetry(r->log.warn("Binance WebSocket reconnecting after: {}", r.failure().toString())))
          .subscribe(null, e->log.error("Binance connector stopped", e));
    }
    @Override public Flux<MarketEvent> events(){return sink.asFlux();}
}
