package nadeuli.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GoogleMapPageController {

//    @GetMapping("/api/place/search")
//    public String showMapPage() {
//        return "index";
//    }

    @GetMapping("/itinerary/testing")
    public String testMapPage() {
        return "/itinerary/place-tab-test";
    }
}
