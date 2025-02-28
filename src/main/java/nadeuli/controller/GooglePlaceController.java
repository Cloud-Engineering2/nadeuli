package nadeuli.controller;

import nadeuli.dto.PlaceRequest;
import nadeuli.entity.Place;
import nadeuli.service.PlaceCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class GooglePlaceController {

    private final PlaceCacheService placeCacheService;

    @PostMapping("/search")
    public ResponseEntity<Place> searchPlace(@RequestBody PlaceRequest request) {
        System.out.println("장소 저장 요청: " + request);
        Place place = placeCacheService.getPlace(request.getUserId(), request.getGooglePlaceId(), request.getPlaceName());
        System.out.println("장소 저장 완료: " + place);
        return ResponseEntity.ok(place);
    }

    @GetMapping("/search/history")
    public ResponseEntity<List<Place>> getUserSearchHistory(@RequestParam String userId) {
        List<Place> searchHistory = placeCacheService.getUserSearchHistory(userId);
        return ResponseEntity.ok(searchHistory);
    }
}
