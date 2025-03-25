/* ExpenseItemUpdateRequestDTO.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.27   ReqDto 생성
 *
 * ========================================================
 */

package nadeuli.dto.request;

import lombok.*;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class ExpenseItemUpdateRequestDTO {
    private String content;
    private String payer;
    private Integer expense;

}