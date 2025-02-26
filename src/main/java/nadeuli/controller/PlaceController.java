/* PlaceController
 * PlaceController 파일
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */
package nadeuli.controller;

import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.Place;
import nadeuli.repository.PlaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nadeuli.service.GooglePlacesService;
import nadeuli.service.SearchHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceRepository placeRepository;
    private final GooglePlacesService placesService;
    private final SearchHistoryService searchHistoryService;

    public PlaceController(GooglePlacesService placesService, SearchHistoryService searchHistoryService, PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
        this.placesService = placesService;
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping("/search")
    public ResponseEntity<String> searchPlaces(@RequestParam String query) {
        log.info("장소 검색 API 호출됨: query={}", query);
        String response = placesService.getPlaceData(query);

        String placeId = extractPlaceId(response);
        String placeName = extractPlaceName(response);

        log.info("검색된 place_id={}, place_name={}", placeId, placeName);

        if (placeId != null && placeName != null) {
            searchHistoryService.saveSearchHistory(placeId, placeName);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-searched")
    public ResponseEntity<List<Place>> getTopSearchedPlaces() {
        List<Place> topPlaces = placeRepository.findAll()
                .stream()
                .sorted((p1, p2) -> Integer.compare(p2.getSearchCount(), p1.getSearchCount())) // 내림차순 정렬
                .limit(10)
                .toList();

        return ResponseEntity.ok(topPlaces);
    }

    @GetMapping("/search/history")
    public ResponseEntity<List<String>> getSearchHistory() {
        List<String> history = searchHistoryService.getRecentSearchHistory();

        List<String> placeNames = history.stream()
                .map(searchHistoryService::getPlaceNameByPid)
                .filter(placeName -> placeName != null && !placeName.isEmpty())
                .collect(Collectors.toList());

        return ResponseEntity.ok(placeNames);
    }

    private String extractPlaceId(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);
            return root.path("candidates").get(0).path("place_id").asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractPlaceName(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);
            return root.path("candidates").get(0).path("name").asText();
        } catch (Exception e) {
            return null;
        }
    }
}
