/* ExpenseItemRequest.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-02-26
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.26   ReqDto 생성
 *
 * ========================================================
 */

package nadeuli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class ExpenseItemRequest {
    @NotNull(message = "지출 내용을 입력하세요")
    private String content;

    @NotNull
    private String payer;

    @NotBlank
    private Integer expense;

    private List<String> withWhom;

}