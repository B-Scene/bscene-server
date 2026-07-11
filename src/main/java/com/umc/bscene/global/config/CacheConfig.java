package com.umc.bscene.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheConfig {

    // 현재 라이브 중인 전체 밴드 목록 캐시. 라이브 상태 변동이 잦으므로 TTL을 짧게 유지
    public static final String LIVE_NOW_ALL = "liveNowAll";

    // TODO: 다시보기 전체 탭 캐시. 다시보기 기능 구현 시 사용
    public static final String REPLAY_ALL = "replayAll";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 값은 타입 정보를 포함한 JSON으로 직렬화하여 서버 재시작/다중 인스턴스 환경에서도 공유 가능
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(15))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .build();
    }
}
