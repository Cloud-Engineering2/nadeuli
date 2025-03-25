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
 * 고민정    2025.03.25     view title 데이터 전송
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.auth.oauth.CustomUserDetails;
import nadeuli.dto.ItineraryDTO;
import nadeuli.service.ItineraryCollaboratorService;
import nadeuli.service.ItineraryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryCollaboratorService collaboratorService;
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
    public String showEditPage(@PathVariable Long itineraryId, Model model,  @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("googleApiKey", googleMapsApiKey);

        Long userId = userDetails.getUser().getId();

        // 일정 수정 권한 체크 (일정 파트 = isExpensePart=false)
        collaboratorService.checkEditPermission(userId, itineraryId, false);

        return "itinerary/edit";  // 정적 HTML 페이지 반환
    }

    @GetMapping("/view/{itineraryId}")
    public String showViewPage(@PathVariable Long itineraryId, Model model,  @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("googleApiKey", googleMapsApiKey);

        Long userId = userDetails.getUser().getId();

        // 일정 읽기 권한 체크
        collaboratorService.checkViewPermission(userId, itineraryId);

        ItineraryDTO itineraryDto = itineraryService.getItinerary(itineraryId);
        model.addAttribute("itineraryName", itineraryDto.getItineraryName());

        return "itinerary/view";  // 정적 HTML 페이지 반환
    }


}
