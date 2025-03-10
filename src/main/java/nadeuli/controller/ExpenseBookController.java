/* ExpenseBookController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   예산 산정 메서드 추가
 *
 * ========================================================
 */

package nadeuli.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.request.BudgetRequestDTO;
import nadeuli.dto.response.FinanceResponseDTO;
import nadeuli.repository.*;
import nadeuli.service.ExpenseBookService;
import nadeuli.service.ItineraryCollaboratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseBookController {

    private final ExpenseBookService expenseBookService;
    private final ItineraryCollaboratorService itineraryCollaboratorService;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryPerDayRepository itineraryPerDayRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;
    private final ExpenseBookRepository expenseBookRepository;


    // 예산 설정  /api/itineraries/{iid}/budget
    @PutMapping("/{iid}/budget")
    public ResponseEntity<ExpenseBookDTO> updateBudget(@RequestBody @Valid BudgetRequestDTO budgetRequestDTO,
                                             @PathVariable("iid") Long iid) {
        Long budget = budgetRequestDTO.getTotalBudget();
        ExpenseBookDTO expenseBookDto = expenseBookService.setBudget(iid, budget);

        return ResponseEntity.ok(expenseBookDto);
    }



    // 총 예산, 지출, 잔액 조회
    @GetMapping("/{iid}/expenses") // url 필요...!
    public ResponseEntity<FinanceResponseDTO> getTotalExpense(@PathVariable("iid") Integer iid) {
        Long itineraryId = Long.valueOf(iid);
        FinanceResponseDTO response = expenseBookService.calculateTotalMoney(itineraryId);

        return ResponseEntity.ok(response);
    }


    // ItineraryEvent 별 정산
    @GetMapping("/{iid}/events/{ieid}/adjustment")
    public ResponseEntity<FinanceResponseDTO> getAdjustment(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid) {
        // PathVariable
        Long itineraryEventId = Long.valueOf(ieid);
        Long itineraryId = Long.valueOf(iid);

        FinanceResponseDTO response = expenseBookService.calculateMoney(itineraryId, itineraryEventId);

        return ResponseEntity.ok(response);
    }
}
