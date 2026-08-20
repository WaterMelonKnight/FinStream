package io.finstream.state;

import io.finstream.domain.MarketEvent; import io.finstream.domain.MarketState;
import java.math.BigDecimal; import java.time.Duration; import java.time.Instant; import java.util.ArrayDeque; import java.util.Deque; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRollingMarketStateStore implements MarketStateStore {
    private final Map<String, Deque<MarketEvent>> windows=new ConcurrentHashMap<>();
    @Override public MarketState update(MarketEvent event) {
        Deque<MarketEvent> ticks=windows.computeIfAbsent(event.symbol(), ignored->new ArrayDeque<>());
        synchronized(ticks) {
            ticks.addLast(event); Instant cutoff=event.eventTime().minus(Duration.ofMinutes(30));
            while(!ticks.isEmpty() && ticks.peekFirst().eventTime().isBefore(cutoff)) ticks.removeFirst();
            return snapshot(event, ticks);
        }
    }
    private MarketState snapshot(MarketEvent current, Deque<MarketEvent> ticks) {
        Instant now=current.eventTime(); BigDecimal high=current.price(), low=current.price(); double v1=0,v5=0,prior4=0;
        BigDecimal p1=null,p5=null,p30=ticks.peekFirst().price();
        for(MarketEvent tick:ticks) {
            Duration age=Duration.between(tick.eventTime(), now);
            if(age.compareTo(Duration.ofMinutes(30))<=0 && p30==null) p30=tick.price();
            if(age.compareTo(Duration.ofMinutes(5))<=0) { if(p5==null)p5=tick.price(); v5+=tick.quantity().doubleValue(); high=high.max(tick.price()); low=low.min(tick.price()); }
            if(age.compareTo(Duration.ofMinutes(1))<=0) { if(p1==null)p1=tick.price(); v1+=tick.quantity().doubleValue(); }
            else if(age.compareTo(Duration.ofMinutes(5))<=0) prior4+=tick.quantity().doubleValue();
        }
        double baseline=prior4/4.0; double ratio=baseline>0?v1/baseline:0;
        return new MarketState(current.symbol(),now,current.price(),pct(current.price(),p1),pct(current.price(),p5),pct(current.price(),p30),v1,v5,high,low,ratio);
    }
    private double pct(BigDecimal current, BigDecimal old){return old==null||old.signum()==0?0:current.subtract(old).divide(old,10,java.math.RoundingMode.HALF_UP).doubleValue()*100;}
}
