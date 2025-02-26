/* OpenAITravelResponse.java
 * OPEN API 연동
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-21
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   Req DTO 생성
 *
 * ========================================================
 */

package nadeuli.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class BudgetReq {
    @NotNull(message = "남은 예산 : ")
    private Integer totalBudget;
}


