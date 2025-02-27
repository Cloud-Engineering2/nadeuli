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
 * 고민정    2025.02.27   지출 작성 메서드 추가
 * ========================================================
 */
package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseItemDTO;
import nadeuli.dto.ExpenseItemRequest;
import nadeuli.dto.ExpenseItemUpdateRequest;
import nadeuli.dto.TravelerDTO;
import nadeuli.service.ExpenseBookService;
import nadeuli.service.ExpenseItemService;
import nadeuli.service.ItineraryService;
import nadeuli.service.TravelerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseItemController {
    private final ExpenseItemService expenseItemService;
    private final TravelerService travelerService;
    private final ExpenseBookService expenseBookService;
    private final ItineraryService itineraryService;

    // 지출 내역 추가
    @PostMapping("/{iid}/events/{ieid}/expense")
    public ResponseEntity<Void> createExpense(@PathVariable Integer iid, @PathVariable Integer ieid, @RequestBody ExpenseItemRequest expenseItemRequest) {
        String content = expenseItemRequest.getContent();
        String payerName = expenseItemRequest.getPayer();
        Long expense = Long.valueOf(expenseItemRequest.getExpense());

        // PathVariable
        Long itineraryId = Long.valueOf(iid);
        Long itineraryEventId = Long.valueOf(ieid);

        // Payer
        TravelerDTO payer = travelerService.get(payerName);

        // ExpenseBook
        Long expenseBookId = expenseBookService.get(itineraryId);

        if (content == null) {
            ExpenseItemDTO expenseItemDto = ExpenseItemDTO.of(expenseBookId, itineraryEventId, payer, "", expense);
            expenseItemService.addExpense(expenseItemDto);
            return ResponseEntity.ok().build();
        }

        ExpenseItemDTO expenseItemDto = ExpenseItemDTO.of(expenseBookId, itineraryEventId, payer, content, expense);

        // 추가
        expenseItemService.addExpense(expenseItemDto);
        return ResponseEntity.ok().build();
    }


    // 지출 내역 조회 (ItineraryEvent 내 모든 지출 내역)
    @GetMapping("/{iid}/events/{ieid}/expense")
    public ResponseEntity<List<ExpenseItemDTO>> getExpense(@PathVariable Integer ieid) {
        // PathVariable
        Long itineraryEventId = Long.valueOf(ieid);

        List<ExpenseItemDTO> expenseItemDtos = expenseItemService.getAll(itineraryEventId);

        return ResponseEntity.ok(expenseItemDtos);
    }

    // 지출 내역 수정
    @PutMapping("/{iid}/events/{ieid}/expense/{eiid}")
    public ResponseEntity<ExpenseItemDTO> updateExpense(@PathVariable Integer eiid, @RequestBody ExpenseItemUpdateRequest expenseItemUpdateRequest) {
        // PathVariable
        Long expenseItemId = Long.valueOf(eiid);

        ExpenseItemDTO expenseItemDTO = expenseItemService.updateExpenseItem(expenseItemId, expenseItemUpdateRequest);

        return ResponseEntity.ok(expenseItemDTO);
    }


    // 지출 내역 삭제
    @DeleteMapping("/{iid}/events/{ieid}/expense/{eiid}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Integer eiid) {
        Long expenseItemId = Long.valueOf(eiid);
        expenseItemService.deleteExpenseItem(expenseItemId);
        return ResponseEntity.ok().build();
    }


}
