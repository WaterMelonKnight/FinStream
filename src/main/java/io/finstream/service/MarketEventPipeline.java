package io.finstream.service;
import io.finstream.anomaly.*; import io.finstream.connector.MarketDataConnector; import io.finstream.domain.*; import io.finstream.persistence.FinancialEventRepository; import io.finstream.state.MarketStateStore; import jakarta.annotation.PostConstruct; import java.util.List; import org.slf4j.*; import org.springframework.stereotype.Service;
@Service public class MarketEventPipeline {
 private static final Logger log=LoggerFactory.getLogger(MarketEventPipeline.class); private final MarketDataConnector connector; private final MarketStateStore states; private final List<AnomalyRule> rules; private final EventCooldown cooldown; private final FinancialEventRepository repository;
 public MarketEventPipeline(MarketDataConnector c,MarketStateStore s,List<AnomalyRule> r,EventCooldown d,FinancialEventRepository repo){connector=c;states=s;rules=r;cooldown=d;repository=repo;}
 @PostConstruct void start(){connector.events().subscribe(this::process,e->log.error("Market pipeline failed",e));}
 void process(MarketEvent event){MarketState state=states.update(event); for(AnomalyRule rule:rules) rule.evaluate(event,state).filter(e->cooldown.acquire(e.getSymbol(),e.getEventType())).ifPresent(e->{log.info("Anomaly triggered: {} {} score={}",e.getSymbol(),e.getEventType(),e.getAnomalyScore()); repository.save(e); log.info("FinancialEvent saved: {}",e.getId());});}
}
