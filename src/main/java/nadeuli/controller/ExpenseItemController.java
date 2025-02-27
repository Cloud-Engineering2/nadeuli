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
import nadeuli.dto.TravelerDTO;
import nadeuli.service.ExpenseBookService;
import nadeuli.service.ExpenseItemService;
import nadeuli.service.ItineraryService;
import nadeuli.service.TravelerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class ExpenseItemController {
    private final ExpenseItemService expenseItemService;
    private final TravelerService travelerService;
    private final ExpenseBookService expenseBookService;
    private final ItineraryService itineraryService;

    // 지출 내역 추가
    @PostMapping("/{iid}/events/{ieid}/expenses")
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

        ExpenseItemDTO expenseItemDto = ExpenseItemDTO.of(expenseBookId, itineraryEventId, payer, content, expense);

        // 추가
        expenseItemService.addExpense(expenseItemDto);
        return ResponseEntity.ok().build();
    }


//    @PostMapping("/{iid}/events/{ieid}/expenses")
//    public List<WithWhomDTO> addWithWhom(@PathVariable Integer iid, @PathVariable Integer ieid, @RequestBody ExpenseItemRequest expenseItemRequest) {) {
//
//        List<String> withWhomNames = expenseItemRequest.getWithWhom(); // ["GAYEON", "NAYEON"]
//
//        // WithWhomNames
//        List<TravelerDTO> travelerDtos = withWhomNames.stream()
//                .map(travelerService::get)
//                .collect(Collectors.toList());
//
//        expenseItemService.addExpense(expenseItemDto, withWhomDtos);
//        return null;
//    }






    }
