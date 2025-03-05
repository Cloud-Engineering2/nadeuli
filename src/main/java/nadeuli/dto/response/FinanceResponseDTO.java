/* FinanceResponseDTO.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.03.04   ResDTO 생성
 *
 * ========================================================
 */

package nadeuli.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.Person;

import java.util.Map;

@Getter
@NoArgsConstructor
public class FinanceResponseDTO {
    Map<String, Person> adjustment;
    private Long totalBudget;
    private Long totalExpenses;
    private Long currentExpense;
    private Long totalBalance;

    public FinanceResponseDTO(Map<String, Person> adjustment, Long totalBudget, Long currentExpense, Long totalExpenses, Long totalBalance) {
        this.adjustment = adjustment;
        this.totalBudget = totalBudget;
        this.currentExpense = currentExpense;
        this.totalExpenses = totalExpenses;
        this.totalBalance = totalBalance;
    }
}
