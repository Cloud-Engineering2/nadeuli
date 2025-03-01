package nadeuli.controller;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.PlaceRequest;
import nadeuli.service.PlaceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/map")
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
        String response = placeService.getAutocompleteResults(input);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/place-details")
    public ResponseEntity<String> getPlaceDetails(@RequestBody PlaceRequest request) {
        String response = placeService.fetchPlaceDetails(request.getPlaceId(), request.getMainText());
        return ResponseEntity.ok(response);
    }


}
