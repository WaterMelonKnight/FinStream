package io.finstream.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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
    void blockingRepositoryRunsOffTheCallingThread() {
        FinancialEventRepository repository = mock(FinancialEventRepository.class);
        CompletableFuture<Void> saveEntered = new CompletableFuture<>();
        CompletableFuture<Void> allowSave = new CompletableFuture<>();
        CompletableFuture<Void> saveFinished = new CompletableFuture<>();
        AtomicReference<String> saveThread = new AtomicReference<>();
        FinancialEvent financialEvent = financialEvent();
        when(repository.save(any())).thenAnswer(invocation -> {
            saveThread.set(Thread.currentThread().getName());
            saveEntered.complete(null);
            try {
                allowSave.join();
                return invocation.getArgument(0);
            } finally {
                saveFinished.complete(null);
            }
        });
        MarketEventPipeline pipeline = pipeline(repository, financialEvent);

        pipeline.process(marketEvent());

        await().until(saveEntered::isDone);
        assertThat(saveThread.get()).startsWith("test-persistence");
        allowSave.complete(null);
        await().until(saveFinished::isDone);
        verify(repository).save(financialEvent);
    }

    @Test
    void persistenceFailureReleasesReservationSoNextEventCanRetry() {
        FinancialEventRepository repository = mock(FinancialEventRepository.class);
        AtomicInteger attempts = new AtomicInteger();
        CompletableFuture<FinancialEvent> successfulSave = new CompletableFuture<>();
        when(repository.save(any())).thenAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("database unavailable");
            }
            FinancialEvent saved = invocation.getArgument(0);
            successfulSave.complete(saved);
            return saved;
        });
        FinancialEvent financialEvent = financialEvent();
        // An immediate scheduler makes process() return only after doOnError has released the
        // reservation. Production continues to use boundedElastic; only this test is synchronous.
        MarketEventPipeline pipeline = pipeline(repository, financialEvent, Schedulers.immediate());

        pipeline.process(marketEvent());
        verify(repository).save(financialEvent);
        pipeline.process(marketEvent());

        await().until(successfulSave::isDone);
        assertThat(successfulSave.join()).isSameAs(financialEvent);
        verify(repository, times(2)).save(any());
    }

    private MarketEventPipeline pipeline(
            FinancialEventRepository repository, FinancialEvent financialEvent) {
        return pipeline(repository, financialEvent, scheduler);
    }

    private MarketEventPipeline pipeline(
            FinancialEventRepository repository,
            FinancialEvent financialEvent,
            Scheduler persistenceScheduler) {
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
                persistenceScheduler);
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
