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
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.service.ItineraryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    // ===========================
    //  일정 생성 페이지
    // ===========================
    @GetMapping("/create")
    public String showCreateItineraryPage() {
        return "/itinerary/create";  // itinerary/create.html을 반환
    }

    @GetMapping("/mylist")
    public String showMyItineraryPage() {
        return "/itinerary/mylist";  // itinerary/mylist.html을 반환
    }

    @GetMapping("/write")
    public String showWriteTestPage() {
        return "/itinerary/write_test";  // itinerary/write_test.html을 반환
    }
}
