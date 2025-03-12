/* RedisConfig.java
 * Redis 연결설정 클래스
 * 해당 파일 설명
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       Redis 연결 설정 초안
 * 국경민      03-12       환경 변수 적용 및 Lettuce 설정 최적화
 * 국경민      03-12       Redis 직렬화 설정 강화 및 HashKey 지원 추가
 * 국경민      03-12       Deprecated 메서드 제거 및 Lettuce 설정 업데이트
 * 국경민      03-12       `LettuceConnectionFactory` 생성자 오류 해결 및 설정 개선
 * 국경민      03-12       `TimeoutOptions` 및 `clientConfig` 최적화
 * ========================================================
 */

package nadeuli.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * ✅ Redis 연결을 위한 ConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // ✅ Redis Standalone 설정 적용
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);

        // ✅ Redis 안정성 향상 옵션 적용
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofSeconds(5)) // ⏳ 연결 타임아웃 설정
                                .build())
                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5))) // ⏳ 전체 타임아웃 설정
                        .build())
                .commandTimeout(Duration.ofSeconds(5)) // ⏳ Redis 명령 실행 타임아웃 설정
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    /**
     * ✅ RedisTemplate 설정 (Key: String, Value: JSON 직렬화)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 🔹 Key 및 HashKey 직렬화 (String)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 🔹 Value 및 HashValue 직렬화 (JSON)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
