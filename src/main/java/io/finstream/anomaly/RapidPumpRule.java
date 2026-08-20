package io.finstream.anomaly;
import io.finstream.config.FinStreamProperties; import io.finstream.domain.*; import java.util.*; import org.springframework.stereotype.Component;
@Component public class RapidPumpRule implements AnomalyRule {
 private final FinStreamProperties.Rule config; public RapidPumpRule(FinStreamProperties p){config=p.anomaly().rapidPump();}
 @Override public String eventType(){return "RAPID_PUMP";}
 @Override public Optional<FinancialEvent> evaluate(MarketEvent e,MarketState s){double r=s.return5m(); if(!config.enabled()||r<config.thresholdPercent())return Optional.empty(); double score=r/config.thresholdPercent(); return Optional.of(RuleEventFactory.create(e,eventType(),score,e.symbol()+" pumped "+String.format(Locale.ROOT,"%.2f%%",r)+" in 5m",Map.of("return5m",r)));}
}
