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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    @Test
    void aggregatesAvailableStatesWithFreshnessAndProvenance() {
        Instant now = Instant.parse("2026-09-13T12:00:00Z");
        when(store.get("BTCUSDT")).thenReturn(Optional.of(new MarketState(
                "BTCUSDT", now.minusSeconds(100), BigDecimal.TEN, 1, 2, 3, 4, 5,
                BigDecimal.TEN, BigDecimal.ONE, 2, true, now.minusSeconds(2))));
        when(fundingRateStore.get("BTCUSDT")).thenReturn(Optional.of(new FundingRateState(
                "BTCUSDT", "BINANCE", new BigDecimal("0.001"), BigDecimal.TEN, BigDecimal.ONE,
                now.plusSeconds(3600), now.minusSeconds(41), now.minusSeconds(40))));
        when(openInterestStore.get("BTCUSDT")).thenReturn(Optional.of(new OpenInterestState(
                "BINANCE", "BTCUSDT", new BigDecimal("123"), null, null, null, null, null,
                now.minusSeconds(16), now.minusSeconds(15))));

        var response = new MarketQueryService(store, fundingRateStore, openInterestStore,
                Clock.fixed(now, ZoneOffset.UTC)).getMarketContext(" btcusdt ");

        assertThat(response.symbol()).isEqualTo("BTCUSDT");
        assertThat(response.generatedAt()).isEqualTo(now);
        assertThat(response.market().available()).isTrue();
        assertThat(response.market().marketType()).isEqualTo("SPOT");
        assertThat(response.market().ageSeconds()).isEqualTo(2);
        assertThat(response.funding().available()).isTrue();
        assertThat(response.funding().marketType()).isEqualTo("USD_M_FUTURES");
        assertThat(response.funding().ageSeconds()).isEqualTo(40);
        assertThat(response.openInterest().available()).isTrue();
        assertThat(response.openInterest().ageSeconds()).isEqualTo(15);
    }

    @Test
    void returnsPartialContextWhenOneStateIsMissing() {
        Instant now = Instant.parse("2026-09-13T12:00:00Z");
        when(store.get("BTCUSDT")).thenReturn(Optional.of(new MarketState(
                "BTCUSDT", now, BigDecimal.TEN, 0, 0, 0, 1, 1, BigDecimal.TEN,
                BigDecimal.TEN, 1, true, now)));
        when(fundingRateStore.get("BTCUSDT")).thenReturn(Optional.empty());
        when(openInterestStore.get("BTCUSDT")).thenReturn(Optional.empty());

        var response = new MarketQueryService(store, fundingRateStore, openInterestStore,
                Clock.fixed(now, ZoneOffset.UTC)).getMarketContext("BTCUSDT");

        assertThat(response.market().available()).isTrue();
        assertThat(response.funding().available()).isFalse();
        assertThat(response.funding().marketType()).isEqualTo("USD_M_FUTURES");
        assertThat(response.funding().data()).isNull();
        assertThat(response.openInterest().available()).isFalse();
    }

    @Test
    void rejectsContextOnlyWhenAllStatesAreMissing() {
        when(store.get("BTCUSDT")).thenReturn(Optional.empty());
        when(fundingRateStore.get("BTCUSDT")).thenReturn(Optional.empty());
        when(openInterestStore.get("BTCUSDT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new MarketQueryService(store, fundingRateStore, openInterestStore,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)).getMarketContext("btcusdt"))
                .isInstanceOfSatisfying(QueryException.class, error -> {
                    assertThat(error.code()).isEqualTo("MARKET_CONTEXT_NOT_FOUND");
                    assertThat(error.notFound()).isTrue();
                });
    }

    @Test
    void clampsAgeWhenReceivedAtIsAfterGeneratedAt() {
        Instant now = Instant.parse("2026-09-13T12:00:00Z");
        when(store.get("BTCUSDT")).thenReturn(Optional.of(new MarketState(
                "BTCUSDT", now, BigDecimal.TEN, 0, 0, 0, 1, 1, BigDecimal.TEN,
                BigDecimal.TEN, 1, true, now.plusSeconds(1))));
        when(fundingRateStore.get("BTCUSDT")).thenReturn(Optional.empty());
        when(openInterestStore.get("BTCUSDT")).thenReturn(Optional.empty());

        var response = new MarketQueryService(store, fundingRateStore, openInterestStore,
                Clock.fixed(now, ZoneOffset.UTC)).getMarketContext("BTCUSDT");

        assertThat(response.market().ageSeconds()).isZero();
    }

}
