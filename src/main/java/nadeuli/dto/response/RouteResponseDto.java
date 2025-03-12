/* RouteResponseDto.java
 * 카카오 경로 시간 검색 Response DTO
 * Place 관련 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.03. 11
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.11     최초작성
 * ========================================================
 */
package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RouteResponseDto {
    private int distanceMeters;
    private int duration;
}
