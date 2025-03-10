package nadeuli.controller;

import nadeuli.dto.PlaceRequest;
import nadeuli.service.PlaceCacheService;
import lombok.RequiredArgsConstructor;
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

}
