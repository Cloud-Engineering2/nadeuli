package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.PlaceRequest;
import nadeuli.entity.Place;
import nadeuli.service.PlaceCacheService;
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
        Place place = placeCacheService.getPlace(request.getPlaceId(), request.getPlaceName(), request.getAddress(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(place);
    }

    @GetMapping("/search/rankings")
    public ResponseEntity<List<String>> getTopSearchRankings() {
        List<String> rankings = placeCacheService.getTopSearchRankings();
        return ResponseEntity.ok(rankings);
    }
}
