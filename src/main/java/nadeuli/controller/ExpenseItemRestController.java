/* ExpenseItemRestController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.27   지출 내역 CRUD 추가
 * 고민정    2025.03.10   지출 추가 메서드 반환값 변경

 * ========================================================
 */
package nadeuli.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.ExpenseItemDTO;
import nadeuli.dto.TravelerDTO;
import nadeuli.dto.request.ExpenseItemRequestDTO;
import nadeuli.dto.request.ExpenseItemUpdateRequestDTO;
import nadeuli.service.ExpenseBookService;
import nadeuli.service.ExpenseItemService;
import nadeuli.service.TravelerService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseItemRestController {
    private final ExpenseItemService expenseItemService;
    private final TravelerService travelerService;
    private final ExpenseBookService expenseBookService;

    // 지출 내역 추가
    @PostMapping("/{iid}/events/{ieid}/expense")
    public ResponseEntity<ExpenseItemDTO> createExpense(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid, @RequestBody @Valid ExpenseItemRequestDTO expenseItemRequestDTO, BindingResult bindingResult) {
        // 유효성 검사
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }
        // 요청 가져오기
        String content = expenseItemRequestDTO.getContent();
        String payerName = expenseItemRequestDTO.getPayer();
        Long expense = Long.valueOf(expenseItemRequestDTO.getExpense());

        // PathVariable
        Long itineraryId = Long.valueOf(iid);
        Long itineraryEventId = Long.valueOf(ieid);

        // Payer
        TravelerDTO payer = travelerService.getByName(itineraryId, payerName);

        // ExpenseBook
        Long expenseBookId = expenseBookService.get(itineraryId);

        if (content == null) {
            ExpenseItemDTO expenseItemDto = ExpenseItemDTO.of(expenseBookId, itineraryEventId, payer, "", expense);
            expenseItemService.addExpense(expenseItemDto);
            return ResponseEntity.ok().build();
        }

        ExpenseItemDTO expenseItemDto = ExpenseItemDTO.of(expenseBookId, itineraryEventId, payer, content, expense);

        // 추가
        ExpenseItemDTO response = expenseItemService.addExpense(expenseItemDto);

        return ResponseEntity.ok(response);
    }


    // 지출 내역 조회 (ItineraryEvent 내 모든 지출 내역)
    @GetMapping("/{iid}/events/{ieid}/expense")
    public ResponseEntity<List<ExpenseItemDTO>> getExpense(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid) {
        // PathVariable
        Long itineraryEventId = Long.valueOf(ieid);

        List<ExpenseItemDTO> expenseItemDtos = expenseItemService.getAll(itineraryEventId);

        return ResponseEntity.ok(expenseItemDtos);
    }

    // 지출 내역 수정
    @PutMapping("/{iid}/events/{ieid}/expense/{emid}")
    public ResponseEntity<ExpenseItemDTO> updateExpense(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid, @PathVariable("emid") Integer emid, @RequestBody @Valid ExpenseItemUpdateRequestDTO expenseItemUpdateRequestDTO) {
        // PathVariable
        Long itineraryId = Long.valueOf(iid);
        Long expenseItemId = Long.valueOf(emid);

        ExpenseItemDTO expenseItemDTO = expenseItemService.updateExpenseItem(itineraryId, expenseItemId, expenseItemUpdateRequestDTO);

        return ResponseEntity.ok(expenseItemDTO);
    }


    // 지출 내역 삭제
    @DeleteMapping("/{iid}/events/{ieid}/expense/{emid}")
    public ResponseEntity<Void> deleteExpense(@PathVariable("iid") Integer iid, @PathVariable("ieid") Integer ieid, @PathVariable("emid") Integer emid) {
        Long expenseItemId = Long.valueOf(emid);
        expenseItemService.deleteExpenseItem(expenseItemId);
        return ResponseEntity.ok().build();
    }




}
