package io.finstream.connector.binance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

class BinanceOpenInterestConnectorTest {
    private final BinanceOpenInterestNormalizer normalizer =
            new BinanceOpenInterestNormalizer(new ObjectMapper(), Clock.systemUTC());

    @Test
    void disabledProducesNoEventsOrRequests() {
        AtomicInteger requests = new AtomicInteger();
        var connector = connector(false, List.of("BTCUSDT"), symbol -> {
            requests.incrementAndGet(); return Mono.empty();
        });
        StepVerifier.create(connector.events()).verifyComplete();
        assertThat(requests).hasValue(0);
    }

    @Test
    void requestAndMalformedResponseFailuresDoNotPreventLaterPolls() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        AtomicInteger requests = new AtomicInteger();
        var connector = new BinanceOpenInterestConnector(
                true, List.of("BTCUSDT"), Duration.ofSeconds(30), scheduler,
                symbol -> switch (requests.getAndIncrement()) {
                    case 0 -> Mono.error(new IllegalStateException("temporary"));
                    case 1 -> Mono.just("{bad");
                    default -> Mono.just(json(symbol));
                }, normalizer);

        StepVerifier.withVirtualTime(connector::events, () -> scheduler, 1)
                .thenAwait(Duration.ofSeconds(60))
                .assertNext(event -> assertThat(event.symbol()).isEqualTo("BTCUSDT"))
                .thenCancel().verify();
        assertThat(requests).hasValue(3);
    }

    @Test
    void oneSymbolFailureDoesNotAffectOtherSymbolsOrNextPollingCycle() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        AtomicInteger btcRequests = new AtomicInteger();
        var connector = new BinanceOpenInterestConnector(
                true, List.of("BTCUSDT", "ETHUSDT"), Duration.ofSeconds(30), scheduler,
                symbol -> symbol.equals("BTCUSDT") && btcRequests.getAndIncrement() == 0
                        ? Mono.error(new IllegalStateException("BTC failure"))
                        : Mono.just(json(symbol)), normalizer);

        StepVerifier.withVirtualTime(connector::events, () -> scheduler, 2)
                .thenAwait(Duration.ofSeconds(30))
                .assertNext(event -> assertThat(event.symbol()).isEqualTo("ETHUSDT"))
                .assertNext(event -> assertThat(event.symbol()).isEqualTo("BTCUSDT"))
                .thenCancel().verify();
    }

    private BinanceOpenInterestConnector connector(
            boolean enabled, List<String> symbols,
            java.util.function.Function<String, Mono<String>> request) {
        return new BinanceOpenInterestConnector(enabled, symbols, Duration.ofSeconds(30),
                VirtualTimeScheduler.create(), request, normalizer);
    }

    private static String json(String symbol) {
        return "{\"symbol\":\"" + symbol + "\",\"openInterest\":\"123.45\",\"time\":1}";
    }
}
