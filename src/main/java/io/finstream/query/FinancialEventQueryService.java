package io.finstream.query;

import io.finstream.domain.FinancialEvent;
import io.finstream.persistence.FinancialEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class FinancialEventQueryService {
    private static final Sort RECENT_FIRST = Sort.by(Sort.Order.desc("detectedAt"), Sort.Order.desc("eventTime"));
    private final FinancialEventRepository repository;

    public FinancialEventQueryService(FinancialEventRepository repository) { this.repository = repository; }

    public List<FinancialEventResponse> getRecentEvents(String symbol, String eventType, Integer limit) {
        return find(QueryParameters.symbol(symbol, false), QueryParameters.eventType(eventType),
                null, null, QueryParameters.limit(limit));
    }

    public FinancialEventResponse getEventDetail(UUID eventId) {
        if (eventId == null) throw new QueryException("INVALID_EVENT_ID", "eventId is required", false);
        return repository.findById(eventId).map(FinancialEventResponse::from).orElseThrow(() ->
                new QueryException("EVENT_NOT_FOUND", "Financial event not found: " + eventId, true));
    }

    public List<FinancialEventResponse> getAbnormalEvents(
            Instant since, Double minScore, String symbol, Integer limit) {
        if (minScore != null && (!Double.isFinite(minScore) || minScore < 0)) {
            throw new QueryException("INVALID_MIN_SCORE", "minScore must be a finite non-negative number", false);
        }
        return find(QueryParameters.symbol(symbol, false), null, since,
                minScore == null ? 1.0 : minScore, QueryParameters.limit(limit));
    }

    private List<FinancialEventResponse> find(
            String symbol, String eventType, Instant since, Double minScore, int limit) {
        Specification<FinancialEvent> spec = Specification.where(null);
        if (symbol != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("symbol"), symbol));
        if (eventType != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        if (since != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("detectedAt"), since));
        if (minScore != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("anomalyScore"), minScore));
        return repository.findAll(spec, PageRequest.of(0, limit, RECENT_FIRST)).stream()
                .map(FinancialEventResponse::from).toList();
    }
}
