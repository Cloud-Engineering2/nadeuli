package nadeuli.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/map")
public class GoogleMapsProxyController {

    private final RestTemplate restTemplate;

    @Value("${google.api.key}")
    private String googleMapsApiKey;

    public GoogleMapsProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/apikey")
    public ResponseEntity<String> getApiKey() {
        return ResponseEntity.ok(googleMapsApiKey);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<String> autocomplete(@RequestParam String input) {
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                "?input=" + input +
                "&key=" + googleMapsApiKey +
                "&components=country:KR&language=ko";

        return restTemplate.getForEntity(url, String.class);
    }

    @GetMapping("/place-details")
    public ResponseEntity<String> getPlaceDetails(@RequestParam String placeId) {
        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + placeId +
                "&key=" + googleMapsApiKey;

        return restTemplate.getForEntity(url, String.class);
    }
}
