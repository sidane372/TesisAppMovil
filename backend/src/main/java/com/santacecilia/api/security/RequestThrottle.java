package com.santacecilia.api.security;
import com.santacecilia.api.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;
/** Limite local acotado; en despliegues multiples se complementa en el gateway. */
@Component
public class RequestThrottle {
    private record Window(Instant inicio,int count){}
    private final Map<String,Window> windows=new HashMap<>();
    private final Clock clock;
    public RequestThrottle(Clock clock){this.clock=clock;}
    public synchronized void check(String key,int max,Duration duration){
        Instant now=clock.instant();
        if(windows.size()>=10000)windows.entrySet().removeIf(e->e.getValue().inicio().plus(Duration.ofHours(1)).isBefore(now));
        Window previous=windows.get(key);
        if(previous==null||!previous.inicio().plus(duration).isAfter(now)){
            if(windows.size()>=10000&&!windows.containsKey(key))throw limit();
            windows.put(key,new Window(now,1));return;
        }
        if(previous.count()>=max)throw limit();
        windows.put(key,new Window(previous.inicio(),previous.count()+1));
    }
    private ApiException limit(){return new ApiException(HttpStatus.TOO_MANY_REQUESTS,"DEMASIADOS_INTENTOS","Espera antes de intentarlo nuevamente");}
}
