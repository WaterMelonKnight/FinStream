package io.finstream.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.finstream.domain.MarketState;
import io.finstream.state.MarketStateStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketQueryServiceTest {
    @Mock MarketStateStore store;

    @Test
    void normalizesSymbolAndMapsSnapshot() {
        MarketState state = new MarketState("BTCUSDT", Instant.EPOCH, BigDecimal.TEN,
                1, 2, 3, 4, 5, BigDecimal.TEN, BigDecimal.ONE, 2, true);
        when(store.get("BTCUSDT")).thenReturn(Optional.of(state));

        assertThat(new MarketQueryService(store).getMarketState(" btcusdt ").symbol())
                .isEqualTo("BTCUSDT");
    }

    @Test
    void givesExplicitNotFoundAndInvalidSymbolErrors() {
        when(store.get("ETHUSDT")).thenReturn(Optional.empty());
        MarketQueryService service = new MarketQueryService(store);
        assertThatThrownBy(() -> service.getMarketState("ETHUSDT"))
                .isInstanceOf(QueryException.class).hasMessageContaining("No current");
        assertThatThrownBy(() -> service.getMarketState("!"))
                .isInstanceOf(QueryException.class).hasMessageContaining("2-20");
    }
}
