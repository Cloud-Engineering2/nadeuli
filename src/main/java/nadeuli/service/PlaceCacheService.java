package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.entity.Place;
import nadeuli.repository.PlaceRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int MAX_RANK_SIZE = 100;  // 실시간 검색 순위 최대 개수
    private static final Duration CACHE_TTL = Duration.ofDays(5);
    private static final String GLOBAL_SEARCH_RANK_KEY = "place_rankings"; // 글로벌 검색 순위 키

    @Transactional
    public Place getPlace(String googlePlaceId, String placeName, String address, double lat, double lng) {
        Optional<Place> placeOpt = placeRepository.findByGooglePlaceId(googlePlaceId);

        if (placeOpt.isPresent()) {
            Place place = placeOpt.get();
            place.incrementSearchCount();
            placeRepository.save(place);
            saveToGlobalCache(place.getPlaceName()); // 🔹 글로벌 캐싱
            return place;
        } else {
            Place newPlace = Place.of(googlePlaceId, placeName, address, lat, lng);
            placeRepository.save(newPlace);
            saveToGlobalCache(newPlace.getPlaceName()); // 🔹 글로벌 캐싱
            return newPlace;
        }
    }

    private void saveToGlobalCache(String placeName) {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        zSetOperations.incrementScore(GLOBAL_SEARCH_RANK_KEY, placeName, 1); // 검색 횟수 증가

        Long size = zSetOperations.zCard(GLOBAL_SEARCH_RANK_KEY);
        if (size != null && size > MAX_RANK_SIZE) {
            zSetOperations.removeRange(GLOBAL_SEARCH_RANK_KEY, 0, size - MAX_RANK_SIZE - 1);
        }

        redisTemplate.expire(GLOBAL_SEARCH_RANK_KEY, CACHE_TTL);
    }

    public List<String> getTopSearchRankings() {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        Set<String> topPlaces = zSetOperations.reverseRange(GLOBAL_SEARCH_RANK_KEY, 0, 9);

        return topPlaces != null ? List.copyOf(topPlaces) : List.of();
    }
}
