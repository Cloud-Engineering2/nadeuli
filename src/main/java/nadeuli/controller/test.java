package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class test {
    @GetMapping("/api/test")
    @ResponseBody
    public String test() {
        return "status - 2025.2.21";
    }
}
