/* PlaceRecommendRequestDto.java
 * nadeuli Service - 나들이 여행 장소 검색(커서 페이징)
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


import lombok.Getter;
import nadeuli.entity.constant.PlaceCategory;

import java.util.List;

@Getter
public class PlaceRecommendRequestDto {
    private double userLng;
    private double userLat;
    private double radius;
    private Double cursorScore; // null 가능
    private Long cursorId;      // null 가능
    private int pageSize = 10;  // 기본값
    private List<String> placeTypes; // 추가된 필드
    private boolean searchEnabled;
    private String searchQuery;
}
