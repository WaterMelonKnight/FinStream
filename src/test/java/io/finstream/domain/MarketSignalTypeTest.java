package io.finstream.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarketSignalTypeTest {
    @Test
    void describesCanonicalInputsRatherThanAnomalyOutputs() {
        assertThat(MarketSignalType.values())
                .containsExactly(
                        MarketSignalType.TRADE,
                        MarketSignalType.FUNDING_RATE,
                        MarketSignalType.OPEN_INTEREST,
                        MarketSignalType.LIQUIDATION);
        assertThat(MarketSignalType.values())
                .extracting(Enum::name)
                .doesNotContain("RAPID_DROP", "RAPID_PUMP", "ABNORMAL_VOLUME");
    }
}
