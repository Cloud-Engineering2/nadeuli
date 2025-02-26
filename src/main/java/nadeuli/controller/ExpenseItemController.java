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
import nadeuli.service.ExpenseItemService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/itineraries", produces = "application/json")
@RequiredArgsConstructor
public class ExpenseItemController {
    private final ExpenseItemService expenseItemService;

}
