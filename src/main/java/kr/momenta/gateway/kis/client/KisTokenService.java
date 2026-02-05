package kr.momenta.gateway.kis.client;

import com.esotericsoftware.kryo.util.ObjectMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.momenta.gateway.kis.api.res.AccessTokenResponse;
import kr.momenta.gateway.kis.infra.GlobalToken;
import kr.momenta.gateway.kis.property.KisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.DataInput;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class KisTokenService {

    private static final String TOKEN_KEY = "token:globalToken";
    private static final String LOCK_KEY  = "lock:token:globalToken";

    private static final String WEBSOCKET_TOKEN_KEY = "token:websocketToken";
    private static final String WEBSOCKET_LOCK_KEY  = "lock:token:websocketToken";

    // 토큰 TTL 여유(만료 N초 전 갱신)
    private static final long SAFETY_SECONDS = 60;

    // 락 TTL (토큰 발급 API + Redis 저장이 이 시간 내 끝나야 함)
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    // 락 못 잡은 인스턴스가 기다리는 시간/횟수
    private static final Duration WAIT_INTERVAL = Duration.ofMillis(200);
    private static final Duration WAIT_TIMEOUT  = Duration.ofSeconds(5);

    private final WebClient kisWebClient;
    private final KisProperties kisProperties;
    private final ObjectMapper objectMapper;

    // token 저장용 (GlobalToken 직렬화)
    private final ReactiveRedisTemplate<String, GlobalToken> tokenRedis;

    // lock 저장용 (String)
    private final ReactiveStringRedisTemplate lockRedis;

    // 인스턴스 내부 중복 호출 방지(in-flight)
    private final AtomicReference<Mono<String>> inFlight = new AtomicReference<>();
    private final AtomicReference<Mono<String>> websocketInFlight = new AtomicReference<>();

    public Mono<String> getAccessToken() {
        return getTokenFromRedis()
                .switchIfEmpty(Mono.defer(this::refreshWithDistributedLock));
    }

    private Mono<String> getTokenFromRedis() {
        return tokenRedis.<String, String>opsForValue()
                .get(TOKEN_KEY)
                .map(GlobalToken::getAccessToken);
    }

    private Mono<String> refreshWithDistributedLock() {
        // 같은 인스턴스 안에서 몰리면 한 번만 갱신하도록 in-flight 공유
        Mono<String> existing = inFlight.get();
        if (existing != null) return existing;

        Mono<String> created = Mono.defer(() -> {
                    String lockValue = UUID.randomUUID().toString();

                    return tryAcquireLock(lockValue)
                            .flatMap(acquired -> {
                                if (acquired) {
                                    // ✅ 내가 락 잡음 → 토큰 발급 후 저장 → 락 해제
                                    return requestAndCacheToken()
                                            .flatMap(token -> releaseLock(lockValue).thenReturn(token));
                                }
                                // ❌ 락 못 잡음 → 누군가 갱신 중 → 토큰이 채워질 때까지 기다림
                                return waitForTokenOrTimeout();
                            });
                })
                .doFinally(sig -> inFlight.set(null))
                .cache(); // 같은 Mono를 여러 구독자가 공유

        if (inFlight.compareAndSet(null, created)) {
            return created;
        }
        Mono<String> winner = inFlight.get();
        return winner != null ? winner : created;
    }

    private Mono<Boolean> tryAcquireLock(String lockValue) {
        // SETNX + TTL
        return lockRedis.opsForValue().setIfAbsent(LOCK_KEY, lockValue, LOCK_TTL)
                .map(Boolean.TRUE::equals)
                .onErrorReturn(false);
    }

    private Mono<String> waitForTokenOrTimeout() {
        // 200ms 간격으로 TOKEN_KEY를 확인 → 생기면 바로 반환
        return Flux.interval(WAIT_INTERVAL)
                .flatMap(tick -> getTokenFromRedis())
                .next() // 첫 토큰만
                .timeout(WAIT_TIMEOUT)
                // 기다렸는데도 안 생기면(락 TTL 만료/장애 등) 다시 한번 refresh 시도(재귀)
                .onErrorResume(TimeoutException.class, ex ->
                        getTokenFromRedis().switchIfEmpty(Mono.defer(this::refreshWithDistributedLock))
                );
    }

    private Mono<String> requestAndCacheToken() {
        return kisWebClient.post()
                .uri("/oauth2/tokenP") // ✅ 너 토큰 엔드포인트에 맞게
                .bodyValue(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", kisProperties.getAppKey(),
                        "appsecret", kisProperties.getAppSecret()
                ))
                .retrieve()
                .bodyToMono(AccessTokenResponse.class)
                .flatMap(resp -> {
                    long ttlSec = Math.max(SAFETY_SECONDS, resp.getExpiresIn() - SAFETY_SECONDS);
                    Duration ttl = Duration.ofSeconds(ttlSec);

                    GlobalToken gt = GlobalToken.from(resp); // accessToken 포함
                    return tokenRedis.opsForValue()
                            .set(TOKEN_KEY, gt, ttl)
                            .thenReturn(resp.getAccessToken());
                });
    }

    private Mono<Void> releaseLock(String lockValue) {
        // "내가 잡은 락"만 지우기 (GET==lockValue 일 때만 DEL)
        String lua =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "  return redis.call('del', KEYS[1]) " +
                        "else " +
                        "  return 0 " +
                        "end";

        RedisScript<Long> script = RedisScript.of(lua, Long.class);

        return lockRedis.execute(script, List.of(LOCK_KEY), lockValue)
                .next()
                .then();
    }

    // ========== WebSocket Token Methods ==========

    public Mono<String> getWebSocketToken() {
        return getWebSocketTokenFromRedis()
                .switchIfEmpty(Mono.defer(this::refreshWebSocketTokenWithDistributedLock));
    }

    private Mono<String> getWebSocketTokenFromRedis() {
        return tokenRedis.<String, String>opsForValue()
                .get(WEBSOCKET_TOKEN_KEY)
                .map(GlobalToken::getAccessToken);
    }

    private Mono<String> refreshWebSocketTokenWithDistributedLock() {
        // 같은 인스턴스 안에서 몰리면 한 번만 갱신하도록 in-flight 공유
        Mono<String> existing = websocketInFlight.get();
        if (existing != null) return existing;

        Mono<String> created = Mono.defer(() -> {
                    String lockValue = UUID.randomUUID().toString();

                    return tryAcquireWebSocketLock(lockValue)
                            .flatMap(acquired -> {
                                if (acquired) {
                                    // ✅ 내가 락 잡음 → 토큰 발급 후 저장 → 락 해제
                                    return requestAndCacheWebSocketToken()
                                            .flatMap(token -> releaseWebSocketLock(lockValue).thenReturn(token));
                                }
                                // ❌ 락 못 잡음 → 누군가 갱신 중 → 토큰이 채워질 때까지 기다림
                                return waitForWebSocketTokenOrTimeout();
                            });
                })
                .doFinally(sig -> websocketInFlight.set(null))
                .cache(); // 같은 Mono를 여러 구독자가 공유

        if (websocketInFlight.compareAndSet(null, created)) {
            return created;
        }
        Mono<String> winner = websocketInFlight.get();
        return winner != null ? winner : created;
    }

    private Mono<Boolean> tryAcquireWebSocketLock(String lockValue) {
        // SETNX + TTL
        return lockRedis.opsForValue().setIfAbsent(WEBSOCKET_LOCK_KEY, lockValue, LOCK_TTL)
                .map(Boolean.TRUE::equals)
                .onErrorReturn(false);
    }

    private Mono<String> waitForWebSocketTokenOrTimeout() {
        // 200ms 간격으로 WEBSOCKET_TOKEN_KEY를 확인 → 생기면 바로 반환
        return Flux.interval(WAIT_INTERVAL)
                .flatMap(tick -> getWebSocketTokenFromRedis())
                .next() // 첫 토큰만
                .timeout(WAIT_TIMEOUT)
                // 기다렸는데도 안 생기면(락 TTL 만료/장애 등) 다시 한번 refresh 시도(재귀)
                .onErrorResume(TimeoutException.class, ex ->
                        getWebSocketTokenFromRedis().switchIfEmpty(Mono.defer(this::refreshWebSocketTokenWithDistributedLock))
                );
    }

    private Mono<String> requestAndCacheWebSocketToken() {
        return kisWebClient.post()
                .uri("/oauth2/Approval")
                .bodyValue(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", kisProperties.getAppKey(),
                        "secretkey", kisProperties.getAppSecret()
                ))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(approvalKey -> {
                    long ttlSec = Math.max(SAFETY_SECONDS, 86_400 - SAFETY_SECONDS);
                    Duration ttl = Duration.ofSeconds(ttlSec);
                    return tokenRedis.opsForValue()
                            .set(WEBSOCKET_TOKEN_KEY, GlobalToken.ws(approvalKey), ttl)
                            .thenReturn(approvalKey);
                });
    }

    private Mono<Void> releaseWebSocketLock(String lockValue) {
        // "내가 잡은 락"만 지우기 (GET==lockValue 일 때만 DEL)
        String lua =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "  return redis.call('del', KEYS[1]) " +
                        "else " +
                        "  return 0 " +
                        "end";

        RedisScript<Long> script = RedisScript.of(lua, Long.class);

        return lockRedis.execute(script, List.of(WEBSOCKET_LOCK_KEY), lockValue)
                .next()
                .then();
    }
}
