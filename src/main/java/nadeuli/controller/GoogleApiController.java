package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.PlaceRequest;
import nadeuli.service.PlaceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
public class GoogleApiController {

    private final PlaceService placeService;

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    @GetMapping("/apikey")
    public ResponseEntity<String> getApiKey() {
        return ResponseEntity.ok(googleMapsApiKey);
    }


    @GetMapping("/autocomplete")
    public ResponseEntity<String> autocomplete(@RequestParam String input) {
        return ResponseEntity.ok(placeService.getAutocompleteResults(input));
    }

    @PostMapping("/place-details")
    public ResponseEntity<Map<String, Object>> getPlaceDetails(@RequestBody PlaceRequest request) {
        Map<String, Object> placeDetails = placeService.fetchPlaceDetails(request.getPlaceId(), request.getMainText());
        return ResponseEntity.ok(placeDetails);
    }
}
