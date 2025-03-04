package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.entity.Place;
import nadeuli.repository.PlaceRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceCacheService {

    private final PlaceRepository placeRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_CACHE_SIZE = 30;
    private static final Duration CACHE_TTL = Duration.ofDays(5);

    public Place getPlace(String userId, String googlePlaceId, String placeName, String address, double lat, double lng) {
        String cacheKey = "user:" + userId + ":places";
        System.out.println("캐시 조회: " + cacheKey);

        Optional<Place> placeOpt = placeRepository.findByGooglePlaceId(googlePlaceId);
        if (placeOpt.isPresent()) {
            Place place = placeOpt.get();
            place.incrementSearchCount();
            placeRepository.save(place); // 검색 횟수 증가 후 저장
            saveToCache(cacheKey, place.getPlaceName()); // ✅ 장소 이름만 저장
            System.out.println("DB에서 검색한 장소 업데이트: " + place);
            return place;
        } else {
            Place newPlace = Place.of(googlePlaceId, placeName, address, lat, lng);
            placeRepository.save(newPlace);
            saveToCache(cacheKey, newPlace.getPlaceName()); // ✅ 장소 이름만 저장
            System.out.println("새로운 장소 저장: " + newPlace);
            return newPlace;
        }
    }

    private void saveToCache(String cacheKey, String placeName) {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        zSetOperations.add(cacheKey, placeName, System.currentTimeMillis()); // ✅ 장소 이름만 저장

        // 30개 초과 시 오래된 데이터 삭제
        Long cacheSize = zSetOperations.size(cacheKey);
        if (cacheSize != null && cacheSize > MAX_CACHE_SIZE) {
            zSetOperations.removeRange(cacheKey, 0, cacheSize - MAX_CACHE_SIZE - 1);
        }

        redisTemplate.expire(cacheKey, CACHE_TTL);
        System.out.println("캐시 저장 완료 (장소 이름만 저장됨): " + cacheKey);
    }

    /**
     * 사용자의 검색 기록을 가져옴 (장소 이름만 반환)
     */
    public List<String> getUserSearchHistory(String userId) {
        String cacheKey = "user:" + userId + ":places";
        System.out.println("사용자 검색 기록 조회: " + cacheKey);
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        Set<String> places = zSetOperations.reverseRange(cacheKey, 0, MAX_CACHE_SIZE - 1); // ✅ 장소 이름만 가져오기

        return places != null ? places.stream().collect(Collectors.toList()) : List.of();
    }
}
