/* WithWhomController.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.27   Controller 생성
 *
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.service.WithWhomService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/itineraries")
@RequiredArgsConstructor
public class WithWhomController {

    private final WithWhomService withWhomService;

//    @PostMapping("/{iid}/events/{ieid}/expenses")
//    public List<WithWhomDTO> addWithWhom(@PathVariable Integer iid, @PathVariable Integer ieid, @RequestBody ExpenseItemRequest expenseItemRequest) {
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