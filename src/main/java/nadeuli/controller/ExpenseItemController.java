/* ExpenseItemController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.27   지출 내역 CRUD 추가
 *
 * ========================================================
 */
package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/itineraries")
@RequiredArgsConstructor
public class ExpenseItemController {


    // Itinerary Item 별 정산 조회 (페이지)
    @GetMapping("/{iid}/events/{ieid}/expense")
    public String getBasic(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid) {
//        JournalDTO journalDTO = journalService.getJournal(ieid);

        System.out.println("📌 페이지 가져오기");

        return "itinerary-event-basic";
    }
}
