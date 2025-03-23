/* AdminRegionUpdateRequestDTO.java
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-22
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.22   위도경도반경 수정용 DTO
 *
 * ========================================================
 */
package nadeuli.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AdminRegionUpdateRequestDTO {

    private Long id;            // 수정 대상 Region ID
    private Double latitude;    // 수정할 위도
    private Double longitude;   // 수정할 경도
    private Double radius;      // 수정할 반경

}