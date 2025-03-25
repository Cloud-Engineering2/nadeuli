package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.PlaceRequest;
import nadeuli.dto.request.PlaceListResponseDto;
import nadeuli.dto.request.PlaceRecommendRequestDto;
import nadeuli.dto.request.RouteRequestDto;
import nadeuli.dto.response.RouteResponseDto;
import nadeuli.service.ItineraryEventService;
import nadeuli.service.PlaceCacheService;
import nadeuli.service.PlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceCacheService placeCacheService;
    private final PlaceService placeService;
    private final ItineraryEventService itineraryEventService;


    @PostMapping("/search")
    public ResponseEntity<Void> searchPlace(@RequestBody PlaceRequest request) {
        System.out.println("장소 저장 요청: " + request);
        placeCacheService.saveToCache(request.getUserId(), request.getPlaceName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search/history")
    public ResponseEntity<List<String>> getUserSearchHistory(@RequestParam String userId) {
        List<String> searchHistory = placeCacheService.getUserSearchHistory(userId);
        return ResponseEntity.ok(searchHistory);
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


    @PostMapping("/routes")
    public ResponseEntity<List<RouteResponseDto>> getRoutes(@RequestBody List<RouteRequestDto> requests) {
        List<CompletableFuture<RouteResponseDto>> futures = requests.stream()
                .map(placeService::computeRouteAsync)
                .toList();

        List<RouteResponseDto> result = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return ResponseEntity.ok(result);
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
