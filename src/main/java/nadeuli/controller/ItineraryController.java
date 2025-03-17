/* ItineraryController.java
 * Itinerary 컨트롤러
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     일정생성페이지(테스트용) 추가
 * 박한철    2025.02.26     일정리스트페이지(테스트용) 추가
 * 박한철    2025.02.26     캘린더선택페이지(테스트용) 추가 -> 삭제
 * 박한철    2025.03.11     테스트 페이지 정리
 * 박한철    2025.03.15     템플릿 리턴 경로 수정 "/.../..." -> ".../..."
 * 박한철    2025.03.17     view 페이지 추가
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.service.ItineraryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;
    @Value("${google.api.key}")
    private String googleMapsApiKey;
    // ===========================
    //  일정 생성 페이지
    // ===========================
    @GetMapping("/create")
    public String showCreateItineraryPage() {
        return "itinerary/create";  // itinerary/create.html을 반환
    }

    @GetMapping("/mylist")
    public String showMyItineraryPage() {
        return "itinerary/mylist";  // itinerary/mylist.html을 반환
    }

    @GetMapping("/edit/{itineraryId}")
    public String showEditPage(@PathVariable Long itineraryId, Model model) {
        model.addAttribute("googleApiKey", googleMapsApiKey);
        return "itinerary/edit";  // 정적 HTML 페이지 반환
    }

    @GetMapping("/view/{itineraryId}")
    public String showViewPage(@PathVariable Long itineraryId, Model model) {
        model.addAttribute("googleApiKey", googleMapsApiKey);
        return "itinerary/view";  // 정적 HTML 페이지 반환
    }

    @GetMapping("/region")
    public String regionTestPage() {
        return "region-test";
    }

}
