package io.finstream.connector.binance;

import io.finstream.config.FinStreamProperties;
import io.finstream.connector.MarketDataConnector;
import io.finstream.domain.MarketEvent;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Component
public class BinanceOpenInterestConnector implements MarketDataConnector {
    private static final Logger log = LoggerFactory.getLogger(BinanceOpenInterestConnector.class);
    private final boolean enabled;
    private final List<String> symbols;
    private final Duration pollInterval;
    private final Scheduler scheduler;
    private final Function<String, Mono<String>> request;
    private final BinanceOpenInterestNormalizer normalizer;

    @Autowired
    public BinanceOpenInterestConnector(
            FinStreamProperties properties, BinanceOpenInterestNormalizer normalizer,
            WebClient.Builder webClientBuilder) {
        this(properties.market().binance().openInterest().enabled(),
                properties.market().symbols(),
                properties.market().binance().openInterest().pollInterval(),
                Schedulers.parallel(),
                requestFunction(webClientBuilder,
                        properties.market().binance().openInterest().baseUrl()), normalizer);
    }

    BinanceOpenInterestConnector(
            boolean enabled, List<String> symbols, Duration pollInterval, Scheduler scheduler,
            Function<String, Mono<String>> request, BinanceOpenInterestNormalizer normalizer) {
        this.enabled = enabled;
        this.symbols = List.copyOf(symbols);
        this.pollInterval = pollInterval;
        this.scheduler = scheduler;
        this.request = request;
        this.normalizer = normalizer;
    }

    @Override
    public Flux<MarketEvent> events() {
        if (!enabled) {
            log.info("Binance open interest polling disabled");
            return Flux.empty();
        }
        return Flux.interval(Duration.ZERO, pollInterval, scheduler)
                .concatMap(ignored -> Flux.fromIterable(symbols).concatMap(this::pollSymbol));
    }

    private Mono<MarketEvent> pollSymbol(String symbol) {
        return request.apply(symbol).map(raw -> normalize(raw, symbol)).onErrorResume(error -> {
            log.warn("Binance open interest request failed for {}: {}", symbol, error.getMessage());
            return Mono.empty();
        });
    }

    private MarketEvent normalize(String raw, String requestedSymbol) {
        try {
            MarketEvent event = normalizer.normalize(raw);
            if (!requestedSymbol.equals(event.symbol())) {
                throw new IllegalArgumentException("Unexpected Binance open interest symbol");
            }
            return event;
        } catch (Exception error) {
            throw new IllegalArgumentException("Malformed Binance open interest response", error);
        }
    }

    private static Function<String, Mono<String>> requestFunction(
            WebClient.Builder builder, String baseUrl) {
        WebClient client = builder.baseUrl(baseUrl).build();
        return symbol -> client.get().uri(uri -> uri.path("/fapi/v1/openInterest")
                        .queryParam("symbol", symbol).build())
                .retrieve().bodyToMono(String.class);
    }
}
