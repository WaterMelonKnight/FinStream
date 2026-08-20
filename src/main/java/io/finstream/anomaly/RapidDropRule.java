package io.finstream.anomaly;
import io.finstream.config.FinStreamProperties; import io.finstream.domain.*; import java.util.*; import org.springframework.stereotype.Component;
@Component public class RapidDropRule implements AnomalyRule {
 private final FinStreamProperties.Rule config; public RapidDropRule(FinStreamProperties p){config=p.anomaly().rapidDrop();}
 @Override public String eventType(){return "RAPID_DROP";}
 @Override public Optional<FinancialEvent> evaluate(MarketEvent e,MarketState s){double r=s.return5m(); if(!config.enabled()||r>-config.thresholdPercent())return Optional.empty(); double score=Math.abs(r)/config.thresholdPercent(); return Optional.of(RuleEventFactory.create(e,eventType(),score,e.symbol()+" dropped "+String.format(Locale.ROOT,"%.2f%%",Math.abs(r))+" in 5m",Map.of("return5m",r)));}
}
