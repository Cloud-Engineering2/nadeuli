/* EventExpenseSummaryTotalResponseDTO.java
 * 일정 이벤트별 경비 합계 리스트 응답 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-17
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */

package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventExpenseSummaryTotalResponseDTO {
    private List<EventExpenseSummaryDTO> summaries;
    private Long totalExpenses;
    private Long totalBudget;
}
