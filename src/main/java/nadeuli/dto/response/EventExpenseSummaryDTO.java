/* EventExpenseSummaryDTO.java
 * 일정 이벤트별 경비 합계 정보 DTO
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

@Data
@AllArgsConstructor
public class EventExpenseSummaryDTO {
    private Long eventId;
    private Long totalExpense;
}