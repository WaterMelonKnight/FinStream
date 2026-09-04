package io.finstream.service;

import io.finstream.anomaly.EventCooldown;
import io.finstream.anomaly.EventCooldown.Reservation;
import io.finstream.connector.MarketDataConnector;
import io.finstream.domain.FinancialEvent;
import io.finstream.domain.MarketEvent;
import io.finstream.persistence.FinancialEventRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Service
public class MarketEventPipeline {
    private static final Logger log = LoggerFactory.getLogger(MarketEventPipeline.class);

    private final List<MarketDataConnector> connectors;
    private final MarketSignalRouter router;
    private final EventCooldown cooldown;
    private final FinancialEventRepository repository;
    private final Scheduler persistenceScheduler;

    @Autowired
    public MarketEventPipeline(
            List<MarketDataConnector> connectors,
            MarketSignalRouter router,
            EventCooldown cooldown,
            FinancialEventRepository repository) {
        this(connectors, router, cooldown, repository, Schedulers.boundedElastic());
    }

    MarketEventPipeline(
            List<MarketDataConnector> connectors,
            MarketSignalRouter router,
            EventCooldown cooldown,
            FinancialEventRepository repository,
            Scheduler persistenceScheduler) {
        this.connectors = List.copyOf(connectors);
        this.router = router;
        this.cooldown = cooldown;
        this.repository = repository;
        this.persistenceScheduler = persistenceScheduler;
    }

    @PostConstruct
    void start() {
        reactor.core.publisher.Flux.merge(
                        connectors.stream().map(MarketDataConnector::events).toList())
                .subscribe(
                this::process,
                error -> log.error("Market pipeline failed", error));
    }

    void process(MarketEvent event) {
        router.route(event).forEach(this::persistIfNotCoolingDown);
    }

    private void persistIfNotCoolingDown(FinancialEvent event) {
        cooldown.reserve(event.getSymbol(), event.getEventType()).ifPresent(reservation ->
                persist(event, reservation).subscribe());
    }

    private Mono<Void> persist(FinancialEvent event, Reservation reservation) {
        return Mono.fromCallable(() -> repository.save(event))
                .subscribeOn(persistenceScheduler)
                .doOnSuccess(saved -> {
                    cooldown.commit(reservation);
                    log.info("Anomaly triggered: {} {} score={}",
                            saved.getSymbol(), saved.getEventType(), saved.getAnomalyScore());
                    log.info("FinancialEvent saved: {}", saved.getId());
                })
                .doOnError(error -> {
                    cooldown.release(reservation);
                    log.error("Failed to save FinancialEvent {}", event.getId(), error);
                })
                .onErrorComplete()
                .then();
    }
}
