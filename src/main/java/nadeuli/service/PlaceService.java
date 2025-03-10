/* PlaceService.java
 * Place 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */
package nadeuli.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import nadeuli.dto.PlaceDTO;
import nadeuli.entity.constant.PlaceCategory;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nadeuli.entity.Place;
import nadeuli.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final S3Service s3Service;

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    private static final String CACHE_PREFIX = "place_details:";
    private static final Duration CACHE_TTL = Duration.ofDays(5);





    @Transactional
    public CompletableFuture<ResponseEntity<Map<String, Object>>> addNewPlace(String placeId) {
        return CompletableFuture.supplyAsync(() -> {
            // 1️⃣ 이미 존재하는 장소인지 확인
            Optional<Place> existingPlace = placeRepository.findByGooglePlaceId(placeId);
            if (existingPlace.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", 200);
                response.put("message", "이미 존재하는 장소입니다.");
                response.put("place", PlaceDTO.from(existingPlace.get()));
                return CompletableFuture.completedFuture(ResponseEntity.ok(response));
            }

            // 2️⃣ Google Place Detail API 호출
            String placeDetailsJson = getPlaceDetails(placeId);
            JsonNode placeDetails = parseJson(placeDetailsJson);

            if (placeDetails.isMissingNode()) {
                throw new IllegalStateException("Google API에서 장소 정보를 가져오지 못했습니다.");
            }

            String placeName = placeDetails.path("displayName").path("text").asText();
            String address = placeDetails.path("formattedAddress").asText();
            double latitude = placeDetails.path("location").path("latitude").asDouble();
            double longitude = placeDetails.path("location").path("longitude").asDouble();
            double rating = placeDetails.path("rating").asDouble(0.0);
            int ratingCount = placeDetails.path("userRatingCount").asInt(0);
            String googleUrl = "https://www.google.com/maps/place/?q=place_id:" + placeId;
            String description = "Google Place API에서 제공하는 기본 데이터"; // 필요 시 추가 가공

            // 3️⃣ 장소 유형 (PlaceType) 분류
            List<String> types = new ArrayList<>();
            JsonNode typesNode = placeDetails.get("types");
            if (typesNode != null && typesNode.isArray()) {
                for (JsonNode typeNode : typesNode) {
                    types.add(typeNode.asText());
                }
            } else {
                throw new IllegalArgumentException("Google API 응답에서 'types' 필드를 찾을 수 없습니다.");
            }
            PlaceCategory.PlaceType placeType = PlaceCategory.determinePlaceType(types);

            // 4️⃣ `regularOpeningHours` 저장
            JsonNode openingHoursNode = placeDetails.path("regularOpeningHours");
            String filteredRegularOpeningHoursJson = "{}"; // 기본값
            if (openingHoursNode != null && !openingHoursNode.isMissingNode()) {
                ObjectNode filteredJson = objectMapper.createObjectNode();
                filteredJson.set("periods", openingHoursNode.path("periods"));
                filteredJson.set("weekdayDescriptions", openingHoursNode.path("weekdayDescriptions"));
                filteredRegularOpeningHoursJson = filteredJson.toString();
            }
            String resultRegularOpeningHoursJson = filteredRegularOpeningHoursJson;

            // 5️⃣ 첫 번째 사진 URL 가져오기
            JsonNode photosNode = placeDetails.path("photos");
            if (photosNode.isArray() && photosNode.size() > 0) {
                String photoReference = photosNode.get(0).path("name").asText();
                String imageUrl = getGooglePlacePhotoUrl(photoReference);

                // 6️⃣ 비동기적으로 S3 업로드 후 장소 저장
                return s3Service.uploadImageFromUrl(imageUrl, placeId)
                        .exceptionally(e -> {
                            System.err.println("업로드 실패: " + e.getMessage());
                            return null;  // 실패 시 null 반환
                        })
                        .thenCompose(s3Url -> CompletableFuture.supplyAsync(() -> {
                            // 7️⃣ Place 저장
                            Place newPlace = new Place(
                                    placeId, placeName, address, latitude, longitude,
                                    description, rating, ratingCount, googleUrl, s3Url, placeType, resultRegularOpeningHoursJson
                            );

                            newPlace = placeRepository.save(newPlace);

                            Map<String, Object> response = new HashMap<>();
                            response.put("status", 201);
                            response.put("message", "새로운 장소가 추가되었습니다.");
                            response.put("place", PlaceDTO.from(newPlace));

                            return ResponseEntity.status(HttpStatus.CREATED).body(response);
                        }));
            } else {
                // 7️⃣ 이미지가 없을 경우 바로 장소 저장
                Place newPlace = new Place(
                        placeId, placeName, address, latitude, longitude,
                        description, rating, ratingCount, googleUrl, null, placeType, resultRegularOpeningHoursJson
                );

                newPlace = placeRepository.save(newPlace);

                Map<String, Object> response = new HashMap<>();
                response.put("status", 201);
                response.put("message", "새로운 장소가 추가되었습니다.");
                response.put("place", PlaceDTO.from(newPlace));

                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CREATED).body(response));
            }
        }).thenCompose(result -> result); // 결과를 최종 CompletableFuture 형태로 변환
    }




