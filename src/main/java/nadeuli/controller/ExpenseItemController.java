/* ExpenseItemController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   Controller 생성
 *
 * ========================================================
 */
package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseItemRequest;
import nadeuli.entity.Traveler;
import nadeuli.service.ExpenseItemService;
import nadeuli.service.TravelerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseItemController {
    private final ExpenseItemService expenseItemService;
    private final TravelerService travelerService;

    // 지출 추가
    @PostMapping("/{iid}/expenses")
    public ResponseEntity createExpense(@PathVariable Integer iid, @RequestBody ExpenseItemRequest expenseItemRequest) {
        String content = expenseItemRequest.getContent();
        Traveler payer = expenseItemRequest.getPayer();
        Integer expense = expenseItemRequest.getExpense();

        return null;
    }



}
