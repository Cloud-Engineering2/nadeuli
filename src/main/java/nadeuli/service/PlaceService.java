package nadeuli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final Duration CACHE_TTL = Duration.ofDays(5);

    public String getAutocompleteResults(String input) {
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                "?input=" + input +
                "&rankby=prominence" +
                "&key=" + googleMapsApiKey +
                "&components=country:KR" +
                "&language=ko";

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Google Places Autocomplete API 호출 중 오류 발생", e);
        }
    }

    @Transactional
    public Map<String, Object> fetchPlaceDetails(String googlePlaceId, String placeName) {
        String cacheKey = CACHE_PREFIX + googlePlaceId;

        // Google Places API 요청 URL
        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + googlePlaceId +
                "&key=" + googleMapsApiKey +
                "&language=ko" +
                "&fields=name,geometry,formatted_address,rating,user_ratings_total,opening_hours,photos,editorial_summary";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Google API 오류: " + response.getStatusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode resultNode = rootNode.path("result");

            // JSON 데이터 추출
            String extractedPlaceName = resultNode.path("name").asText();
            String address = resultNode.path("formatted_address").asText();
            double latitude = resultNode.path("geometry").path("location").path("lat").asDouble();
            double longitude = resultNode.path("geometry").path("location").path("lng").asDouble();
            double rating = resultNode.path("rating").asDouble();
            int reviewCount = resultNode.path("user_ratings_total").asInt();
            String openingHours = resultNode.path("opening_hours").path("weekday_text").toString();
            String description = resultNode.path("editorial_summary").path("overview").asText();

            // 사진 URL 추출
            List<String> photos = new ArrayList<>();
            JsonNode photosNode = resultNode.path("photos");
            if (photosNode.isArray()) {
                for (JsonNode photo : photosNode) {
                    String photoReference = photo.path("photo_reference").asText();
                    String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400" +
                            "&photo_reference=" + photoReference +
                            "&key=" + googleMapsApiKey;
                    photos.add(photoUrl);
                }
            }

            // DB 저장
            saveOrUpdatePlace(googlePlaceId, extractedPlaceName, address, latitude, longitude);

            // Redis 캐싱 (장소 이름만 저장)
            redisTemplate.opsForValue().set(cacheKey, extractedPlaceName, CACHE_TTL);

            // 응답 데이터 구성
            Map<String, Object> placeDetails = new HashMap<>();
            placeDetails.put("place_name", extractedPlaceName);
            placeDetails.put("lat", latitude);
            placeDetails.put("lng", longitude);
            placeDetails.put("formatted_address", address);
            placeDetails.put("rating", rating);
            placeDetails.put("review_count", reviewCount);
            placeDetails.put("opening_hours", openingHours);
            placeDetails.put("photos", photos);
            placeDetails.put("description", description);

            return placeDetails;
        } catch (Exception e) {
            throw new RuntimeException("Google Places API 호출 중 오류 발생", e);
        }
    }


    @Transactional
    public Place saveOrUpdatePlace(String googlePlaceId, String placeName, String address, double latitude, double longitude) {
        return placeRepository.findByGooglePlaceId(googlePlaceId)
                .map(existingPlace -> {
                    existingPlace.incrementSearchCount();
                    return placeRepository.save(existingPlace);
                })
                .orElseGet(() -> placeRepository.save(new Place(googlePlaceId, placeName, address, latitude, longitude)));
    }
}
