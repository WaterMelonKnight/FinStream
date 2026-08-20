package io.finstream.anomaly;
import io.finstream.domain.*; import java.time.Clock; import java.util.Map; import java.util.UUID;
final class RuleEventFactory {
 private RuleEventFactory(){}
 static FinancialEvent create(MarketEvent e,String type,double score,String summary,Map<String,Object> metrics){return new FinancialEvent(UUID.randomUUID(),e.source(),e.symbol(),type,e.eventTime(),Clock.systemUTC().instant(),score>=2?"HIGH":"MEDIUM",score,summary,metrics,Map.of("lastPrice",e.price()));}
}
