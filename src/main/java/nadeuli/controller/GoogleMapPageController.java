package nadeuli.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GoogleMapPageController {

    @GetMapping("/place/search")
    public String showMapPage() {
        return "index";
    }
}
