/* ExpenseItemController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.03.04  basic 페이지
 * 고민정    2025.03.07  오른쪽 화면 경비 내역 페이지 로드
 * ========================================================
 */
package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/itinerary")
@RequiredArgsConstructor
public class ExpenseItemController {


    // Itinerary Item 별 정산 조회 (페이지)
    @GetMapping("/{iid}")
    public String getBasicPage(@PathVariable("iid") Long iid) {
        System.out.println("📌 itinerary-event-basic 페이지 가져오기");

        return "itinerary-event-basic";
    }

    @GetMapping("/{iid}/events/{ieid}/expense-right")
    public String getExpensePage(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid) {

        System.out.println("📌 expense-book/expense-right 페이지 가져오기");

        return "expense-book/expense-right";
    }

    @GetMapping("/{iid}/events/{ieid}/adjustment-right")
    public String getAdjustmentPage(@PathVariable("iid") Long iid, @PathVariable("ieid") Long ieid) {

        System.out.println("📌 expense-book/adjustment-right 페이지 가져오기");

        return "expense-book/adjustment-right";
    }


}
