package io.finstream.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.finstream.domain.FinancialEvent;
import io.finstream.persistence.FinancialEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class FinancialEventQueryServiceTest {
    @Mock FinancialEventRepository repository;

    @Test
    void normalizesFiltersAndClampsLimit() {
        FinancialEvent event = event(UUID.randomUUID(), "BTCUSDT", "RAPID_DROP", 2);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));
        var service = new FinancialEventQueryService(repository);

        assertThat(service.getRecentEvents("btcusdt", "rapid_drop", 999)).hasSize(1);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    void fundingExtremeUsesTheExistingEventTypeQueryPath() {
        FinancialEvent event = event(UUID.randomUUID(), "BTCUSDT", "FUNDING_EXTREME", 1.2);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(new FinancialEventQueryService(repository)
                .getRecentEvents(null, "funding_extreme", 10))
                .extracting(response -> response.eventType()).containsExactly("FUNDING_EXTREME");
    }

    @Test
    void openInterestSurgeUsesTheExistingEventTypeQueryPath() {
        FinancialEvent event = event(UUID.randomUUID(), "BTCUSDT", "OPEN_INTEREST_SURGE", 1.2);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(event)));

        assertThat(new FinancialEventQueryService(repository)
                .getRecentEvents(null, "open_interest_surge", 10))
                .extracting(response -> response.eventType()).containsExactly("OPEN_INTEREST_SURGE");
    }

    @Test
    void validatesParametersAndProvidesDetailNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        var service = new FinancialEventQueryService(repository);
        assertThatThrownBy(() -> service.getRecentEvents(null, "unknown", 1))
                .isInstanceOf(QueryException.class).hasMessageContaining("Unsupported");
        assertThatThrownBy(() -> service.getAbnormalEvents(null, -1.0, null, 10))
                .isInstanceOf(QueryException.class).hasMessageContaining("non-negative");
        assertThatThrownBy(() -> service.getEventDetail(UUID.randomUUID()))
                .isInstanceOf(QueryException.class).hasMessageContaining("not found");
    }

    static FinancialEvent event(UUID id, String symbol, String type, double score) {
        return new FinancialEvent(id, "BINANCE", symbol, type, Instant.EPOCH, Instant.EPOCH,
                "HIGH", score, "summary", Map.of("return5m", -4), Map.of("price", 100));
    }
}
