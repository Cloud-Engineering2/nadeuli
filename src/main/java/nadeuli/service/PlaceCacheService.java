package nadeuli.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlaceCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveToCache(String userId, String placeName, double latitude, double longitude, double radius) {
        String key = "user:" + userId + ":places";
        long timestamp = Instant.now().toEpochMilli();

        PlaceCacheEntry entry = new PlaceCacheEntry(placeName, latitude, longitude, radius);
        try {
            String json = objectMapper.writeValueAsString(entry);
            redisTemplate.opsForZSet().add(key, json, timestamp);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getUserSearchHistory(String userId) {
        String key = "user:" + userId + ":places";
        return redisTemplate.opsForZSet().reverseRange(key, 0, -1);
    }

    private static class PlaceCacheEntry {
        public String name;
        public double lat;
        public double lng;
        public double radius;

        public PlaceCacheEntry(String name, double lat, double lng, double radius) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
            this.radius = radius;
        }
    }
}
