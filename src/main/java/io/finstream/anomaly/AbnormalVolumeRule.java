package io.finstream.anomaly;
import io.finstream.config.FinStreamProperties; import io.finstream.domain.*; import java.util.*; import org.springframework.stereotype.Component;
@Component public class AbnormalVolumeRule implements AnomalyRule {
 private final FinStreamProperties.VolumeRule config; public AbnormalVolumeRule(FinStreamProperties p){config=p.anomaly().abnormalVolume();}
 @Override public String eventType(){return "ABNORMAL_VOLUME";}
 @Override public Optional<FinancialEvent> evaluate(MarketEvent e,MarketState s){double ratio=s.volumeRatio(); if(!config.enabled()||ratio<config.ratio())return Optional.empty(); return Optional.of(RuleEventFactory.create(e,eventType(),ratio/config.ratio(),e.symbol()+" volume is "+String.format(Locale.ROOT,"%.2fx",ratio)+" baseline",Map.of("volume1m",s.volume1m(),"volumeRatio",ratio)));}
}
