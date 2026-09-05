package io.finstream.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.finstream.domain.FundingRateState;
import io.finstream.domain.MarketState;
import io.finstream.state.FundingRateStateStore;
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
    @Mock FundingRateStateStore fundingRateStore;

    @Test
    void normalizesSymbolAndMapsSnapshot() {
        MarketState state = new MarketState("BTCUSDT", Instant.EPOCH, BigDecimal.TEN,
                1, 2, 3, 4, 5, BigDecimal.TEN, BigDecimal.ONE, 2, true);
        when(store.get("BTCUSDT")).thenReturn(Optional.of(state));

        assertThat(new MarketQueryService(store, fundingRateStore).getMarketState(" btcusdt ").symbol())
                .isEqualTo("BTCUSDT");
    }

    @Test
    void givesExplicitNotFoundAndInvalidSymbolErrors() {
        when(store.get("ETHUSDT")).thenReturn(Optional.empty());
        MarketQueryService service = new MarketQueryService(store, fundingRateStore);
        assertThatThrownBy(() -> service.getMarketState("ETHUSDT"))
                .isInstanceOf(QueryException.class).hasMessageContaining("No current");
        assertThatThrownBy(() -> service.getMarketState("!"))
                .isInstanceOf(QueryException.class).hasMessageContaining("2-20");
    }

    @Test
    void getsFundingRateStateAndNormalizesLowercaseSymbol() {
        FundingRateState state = new FundingRateState(
                "BTCUSDT", "BINANCE", new BigDecimal("0.001"), BigDecimal.TEN,
                BigDecimal.ONE, Instant.EPOCH.plusSeconds(3600), Instant.EPOCH, Instant.EPOCH);
        when(fundingRateStore.get("BTCUSDT")).thenReturn(Optional.of(state));

        FundingRateStateResponse response = new MarketQueryService(store, fundingRateStore)
                .getFundingRateState("btcusdt");

        assertThat(response.symbol()).isEqualTo("BTCUSDT");
        assertThat(response.fundingRatePercent()).isEqualByComparingTo("0.1");
    }

    @Test
    void givesExplicitFundingRateNotFoundError() {
        when(fundingRateStore.get("ETHUSDT")).thenReturn(Optional.empty());
        MarketQueryService service = new MarketQueryService(store, fundingRateStore);

        assertThatThrownBy(() -> service.getFundingRateState("ethusdt"))
                .isInstanceOfSatisfying(QueryException.class, error -> {
                    assertThat(error.code()).isEqualTo("FUNDING_RATE_STATE_NOT_FOUND");
                    assertThat(error.notFound()).isTrue();
                    assertThat(error).hasMessage("No current funding rate state for ETHUSDT");
                });
    }
}
