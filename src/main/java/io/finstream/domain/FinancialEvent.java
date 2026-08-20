package io.finstream.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "financial_event")
public class FinancialEvent {
    @Id private UUID id;

    private String source;
    private String symbol;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "detected_at")
    private Instant detectedAt;

    private String severity;

    @Column(name = "anomaly_score")
    private double anomalyScore;

    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metrics;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> evidence;

    protected FinancialEvent() {}

    public FinancialEvent(
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
        this.id = id;
        this.source = source;
        this.symbol = symbol;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.detectedAt = detectedAt;
        this.severity = severity;
        this.anomalyScore = anomalyScore;
        this.summary = summary;
        this.metrics = Map.copyOf(metrics);
        this.evidence = Map.copyOf(evidence);
    }

    public UUID getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public String getSeverity() {
        return severity;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public String getSummary() {
        return summary;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }
}
