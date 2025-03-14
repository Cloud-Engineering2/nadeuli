/* ItineraryRegionDTO.java
 * ItineraryRegionDTO 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-05
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.14    위도경도 추가
 *
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ItineraryRegion;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryRegionDTO {

    private Long itineraryId;
    private Long regionId;
    private String regionName;
    private Double latitude;
    private Double longitude;

    // Entity → DTO 변환
    public static ItineraryRegionDTO from(ItineraryRegion entity) {
        return new ItineraryRegionDTO(
                entity.getItinerary().getId(),
                entity.getRegion().getId(),
                entity.getRegion().getName(),
                entity.getRegion().getLatitude(),
                entity.getRegion().getLongitude()
        );
    }
}