/* TravelerNameRequestDTO.java
 * 작성자 : 고민정
 * 최초 작성 날짜 : 2025-03-24
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.03.24   Req DTO 생성
 *
 * ========================================================
 */

package nadeuli.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class TravelerNameRequestDTO {
    @NotBlank(message = "여행자 이름은 필수입니다.")
    private String name;
}


