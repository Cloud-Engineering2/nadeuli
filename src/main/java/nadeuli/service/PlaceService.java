/* PlaceService.java
 * Place 서비스
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 김대환      2025.02.25   초기 작성
 * 박한철      2025.03.09   구글 TextSearch 위도경도반경 제한 포함  API 프록시 함수 작성
 * 박한철      2025.03.10   getRecommendedPlacesWithCursor 작성 (나들이 장소 페이징 커서)
 * 박한철      2025.03.11   computeRouteAsync   카카오 거리 API 작성
 * 김대환      2025.03.12   구글 TextSearch 캐싱 추가
 * 박한철      2025.03.12   카카오 computeRouteAsync 캐싱 추가
 * ========================================================
 */
package nadeuli.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import nadeuli.dto.PlaceDTO;
import nadeuli.dto.request.PlaceListResponseDto;
import nadeuli.dto.request.RouteRequestDto;
import nadeuli.dto.response.PlaceResponseDto;
import nadeuli.dto.response.RouteResponseDto;
import nadeuli.common.enums.PlaceCategory;
import nadeuli.repository.PlaceNativeQueryExecutor;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nadeuli.entity.Place;
import nadeuli.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;
    private final PlaceNativeQueryExecutor placeNativeQueryExecutor;

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final S3Service s3Service;

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    @Value("${kakao.api.key}")
    private String kakaoRestApiKey;

    private static final String CACHE_PREFIX = "place_details:";
    private static final Duration CACHE_TTL = Duration.ofDays(5);
    private static final Duration SEARCH_CACHE_TTL = Duration.ofHours(5);
    private static final Duration ROUTE_CACHE_TTL = Duration.ofDays(30);

    // 나들이 장소 커서 페이징 호출함수
    public PlaceListResponseDto getRecommendedPlacesWithCursor(double userLng, double userLat, double radius, Double cursorScore, Long cursorId, int pageSize, List<String> placeTypes, boolean searchEnabled, String searchQuery) {
        List<Object[]> result = placeNativeQueryExecutor.findPlacesWithDynamicQuery(userLng, userLat, radius, cursorScore, cursorId, pageSize, placeTypes, searchEnabled, searchQuery);

        List<PlaceResponseDto> places = result.stream().map(row -> PlaceResponseDto.builder().id(((Number) row[0]).longValue()).googlePlaceId((String) row[1]).placeName((String) row[2]).searchCount(((Number) row[3]).intValue()).address((String) row[4]).latitude(((Number) row[5]).doubleValue()).longitude(((Number) row[6]).doubleValue()).explanation((String) row[7]).googleRating((row[8] != null) ? ((Number) row[8]).doubleValue() : null).googleRatingCount((row[9] != null) ? ((Number) row[9]).intValue() : null).googleURL((String) row[10]).imageUrl((String) row[11]).placeType(PlaceCategory.PlaceType.valueOf((String) row[12])).regularOpeningHours((String) row[13]).distance(((Number) row[14]).doubleValue()).finalScore(((Number) row[15]).doubleValue()).build()).toList();

        Double nextCursorScore = null;
        Long nextCursorId = null;
        if (!places.isEmpty()) {
            PlaceResponseDto last = places.get(places.size() - 1);
            nextCursorScore = last.getFinalScore();
            nextCursorId = last.getId();
        }

        return PlaceListResponseDto.builder().places(places).nextCursorScore(nextCursorScore).nextCursorId(nextCursorId).build();
    }


    // 나들이서버에 새로운 장소를 추가하는 함수
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
            String explanation = "Google Place API에서 제공하는 기본 데이터"; // 필요 시 추가 가공

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
                return s3Service.uploadImageFromUrl(imageUrl, placeId).exceptionally(e -> {
                    System.err.println("업로드 실패: " + e.getMessage());
                    return null;  // 실패 시 null 반환
                }).thenCompose(s3Url -> CompletableFuture.supplyAsync(() -> {
                    // 7️⃣ Place 저장
                    Place newPlace = new Place(placeId, placeName, address, latitude, longitude, explanation, rating, ratingCount, googleUrl, s3Url, placeType, resultRegularOpeningHoursJson);

                    newPlace = placeRepository.save(newPlace);

                    Map<String, Object> response = new HashMap<>();
                    response.put("status", 201);
                    response.put("message", "새로운 장소가 추가되었습니다.");
                    response.put("place", PlaceDTO.from(newPlace));

                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                }));
            } else {
                // 7️⃣ 이미지가 없을 경우 바로 장소 저장
                Place newPlace = new Place(placeId, placeName, address, latitude, longitude, explanation, rating, ratingCount, googleUrl, null, placeType, resultRegularOpeningHoursJson);

                newPlace = placeRepository.save(newPlace);

                Map<String, Object> response = new HashMap<>();
                response.put("status", 201);
                response.put("message", "새로운 장소가 추가되었습니다.");
                response.put("place", PlaceDTO.from(newPlace));

                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CREATED).body(response));
            }
        }).thenCompose(result -> result); // 결과를 최종 CompletableFuture 형태로 변환
    }


    public String getAutocompleteResults(String input) {
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" + "?input=" + input + "&key=" + googleMapsApiKey + "&components=country:KR&language=ko";

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Google Places Autocomplete API 호출 중 오류 발생", e);
        }
    }

    //구글 장소 Detail API 프록시
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

    //카카오 장소간 거리(시간) 측정 API 프록시
    @Async
    public CompletableFuture<RouteResponseDto> computeRouteAsync(RouteRequestDto dto) {
        // 캐시 키 구성
        String cacheKey = String.format(
                "route_cache:%.6f:%.6f:%.6f:%.6f",
                dto.getOriginLatitude(), dto.getOriginLongitude(),
                dto.getDestinationLatitude(), dto.getDestinationLongitude()
        );

        // Redis 캐시 조회
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            System.out.println("✅ 캐시에서 거리/시간 정보 반환: " + cacheKey);
            try {
                // JSON 문자열 → DTO 파싱
                RouteResponseDto cachedDto = objectMapper.readValue(cachedResult, RouteResponseDto.class);
                return CompletableFuture.completedFuture(cachedDto);
            } catch (Exception e) {
                System.err.println("캐시 응답 파싱 오류: " + e.getMessage());
                // 캐시 파싱 실패 시 캐시 무시하고 진행
            }
        }

        // API 호출 URL 구성
        String url = "https://apis-navi.kakaomobility.com/v1/directions";

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("origin", dto.getOriginLongitude() + "," + dto.getOriginLatitude())
                .queryParam("destination", dto.getDestinationLongitude() + "," + dto.getDestinationLatitude())
                .queryParam("summary", true)
                .queryParam("priority", "RECOMMEND")
                .queryParam("car_fuel", "GASOLINE")
                .queryParam("car_hipass", true)
                .queryParam("alternatives", false)
                .queryParam("road_details", false);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Kakao Directions API 응답 오류: " + response.getStatusCode());
            }

            System.out.println("[DEBUG] Kakao Directions API 응답: " + response.getBody());

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode route = json.path("routes").get(0);

            int resultCode = route.path("result_code").asInt(-1);
            if (resultCode != 0) {
                System.out.println("❌ Kakao 길찾기 실패 result_code=" + resultCode);
                return CompletableFuture.completedFuture(new RouteResponseDto(0, 0));
            }

            double distance = route.path("summary").path("distance").asDouble(0); // meters
            int distanceMeters = (int) Math.round(distance);

            double durationSeconds = route.path("summary").path("duration").asDouble(0); // seconds
            int durationMinutes = (int) Math.ceil(durationSeconds / 60.0);

            RouteResponseDto resultDto = new RouteResponseDto(distanceMeters, durationMinutes);

            // 캐시에 저장 (JSON 문자열로 직렬화)
            String jsonToCache = objectMapper.writeValueAsString(resultDto);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, ROUTE_CACHE_TTL);
            System.out.println("✅ 거리/시간 정보 캐시에 저장: " + cacheKey);

            return CompletableFuture.completedFuture(resultDto);

        } catch (Exception e) {

            System.err.println("Kakao 응답 파싱 오류: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(new RouteResponseDto(-1, 0));
        }
    }



    //구글 TextSearch 위도경도반경 제한 포함  API 프록시
    public String searchPlaces(String query, double lat, double lng, double radius) {
        String cacheKey = String.format("search_places:%s:%.6f:%.6f:%.1f", query, lat, lng, radius);

        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            System.out.println("캐시에서 검색 결과 반환: " + cacheKey);
            return cachedResult;
        }

        System.out.println("Google Places API 요청 실행...");

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

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            System.out.println("Google API 호출 중 예외 발생: " + e.getMessage());
            return null; //
        }

        // 응답 상태 코드 검증
        System.out.println("응답 코드: " + response.getStatusCode());
        if (!response.getStatusCode().is2xxSuccessful()) {
            System.out.println("응답 상태 코드: " + response.getStatusCode());
            return null;
        }

        // 응답 본문 확인 (디버깅용)
        String responseBody = response.getBody();
        System.out.println("Google API Response Body: " + responseBody);

        if (responseBody == null || responseBody.isEmpty()) {
            System.out.println("Google API 응답 본문이 비어 있음.");
            return null;
        }

        // 캐시에 저장
        redisTemplate.opsForValue().set(cacheKey, responseBody, SEARCH_CACHE_TTL);
        System.out.println("검색 결과를 캐시에 저장: " + cacheKey);

        return responseBody;
    }

    // Google Place 이미지 API URL 생성
    private String getGooglePlacePhotoUrl(String photoReference) {
        return "https://places.googleapis.com/v1/" + photoReference + "/media?maxHeightPx=1200&maxWidthPx=1200&key=" + googleMapsApiKey;
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
        return typesNode.isArray() ? typesNode.findValuesAsText("text") : List.of();
    }


}
