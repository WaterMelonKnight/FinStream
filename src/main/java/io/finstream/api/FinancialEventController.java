package io.finstream.api;

import io.finstream.query.FinancialEventQueryService;
import io.finstream.query.FinancialEventResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/events")
public class FinancialEventController {
    private final FinancialEventQueryService service;

    public FinancialEventController(FinancialEventQueryService service) { this.service = service; }

    @GetMapping
    public Mono<List<FinancialEventResponse>> recent(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Integer limit) {
        return blocking(() -> service.getRecentEvents(symbol, eventType, limit));
    }

    @GetMapping("/abnormal")
    public Mono<List<FinancialEventResponse>> abnormal(
            @RequestParam(required = false) Instant since,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) Integer limit) {
        return blocking(() -> service.getAbnormalEvents(since, minScore, symbol, limit));
    }

    @GetMapping("/{eventId}")
    public Mono<FinancialEventResponse> detail(@PathVariable UUID eventId) {
        return blocking(() -> service.getEventDetail(eventId));
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> query) {
        return Mono.fromCallable(query).subscribeOn(Schedulers.boundedElastic());
    }
}
