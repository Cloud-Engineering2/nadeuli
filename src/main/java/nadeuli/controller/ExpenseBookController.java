/* OpenAITravelResponse.java
 * OPEN API 연동
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-21
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.BudgetReq;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.service.ExpenseBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/itineraries", produces = "application/json")
@RequiredArgsConstructor
public class ExpenseBookController {

    private final ExpenseBookService expenseBookService;

    // 예산 설정  /api/itineraries/{iid}/budget
    @PutMapping("/{iid}/budget")
    public ResponseEntity<ExpenseBookDTO> planBudget(@RequestBody @Valid BudgetReq budgetReq,
                                             @PathVariable("iid") Long iid,
                                             BindingResult bindingResult) {
//                                                @AuthenticationPrincipal WbcUserDetails wbcUser) {
        Integer budget = budgetReq.getTotalBudget();
        ExpenseBookDTO expenseBookDto = expenseBookService.setBudget(iid, budget);

        return ResponseEntity.ok(expenseBookDto);


    }


    @GetMapping
    public String tester() {
        System.out.println("tester");
        return "여기";
    }
}
