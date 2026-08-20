package io.finstream.connector.binance;

import io.finstream.config.FinStreamProperties;
import io.finstream.connector.MarketDataConnector;
import io.finstream.domain.MarketEvent;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

@Component
public class BinanceMarketDataConnector implements MarketDataConnector {
    private static final Logger log = LoggerFactory.getLogger(BinanceMarketDataConnector.class);

    private final Sinks.Many<MarketEvent> sink =
            Sinks.many().multicast().onBackpressureBuffer(10_000, false);

    public BinanceMarketDataConnector(
            FinStreamProperties properties, BinanceTradeNormalizer normalizer) {
        if (!properties.market().binance().enabled()) {
            log.info("Binance WebSocket disabled");
            return;
        }
        String streams = properties.market().symbols().stream()
                .map(symbol -> symbol.toLowerCase(Locale.ROOT) + "@aggTrade")
                .reduce((left, right) -> left + "/" + right)
                .orElseThrow();
        URI uri = URI.create(properties.market().binance().baseUrl() + streams);

        Flux.defer(() -> connect(uri, properties, normalizer))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofMinutes(1))
                        .doBeforeRetry(retry -> log.warn(
                                "Binance WebSocket reconnecting after: {}",
                                retry.failure().toString())))
                .subscribe(null, error -> log.error("Binance connector stopped", error));
    }

    private Flux<Void> connect(
            URI uri, FinStreamProperties properties, BinanceTradeNormalizer normalizer) {
        log.info("Binance WebSocket connecting to {} symbols", properties.market().symbols());
        return new ReactorNettyWebSocketClient()
                .execute(uri, session -> {
                    log.info("Binance WebSocket connected");
                    return session.receive()
                            .map(message -> message.getPayloadAsText())
                            .doOnNext(raw -> normalize(raw, normalizer))
                            .then();
                })
                .doFinally(signal -> log.warn("Binance WebSocket disconnected: {}", signal))
                .flux();
    }

    private void normalize(String raw, BinanceTradeNormalizer normalizer) {
        try {
            MarketEvent event = normalizer.normalize(raw);
            sink.tryEmitNext(event);
            log.debug("Symbol data received: {}", event.symbol());
        } catch (Exception error) {
            log.warn("Ignored malformed Binance message: {}", error.getMessage());
        }
    }

    @Override
    public Flux<MarketEvent> events() {
        return sink.asFlux();
    }
}
