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

    @Transactional
    public String fetchPlaceDetails(String googlePlaceId, String placeName) {
        String cacheKey = CACHE_PREFIX + googlePlaceId;

        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + googlePlaceId +
                "&key=" + googleMapsApiKey;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Google API 오류: " + response.getStatusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String address = rootNode.path("result").path("formatted_address").asText();
            double latitude = rootNode.path("result").path("geometry").path("location").path("lat").asDouble();
            double longitude = rootNode.path("result").path("geometry").path("location").path("lng").asDouble();

            saveOrUpdatePlace(googlePlaceId, placeName, address, latitude, longitude);

            redisTemplate.opsForValue().set(cacheKey, response.getBody(), CACHE_TTL);
            return response.getBody();
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
                .orElseGet(() -> placeRepository.save(Place.of(googlePlaceId, placeName, address, latitude, longitude)));
    }


    public String getAutocompleteResults(String input) {
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                "?input=" + input +
                "&key=" + googleMapsApiKey +
                "&components=country:KR&language=ko";

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Google Places Autocomplete API 호출 중 오류 발생", e);
        }
    }
}