// 장소 저장
//    @Transactional
//    public Place saveOrUpdatePlace(String googlePlaceId, String placeName, String address, double latitude, double longitude) {
//        return placeRepository.findByGooglePlaceId(googlePlaceId)
//                .map(existingPlace -> {
//                    existingPlace.incrementSearchCount();
//                    return placeRepository.save(existingPlace);
//                })
//                .orElseGet(() -> placeRepository.save(new Place(googlePlaceId, placeName, address, latitude, longitude)));
//    }


// AutoComplete 기능  -> 미사용하기로 결정
//    public String getAutocompleteResults(String input) {
//        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
//                "?input=" + input +
//                "&key=" + googleMapsApiKey +
//                "&components=country:KR&language=ko";
//
//        try {
//            return restTemplate.getForObject(url, String.class);
//        } catch (Exception e) {
//            throw new RuntimeException("Google Places Autocomplete API 호출 중 오류 발생", e);
//        }
//    }

    public String getPlaceDetails(String placeId) {
        String url = "https://places.googleapis.com/v1/places/" + placeId + "?languageCode=ko";

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleMapsApiKey);
        headers.set("X-Goog-FieldMask", "id,displayName,formattedAddress,location,types,rating,userRatingCount,regularOpeningHours,photos");

        // 요청 객체 생성
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // API 요청
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Google API 오류: " + response.getStatusCode());
        }

        return response.getBody();
    }




    //googleMapsApiKey
    public String searchPlaces(String query, double lat, double lng, double radius) {
        String url = "https://places.googleapis.com/v1/places:searchText";

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleMapsApiKey);
        headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.types,places.location");

        // 요청 본문 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("textQuery", query);
        requestBody.put("languageCode", "ko");

        // locationBias 추가
        Map<String, Object> locationBias = new HashMap<>();
        Map<String, Object> circle = new HashMap<>();
        Map<String, Object> center = new HashMap<>();
        center.put("latitude", lat);
        center.put("longitude", lng);
        circle.put("center", center);
        circle.put("radius", radius);
        locationBias.put("circle", circle);
        requestBody.put("locationBias", locationBias);

        // 요청 객체 생성
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // API 요청
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Google API 오류: " + response.getStatusCode());
        }

        return response.getBody();
    }

    // Google Place 이미지 API URL 생성
    private String getGooglePlacePhotoUrl(String photoReference) {
        return "https://places.googleapis.com/v1/" +photoReference + "/media?maxHeightPx=1200&maxWidthPx=1200&key=" + googleMapsApiKey;
    }

    // Google Place API 응답 JSON 파싱
    private JsonNode parseJson(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 오류", e);
        }
    }

    // JSON에서 types 리스트 파싱
    private List<String> parseTypes(JsonNode typesNode) {
        return typesNode.isArray() ?
                typesNode.findValuesAsText("text") :
                List.of();
    }

}
