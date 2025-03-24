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
 * 고민정    2025.03.11   예산 산정 메서드 삭제
 * 고민정    2025.03.11   최종, itinerary event 별 정산 메서드 추가
 * 박한철    2025.03.17   합산 금액 리턴값 추가
 * 고민정    2025.03.24   expense book에 포함된 모든 expense item 조회 메서드 추가
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseBookDTO;
import nadeuli.dto.ExpenseItemDTO;
import nadeuli.dto.response.AdjustmentResponseDTO;
import nadeuli.dto.response.EventExpenseSummaryTotalResponseDTO;
import nadeuli.dto.response.FinanceResponseDTO;
import nadeuli.service.ExpenseBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseBookController {

    private final ExpenseBookService expenseBookService;

    @GetMapping("/{iid}/expense-summary")
    public ResponseEntity<EventExpenseSummaryTotalResponseDTO> getEventExpenseSummary(@PathVariable("iid") Long iid) {
        EventExpenseSummaryTotalResponseDTO response = expenseBookService.getEachTotalExpense(iid);
        return ResponseEntity.ok(response);
    }


    // ItineraryEvent 별 정산 : 총 지출, 1/n 정산
    @GetMapping("/{iid}/events/{ieid}/adjustment")
    public ResponseEntity<FinanceResponseDTO> getAdjustment(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid) {
        // PathVariable
        Long itineraryId = Long.valueOf(iid);
        Long itineraryEventId = Long.valueOf(ieid);

        FinanceResponseDTO response = expenseBookService.calculateMoney(itineraryId, itineraryEventId);

        return ResponseEntity.ok(response);
    }


    // Total 예산/잔액/지출 조회 및 Traveler 별 최종 1/n 정산
    @GetMapping("/{iid}/adjustment")
    public ResponseEntity<AdjustmentResponseDTO> getFinalAdjustment(@PathVariable("iid") Integer iid) {
        Long itineraryId = Long.valueOf(iid);

        FinanceResponseDTO financeResponseDTO = expenseBookService.getAdjustment(itineraryId);
        Long totalExpense = financeResponseDTO.getTotalExpense();

        System.out.println("totalExpense: " + totalExpense); // 🔥 확인

        // 지출, 잔액 갱신
        ExpenseBookDTO expenseBookDto = expenseBookService.updateExpenseBook(itineraryId, totalExpense);

        Long balance = expenseBookDto.getTotalBudget() - totalExpense;


        return ResponseEntity.ok(new AdjustmentResponseDTO(financeResponseDTO.getAdjustment(),
                                                            financeResponseDTO.getEachExpenses(),
                                                            expenseBookDto,
                                                            balance)
                                );
    }



    // itinerary(ExpenseBook) 별 모든 expense item 조회
    @GetMapping("/{iid}/expense")
    public ResponseEntity<List<ExpenseItemDTO>> getExpense(@PathVariable("iid") Integer iid) {
        Long itineraryId = Long.valueOf(iid);

        List<ExpenseItemDTO> expenseItemDtos = expenseBookService.getAllExpenseItems(itineraryId);
        return ResponseEntity.ok(expenseItemDtos);
    }


}
