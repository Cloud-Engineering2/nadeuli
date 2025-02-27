/* ExpenseBookDTO.java
 * nadeuli Service - 여행
 * ExpenseBook 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * 고민정    2025;02.25     필드 수정
 *
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ExpenseBook;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseBookDTO {
    private Long id;
    private ItineraryDTO itineraryDTO;
    private Long totalBudget;
    private Long totalExpenses;


    // static factory method
    public static ExpenseBookDTO of (Long id, ItineraryDTO itineraryDTO, Long totalBudget, Long totalExpenses) {
        return new ExpenseBookDTO(id, itineraryDTO, totalBudget, totalExpenses);
    }

    public static ExpenseBookDTO of (ItineraryDTO itineraryDTO, Long totalBudget, Long totalExpenses) {
        return new ExpenseBookDTO(null, itineraryDTO, totalBudget, totalExpenses);
    }

    // entity -> dto
    public static ExpenseBookDTO from(ExpenseBook expenseBook) {
        return new ExpenseBookDTO(
                expenseBook.getId(),
                ItineraryDTO.from(expenseBook.getIid()),
                expenseBook.getTotalBudget(),
                expenseBook.getTotalExpenses()
        );
    }

    // dto => entity
    public ExpenseBook toEntity() {
        return ExpenseBook.of(itineraryDTO.toEntity(), totalBudget, totalExpenses);
    }
}
