package kr.momenta.gateway.config;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class KisRateLimitConfig {

    @Bean
    public RRateLimiter kisGlobalRateLimiter(RedissonClient redissonClient) {
        RRateLimiter limiter = redissonClient.getRateLimiter("rl:kis:global");
        // 앱 전체 합산 20회/초
        limiter.trySetRate(RateType.OVERALL, 19, Duration.ofSeconds(1));
        return limiter;
    }

    @Bean
    public RRateLimiter kisStockRateLimiter(RedissonClient redissonClient) {
        RRateLimiter limiter = redissonClient.getRateLimiter("rl:kis:stock");
        // 앱 전체 합산 20회/초
        limiter.trySetRate(RateType.OVERALL, 1, Duration.ofSeconds(1));
        return limiter;
    }
}
