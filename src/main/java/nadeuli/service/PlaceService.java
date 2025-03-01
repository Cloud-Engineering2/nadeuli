package nadeuli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import nadeuli.entity.Place;
import nadeuli.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    private static final String CACHE_PREFIX = "place_details:";
    private static final Duration CACHE_TTL = Duration.ofDays(5); // 캐시 유지 기간 5일


    @Transactional
    public String fetchPlaceDetails(String googlePlaceId, String placeName) {
        String cacheKey = CACHE_PREFIX + googlePlaceId;

        // 1️⃣ DB에서 장소 저장/업데이트 (캐시 여부와 관계없이 실행)
        Place place = saveOrUpdatePlace(googlePlaceId, placeName);

        // 2️⃣ Redis에서 캐시 확인
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            return addNadeuliPlaceIdToJson(cachedData, place.getId()); // JSON 변환 후 반환
        }

        // 3️⃣ Google Places API 요청 (캐시에 없을 때만 실행)
        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + googlePlaceId +
                "&key=" + googleMapsApiKey;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String responseBody = response.getBody();

        // ✅ responseBody가 null인지 체크 (API 응답이 없을 경우 예외 처리)
        if (responseBody == null || responseBody.isEmpty()) {
            throw new RuntimeException("Google Places API 응답이 비어 있습니다.");
        }

        // 4️⃣ JSON 데이터에 DB의 placeId(nadeuli_placeid) 추가
        String modifiedResponse = addNadeuliPlaceIdToJson(responseBody, place.getId());

        // 5️⃣ Redis에 저장 (TTL 적용)
        redisTemplate.opsForValue().set(cacheKey, modifiedResponse, CACHE_TTL);

        return modifiedResponse;
    }


    @Transactional
    public Place saveOrUpdatePlace(String googlePlaceId, String placeName) {
        return placeRepository.findByGooglePlaceId(googlePlaceId)
                .map(existingPlace -> {
                    existingPlace.incrementSearchCount();
                    return existingPlace;
                })
                .orElseGet(() -> placeRepository.save(Place.of(googlePlaceId, placeName)));
    }

    private String addNadeuliPlaceIdToJson(String jsonResponse, Long nadeuliPlaceId) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            ((ObjectNode) rootNode).put("nadeuli_placeid", nadeuliPlaceId); // 우리 DB의 placeId 추가
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 중 오류 발생", e);
        }
    }

    public String getAutocompleteResults(String input) {
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                "?input=" + input +
                "&key=" + googleMapsApiKey +
                "&components=country:KR&language=ko";

        return restTemplate.getForObject(url, String.class);
    }

}
