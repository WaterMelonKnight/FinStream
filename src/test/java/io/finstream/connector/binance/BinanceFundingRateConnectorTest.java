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

class BinanceFundingRateConnectorTest {
    @Test
    void requestFailureDoesNotPreventNextPoll() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        AtomicInteger requests = new AtomicInteger();
        BinanceFundingRateNormalizer normalizer =
                new BinanceFundingRateNormalizer(new ObjectMapper(), Clock.systemUTC());
        BinanceFundingRateConnector connector = new BinanceFundingRateConnector(
                true, List.of("BTCUSDT"), Duration.ofSeconds(60), scheduler,
                symbol -> requests.getAndIncrement() == 0
                        ? Mono.error(new IllegalStateException("temporary failure"))
                        : Mono.just("""
                          {"symbol":"BTCUSDT","markPrice":"100","indexPrice":"99",
                           "lastFundingRate":"0.0001","nextFundingTime":60000,"time":1000}
                          """),
                normalizer);

        StepVerifier.withVirtualTime(connector::events, () -> scheduler, 1)
                .thenAwait(Duration.ofSeconds(60))
                .assertNext(event -> assertThat(event.symbol()).isEqualTo("BTCUSDT"))
                .thenCancel()
                .verify();
        assertThat(requests).hasValue(2);
    }
}
