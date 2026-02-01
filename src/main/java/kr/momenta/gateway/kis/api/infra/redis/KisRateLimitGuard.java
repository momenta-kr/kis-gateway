package kr.momenta.gateway.kis.api.infra.redis;

import kr.momenta.gateway.kis.exception.KisRateLimitedException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class KisRateLimitGuard {

    @Qualifier("kisGlobalRateLimiter")
    private final RRateLimiter kisGlobalRateLimiter;

    @Qualifier("kisStockRateLimiter")
    private final RRateLimiter kisStockRateLimiter;

    public Mono<Void> acquire(Duration maxWait) {
        return Mono.fromCallable(() ->
                kisGlobalRateLimiter.tryAcquire(1, maxWait))
                .subscribeOn(Schedulers.boundedElastic()) // WebFlux 이벤트루프 보호
                .flatMap(ok -> ok ? Mono.empty() : Mono.error(new KisRateLimitedException()));
    }

    public Mono<Void> acquireStock(Duration maxWait) {
        return Mono.fromCallable(() ->
                        kisStockRateLimiter.tryAcquire(1, maxWait))
                .subscribeOn(Schedulers.boundedElastic()) // WebFlux 이벤트루프 보호
                .flatMap(ok -> ok ? Mono.empty() : Mono.error(new KisRateLimitedException()));
    }
}
