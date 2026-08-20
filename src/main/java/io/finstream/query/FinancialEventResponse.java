package io.finstream.query;

import io.finstream.domain.FinancialEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FinancialEventResponse(
        UUID id,
        String source,
        String symbol,
        String eventType,
        Instant eventTime,
        Instant detectedAt,
        String severity,
        double anomalyScore,
        String summary,
        Map<String, Object> metrics,
        Map<String, Object> evidence) {
    public FinancialEventResponse {
        metrics = Map.copyOf(metrics);
        evidence = Map.copyOf(evidence);
    }

    public static FinancialEventResponse from(FinancialEvent event) {
        return new FinancialEventResponse(event.getId(), event.getSource(), event.getSymbol(),
                event.getEventType(), event.getEventTime(), event.getDetectedAt(),
                event.getSeverity(), event.getAnomalyScore(), event.getSummary(),
                event.getMetrics(), event.getEvidence());
    }
}
