package io.finstream.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.finstream.domain.FundingRateState;
import io.finstream.domain.MarketState;
import io.finstream.state.FundingRateStateStore;
import io.finstream.state.MarketStateStore;
import io.finstream.state.OpenInterestStateStore;
import io.finstream.domain.OpenInterestState;
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
    @Mock OpenInterestStateStore openInterestStore;

    @Test
    void normalizesSymbolAndMapsSnapshot() {
        MarketState state = new MarketState("BTCUSDT", Instant.EPOCH, BigDecimal.TEN,
                1, 2, 3, 4, 5, BigDecimal.TEN, BigDecimal.ONE, 2, true);
        when(store.get("BTCUSDT")).thenReturn(Optional.of(state));

        assertThat(new MarketQueryService(store, fundingRateStore, openInterestStore).getMarketState(" btcusdt ").symbol())
                .isEqualTo("BTCUSDT");
    }

    @Test
    void givesExplicitNotFoundAndInvalidSymbolErrors() {
        when(store.get("ETHUSDT")).thenReturn(Optional.empty());
        MarketQueryService service = new MarketQueryService(store, fundingRateStore, openInterestStore);
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

        FundingRateStateResponse response = new MarketQueryService(store, fundingRateStore, openInterestStore)
                .getFundingRateState("btcusdt");

        assertThat(response.symbol()).isEqualTo("BTCUSDT");
        assertThat(response.fundingRatePercent()).isEqualByComparingTo("0.1");
    }

    @Test
    void givesExplicitFundingRateNotFoundError() {
        when(fundingRateStore.get("ETHUSDT")).thenReturn(Optional.empty());
        MarketQueryService service = new MarketQueryService(store, fundingRateStore, openInterestStore);

        assertThatThrownBy(() -> service.getFundingRateState("ethusdt"))
                .isInstanceOfSatisfying(QueryException.class, error -> {
                    assertThat(error.code()).isEqualTo("FUNDING_RATE_STATE_NOT_FOUND");
                    assertThat(error.notFound()).isTrue();
                    assertThat(error).hasMessage("No current funding rate state for ETHUSDT");
                });
    }
    @Test
    void getsOpenInterestStateAndNormalizesMixedCaseSymbol() {
        OpenInterestState state = new OpenInterestState(
                "BINANCE", "BTCUSDT", new BigDecimal("12345.67890123456789"),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
        when(openInterestStore.get("BTCUSDT")).thenReturn(Optional.of(state));

        OpenInterestStateResponse response = new MarketQueryService(
                store, fundingRateStore, openInterestStore).getOpenInterestState(" btcUsdt ");

        assertThat(response.symbol()).isEqualTo("BTCUSDT");
        assertThat(response.openInterest()).isEqualByComparingTo("12345.67890123456789");
    }

    @Test
    void givesExplicitOpenInterestNotFoundError() {
        when(openInterestStore.get("ETHUSDT")).thenReturn(Optional.empty());
        MarketQueryService service = new MarketQueryService(
                store, fundingRateStore, openInterestStore);

        assertThatThrownBy(() -> service.getOpenInterestState("ethusdt"))
                .isInstanceOfSatisfying(QueryException.class, error -> {
                    assertThat(error.code()).isEqualTo("OPEN_INTEREST_STATE_NOT_FOUND");
                    assertThat(error.notFound()).isTrue();
                    assertThat(error).hasMessage("No current open interest state for ETHUSDT");
                });
    }

}
