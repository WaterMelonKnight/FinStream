package io.finstream.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.finstream.domain.FinancialEvent;
import io.finstream.query.FinancialEventQueryService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class FinancialEventRepositoryTest {
    @Autowired FinancialEventRepository repository;

    @Test
    void persistsJsonMetrics() {
        UUID id = UUID.randomUUID();
        repository.saveAndFlush(new FinancialEvent(
                id,
                "BINANCE",
                "BTCUSDT",
                "RAPID_DROP",
                Instant.EPOCH,
                Instant.EPOCH,
                "HIGH",
                2.1,
                "drop",
                Map.of("return5m", -5.8),
                Map.of("price", 100)));

        FinancialEvent saved = repository.findById(id).orElseThrow();

        assertThat(saved.getMetrics()).containsEntry("return5m", -5.8);
        assertThat(saved.getEventType()).isEqualTo("RAPID_DROP");
    }

    @Test
    void queryServiceAppliesCombinedAndAbnormalFiltersInRecentOrder() {
        repository.save(event("BTCUSDT", "RAPID_DROP", 2.0, Instant.parse("2026-08-20T00:20:00Z")));
        repository.save(event("BTCUSDT", "RAPID_PUMP", 1.2, Instant.parse("2026-08-20T00:10:00Z")));
        repository.save(event("ETHUSDT", "RAPID_DROP", 3.0, Instant.parse("2026-08-20T00:30:00Z")));
        repository.flush();
        FinancialEventQueryService service = new FinancialEventQueryService(repository);

        assertThat(service.getRecentEvents("btcusdt", "rapid_drop", 10))
                .extracting(response -> response.eventType()).containsExactly("RAPID_DROP");
        assertThat(service.getAbnormalEvents(
                        Instant.parse("2026-08-20T00:15:00Z"), 1.5, null, 10))
                .extracting(response -> response.symbol()).containsExactly("ETHUSDT", "BTCUSDT");
    }

    private FinancialEvent event(String symbol, String type, double score, Instant detectedAt) {
        return new FinancialEvent(UUID.randomUUID(), "BINANCE", symbol, type, detectedAt,
                detectedAt, "HIGH", score, "event", Map.of(), Map.of());
    }
}
