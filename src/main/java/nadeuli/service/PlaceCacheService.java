package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.repository.PlaceRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceCacheService {

    private final PlaceRepository placeRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_CACHE_SIZE = 30;
    private static final Duration CACHE_TTL = Duration.ofDays(5);

    public void saveToCache(String userId, String placeName) {
        String cacheKey = "user:" + userId + ":places";
        System.out.println("캐시 저장: " + cacheKey);

        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        zSetOperations.add(cacheKey, placeName, System.currentTimeMillis()); // ✅ place_name만 저장

        Long cacheSize = zSetOperations.size(cacheKey);
        if (cacheSize != null && cacheSize > MAX_CACHE_SIZE) {
            zSetOperations.removeRange(cacheKey, 0, cacheSize - MAX_CACHE_SIZE - 1);
        }

        redisTemplate.expire(cacheKey, CACHE_TTL);
        System.out.println("캐시 저장 완료 (장소 이름만 저장됨): " + cacheKey);
    }

    public List<String> getUserSearchHistory(String userId) {
        String cacheKey = "user:" + userId + ":places";
        System.out.println("사용자 검색 기록 조회: " + cacheKey);
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        Set<String> places = zSetOperations.reverseRange(cacheKey, 0, MAX_CACHE_SIZE - 1);

        return places != null ? places.stream().collect(Collectors.toList()) : List.of();
    }
}
