/* JournalController.java
 * nadeuli Service - 여행
 * 기행문 관련 Controller
 * 작성자 : 이홍비
 * 최초 작성 일자 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.03.20     최초 작성 : Controller 와 RestController 분리
 *                         로그인 인증 관련 처리
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.security.CustomUserDetails;
import nadeuli.service.ItineraryCollaboratorService;
import nadeuli.service.JournalService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequiredArgsConstructor
@Controller
public class JournalController {
    private final JournalService journalService;
    private final ItineraryCollaboratorService itineraryCollaboratorService;


    // 기행문 조회 (열람)
    @GetMapping("/itineraries/{iid}/events/{ieid}/journal")
    public String redirectToJournalPage(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid, @AuthenticationPrincipal CustomUserDetails userDetails) {

        itineraryCollaboratorService.checkViewPermission(userDetails.getUser().getId(), iid); // 로그인 인증

        journalService.getJournal(ieid); // exception 발생 시 error.html 로 바로 이동하기 위해서 작성

        System.out.println("📌 Journal.html 로 이동");

        return "journal/journal";
    }
}