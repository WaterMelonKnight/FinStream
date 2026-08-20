package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.finstream.anomaly.AnomalyRule;
import io.finstream.anomaly.EventCooldown;
import io.finstream.config.FinStreamProperties;
import io.finstream.connector.MarketDataConnector;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.domain.MarketState;
import io.finstream.persistence.FinancialEventRepository;
import io.finstream.state.MarketStateStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

class MarketEventPipelineTest {
    private final Scheduler scheduler = Schedulers.newBoundedElastic(1, 10, "test-persistence");

    @AfterEach
    void disposeScheduler() {
        scheduler.dispose();
    }

    @Test
    void blockingRepositoryRunsOffTheCallingThread() throws Exception {
        FinancialEventRepository repository = mock(FinancialEventRepository.class);
        CountDownLatch saveEntered = new CountDownLatch(1);
        CountDownLatch allowSave = new CountDownLatch(1);
        CountDownLatch saveFinished = new CountDownLatch(1);
        AtomicReference<String> saveThread = new AtomicReference<>();
        FinancialEvent financialEvent = financialEvent();
        when(repository.save(any())).thenAnswer(invocation -> {
            saveThread.set(Thread.currentThread().getName());
            saveEntered.countDown();
            allowSave.await(2, TimeUnit.SECONDS);
            saveFinished.countDown();
            return invocation.getArgument(0);
        });
        MarketEventPipeline pipeline = pipeline(repository, financialEvent);

        pipeline.process(marketEvent());

        assertThat(saveEntered.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(saveThread.get()).startsWith("test-persistence");
        allowSave.countDown();
        assertThat(saveFinished.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void persistenceFailureReleasesReservationSoNextEventCanRetry() throws Exception {
        FinancialEventRepository repository = mock(FinancialEventRepository.class);
        CountDownLatch secondSave = new CountDownLatch(1);
        when(repository.save(any()))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenAnswer(invocation -> {
                    secondSave.countDown();
                    return invocation.getArgument(0);
                });
        MarketEventPipeline pipeline = pipeline(repository, financialEvent());

        pipeline.process(marketEvent());
        awaitRepositoryCalls(repository);
        pipeline.process(marketEvent());

        assertThat(secondSave.await(2, TimeUnit.SECONDS)).isTrue();
        verify(repository, times(2)).save(any());
    }

    private void awaitRepositoryCalls(FinancialEventRepository repository)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            try {
                verify(repository).save(any());
                return;
            } catch (AssertionError ignored) {
                Thread.sleep(10);
            }
        }
        verify(repository).save(any());
    }

    private MarketEventPipeline pipeline(
            FinancialEventRepository repository, FinancialEvent financialEvent) {
        MarketDataConnector connector = Flux::empty;
        MarketStateStore states = ignored -> mock(MarketState.class);
        AnomalyRule rule = new AnomalyRule() {
            @Override
            public String eventType() {
                return "RAPID_DROP";
            }

            @Override
            public Optional<FinancialEvent> evaluate(MarketEvent event, MarketState state) {
                return Optional.of(financialEvent);
            }
        };
        return new MarketEventPipeline(
                connector,
                states,
                List.of(rule),
                new EventCooldown(properties()),
                repository,
                scheduler);
    }

    private FinStreamProperties properties() {
        return new FinStreamProperties(
                null,
                new FinStreamProperties.Anomaly(
                        Duration.ofMinutes(5), null, null, null));
    }

    private MarketEvent marketEvent() {
        return new MarketEvent(
                "TEST",
                "BTCUSDT",
                MarketEvent.EventType.TRADE,
                Instant.EPOCH,
                Instant.EPOCH,
                BigDecimal.TEN,
                BigDecimal.ONE);
    }

    private FinancialEvent financialEvent() {
        return new FinancialEvent(
                UUID.randomUUID(),
                "TEST",
                "BTCUSDT",
                "RAPID_DROP",
                Instant.EPOCH,
                Instant.EPOCH,
                "HIGH",
                2,
                "drop",
                Map.of("return5m", -6),
                Map.of());
    }
}
