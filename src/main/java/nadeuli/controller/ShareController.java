/* ShareController.java
 * ShareController
 * 공유 관련 페이지 컨트롤러
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-06
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철     2025.03.07    최초작성
 *
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.response.ItineraryWithOwnerDTO;
import nadeuli.repository.ItineraryRepository;
import nadeuli.service.ShareService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/join")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final ItineraryRepository itineraryRepository;

    @GetMapping("/{token}")
    public String shareJoinPage(@PathVariable String token, Model model) {
        try {
            ItineraryWithOwnerDTO itineraryDTO = shareService.getItineraryFromToken(token);

            model.addAttribute("itinerary", itineraryDTO);

            model.addAttribute("token", token);
            return "join";  // Thymeleaf 템플릿 (join.html) 반환
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "error";  // 오류 페이지
        }
    }

//    @PostMapping("/{token}")
//    public String shareJoin(@PathVariable String token, RedirectAttributes redirectAttributes,  @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long userId = userDetails.getUser().getId();
//
//        try {
//            String message = shareService.joinItinerary(token, userId);
//            redirectAttributes.addFlashAttribute("successMessage", message);
//            return "redirect:/join/" + token;  // 성공 후 다시 일정 페이지로 이동
//        } catch (IllegalArgumentException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/join/" + token;
//        }
//    }
}
