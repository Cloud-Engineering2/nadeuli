package nadeuli.controller;

import nadeuli.dto.PlaceRequest;
import nadeuli.dto.request.PlaceListResponseDto;
import nadeuli.dto.request.PlaceRecommendRequestDto;
import nadeuli.dto.response.PlaceResponseDto;
import nadeuli.service.PlaceCacheService;
import lombok.RequiredArgsConstructor;
import nadeuli.service.PlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceCacheService placeCacheService;
    private final PlaceService placeService;


    @PostMapping("/search")
    public ResponseEntity<Void> searchPlace(@RequestBody PlaceRequest request) {
        System.out.println("장소 저장 요청: " + request);

        placeCacheService.saveToCache(
                request.getUserId(),
                request.getPlaceName(),
                request.getLatitude(),
                request.getLongitude(),
                1000
        );
        return ResponseEntity.ok().build();
    }


    @GetMapping("/search/history")
    public ResponseEntity<List<String>> getUserSearchHistory(@RequestParam String userId) {
        Set<String> searchHistorySet = placeCacheService.getUserSearchHistory(userId);
        List<String> searchHistoryList = new ArrayList<>(searchHistorySet);

        return ResponseEntity.ok(searchHistoryList);
    }


    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> addNewPlace(@RequestBody PlaceRequest request) {
        return placeService.addNewPlace(request.getPlaceId())
                .exceptionally(e -> {
                    // 예외 발생 시 처리
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", 400);
                    errorResponse.put("message", "장소 추가 중 오류 발생: " + e.getMessage());
                    return ResponseEntity.badRequest().body(errorResponse);
                });
    }

    /**
     * POST 방식 장소 추천 (커서 기반 페이징)
     */
    @PostMapping("/recommend")
    public PlaceListResponseDto getRecommendedPlaces(@RequestBody PlaceRecommendRequestDto request) {
        return placeService.getRecommendedPlacesWithCursor(
                request.getUserLng(),
                request.getUserLat(),
                request.getRadius(),
                request.getCursorScore(),
                request.getCursorId(),
                request.getPageSize(),
                request.getPlaceTypes(),
                request.isSearchEnabled(),
                request.getSearchQuery()
        );
    }


}
