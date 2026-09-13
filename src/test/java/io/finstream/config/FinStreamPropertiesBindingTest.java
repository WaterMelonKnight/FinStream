package io.finstream.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;

class FinStreamPropertiesBindingTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsEnabledBinanceNestedConfigurationAndNonDefaultValues() {
        contextRunner
                .withPropertyValues(
                        "finstream.market.binance.enabled=true",
                        "finstream.market.binance.base-url=wss://example.test/stream",
                        "finstream.market.binance.funding-rate.enabled=true",
                        "finstream.market.binance.funding-rate.base-url=https://funding.example.test",
                        "finstream.market.binance.funding-rate.poll-interval=23s",
                        "finstream.market.binance.open-interest.enabled=true",
                        "finstream.market.binance.open-interest.base-url=https://interest.example.test",
                        "finstream.market.binance.open-interest.poll-interval=17s")
                .run(context -> {
                    FinStreamProperties.Binance binance = context.getBean(FinStreamProperties.class)
                            .market().binance();

                    assertThat(binance.enabled()).isTrue();
                    assertThat(binance.baseUrl()).isEqualTo("wss://example.test/stream");
                    assertThat(binance.fundingRate().enabled()).isTrue();
                    assertThat(binance.fundingRate().baseUrl()).isEqualTo("https://funding.example.test");
                    assertThat(binance.fundingRate().pollInterval()).isEqualTo(Duration.ofSeconds(23));
                    assertThat(binance.openInterest().enabled()).isTrue();
                    assertThat(binance.openInterest().baseUrl()).isEqualTo("https://interest.example.test");
                    assertThat(binance.openInterest().pollInterval()).isEqualTo(Duration.ofSeconds(17));
                });
    }

    @Test
    void bindsExplicitFalseValuesWithoutDefaultInterference() {
        contextRunner
                .withPropertyValues(
                        "finstream.market.binance.enabled=false",
                        "finstream.market.binance.funding-rate.enabled=false",
                        "finstream.market.binance.open-interest.enabled=false")
                .run(context -> {
                    FinStreamProperties.Binance binance = context.getBean(FinStreamProperties.class)
                            .market().binance();

                    assertThat(binance.enabled()).isFalse();
                    assertThat(binance.fundingRate().enabled()).isFalse();
                    assertThat(binance.openInterest().enabled()).isFalse();
                });
    }

    @Test
    void bindsAnomalyNestedConfigurationThroughItsCanonicalConstructor() {
        contextRunner
                .withPropertyValues(
                        "finstream.anomaly.cooldown=4m",
                        "finstream.anomaly.rapid-drop.enabled=false",
                        "finstream.anomaly.rapid-drop.threshold-percent=7.5",
                        "finstream.anomaly.funding-extreme.enabled=false",
                        "finstream.anomaly.funding-extreme.threshold=0.002")
                .run(context -> {
                    FinStreamProperties.Anomaly anomaly = context.getBean(FinStreamProperties.class)
                            .anomaly();

                    assertThat(anomaly.cooldown()).isEqualTo(Duration.ofMinutes(4));
                    assertThat(anomaly.rapidDrop().enabled()).isFalse();
                    assertThat(anomaly.rapidDrop().thresholdPercent()).isEqualTo(7.5);
                    assertThat(anomaly.fundingExtreme().enabled()).isFalse();
                    assertThat(anomaly.fundingExtreme().threshold()).isEqualByComparingTo("0.002");
                });
    }

    @Test
    void resolvesBinanceEnvironmentPlaceholdersFromApplicationYaml() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(
                        "spring.config.location=file:src/main/resources/application.yml",
                        "BINANCE_ENABLED=true",
                        "BINANCE_FUNDING_ENABLED=true",
                        "BINANCE_OPEN_INTEREST_ENABLED=true")
                .run(context -> {
                    FinStreamProperties.Binance binance = context.getBean(FinStreamProperties.class)
                            .market().binance();

                    assertThat(binance.enabled()).isTrue();
                    assertThat(binance.fundingRate().enabled()).isTrue();
                    assertThat(binance.openInterest().enabled()).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FinStreamProperties.class)
    static class PropertiesConfiguration {}
}
