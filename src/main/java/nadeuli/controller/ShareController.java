package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.entity.Itinerary;
import nadeuli.repository.ItineraryRepository;
import nadeuli.service.ItineraryService;
import nadeuli.service.ShareService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/join")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final ItineraryRepository itineraryRepository;

    @GetMapping("/{token}")
    public String shareJoinPage(@PathVariable String token, Model model) {
        try {
            ItineraryDTO itineraryDTO = shareService.getItineraryFromToken(token);

            model.addAttribute("itinerary", itineraryDTO);
            model.addAttribute("token", token);
            return "join";  // Thymeleaf 템플릿 (join.html) 반환
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "error";  // 오류 페이지
        }
    }

    @PostMapping("/{token}")
    public String shareJoin(@PathVariable String token, RedirectAttributes redirectAttributes) {
        Long userId = 2L; // 세션 대신 임시 ID

        try {
            String message = shareService.joinItinerary(token, userId);
            redirectAttributes.addFlashAttribute("successMessage", message);
            return "redirect:/join/" + token;  // 성공 후 다시 일정 페이지로 이동
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/join/" + token;
        }
    }
}
