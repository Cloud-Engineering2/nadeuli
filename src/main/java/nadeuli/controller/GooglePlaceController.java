package nadeuli.controller;

import nadeuli.dto.PlaceRequest;
import nadeuli.entity.Place;
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
    public ResponseEntity<Place> searchPlace(@RequestBody PlaceRequest request) {
        System.out.println("장소 저장 요청: " + request);
        Place place = placeCacheService.getPlace(request.getUserId(), request.getPlaceId(), request.getPlaceName(), request.getAddress(), request.getLatitude(), request.getLongitude());
        System.out.println("장소 저장 완료: " + place);
        return ResponseEntity.ok(place);
    }

    @GetMapping("/search/history")
    public ResponseEntity<List<String>> getUserSearchHistory(@RequestParam String userId) {
        List<String> searchHistory = placeCacheService.getUserSearchHistory(userId);
        return ResponseEntity.ok(searchHistory);
    }
}
