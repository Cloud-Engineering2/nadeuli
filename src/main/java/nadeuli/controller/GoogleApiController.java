package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.PlaceDTO;
import nadeuli.dto.PlaceRequest;
import nadeuli.service.PlaceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/google-places")
@RequiredArgsConstructor
public class GoogleApiController {

    private final PlaceService placeService;

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    @GetMapping("/apikey")
    public ResponseEntity<String> getApiKey() {
        return ResponseEntity.ok(googleMapsApiKey);
    }

//    @GetMapping("/autocomplete")
//    public ResponseEntity<String> autocomplete(@RequestParam String input) {
//        return ResponseEntity.ok(placeService.getAutocompleteResults(input));
//    }

//    @PostMapping("/place-details")
//    public ResponseEntity<String> getPlaceDetails(@RequestBody PlaceRequest request) {
//        return ResponseEntity.ok(placeService.fetchPlaceDetails(request.getPlaceId(), request.getMainText()));
//    }

    @GetMapping("/search")
    public ResponseEntity<String> searchPlaces(
            @RequestParam String query,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20000") int radius) {

        String result = placeService.searchPlaces(query, lat, lng, radius);
        return ResponseEntity.ok(result);
    }




}
