package kr.momenta.gateway.config;

import kr.momenta.gateway.kis.api.infra.redis.KisRateLimitGuard;
import kr.momenta.gateway.kis.property.KisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@RequiredArgsConstructor
@Configuration
public class KisWebClientConfig {

    private final KisProperties kisProperties;

    @Bean
    public WebClient kisWebClient(KisRateLimitGuard guard) {
        return WebClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .filter((req, next) ->
                        guard.acquire(Duration.ofMillis(200))
                                .then(next.exchange(req)))
                .build();
    }

    @Bean
    public WebClient kisBatchWebClient(KisRateLimitGuard guard) {
        return WebClient.builder()
                .baseUrl(kisProperties.getBaseUrl())
                .filter((req, next) ->
                        guard.acquire(Duration.ofSeconds(2))
                                .then(next.exchange(req)))
                .build();
    }
}
