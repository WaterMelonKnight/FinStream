package io.finstream.anomaly;
import io.finstream.config.FinStreamProperties; import java.time.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap; import org.springframework.stereotype.Component;
@Component public class EventCooldown {
 private final Duration duration; private final Clock clock; private final Map<String,Instant> emitted=new ConcurrentHashMap<>();
 public EventCooldown(FinStreamProperties p){this(p.anomaly().cooldown(),Clock.systemUTC());} EventCooldown(Duration d,Clock c){duration=d;clock=c;}
 public boolean acquire(String symbol,String type){Instant now=clock.instant(); boolean[] accepted={false}; emitted.compute(symbol+":"+type,(k,last)->{if(last==null||!last.plus(duration).isAfter(now)){accepted[0]=true;return now;}return last;}); return accepted[0];}
}
