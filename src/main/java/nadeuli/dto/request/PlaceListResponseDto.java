/* PlaceRecommendRequestDto.java
 * 구글 장소 검색(커서 페이징 포함)
 * Place 관련 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.09     최초작성
 * ========================================================
 */
package nadeuli.dto.request;

import lombok.Builder;
import lombok.Getter;
import nadeuli.dto.response.PlaceResponseDto;

import java.util.List;

@Getter
@Builder
public class PlaceListResponseDto {
    private List<PlaceResponseDto> places;
    private Double nextCursorScore;
    private Long nextCursorId;
}
