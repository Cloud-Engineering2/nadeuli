package nadeuli.controller;

import nadeuli.dto.PlaceRequest;
import nadeuli.service.PlaceCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
public class GooglePlaceController {

    private final PlaceCacheService placeCacheService;

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
}
