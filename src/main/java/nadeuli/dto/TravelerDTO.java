/* TravelerDTO.java
 * nadeuli Service - 여행
 * Traveler 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * 고민정    2025;02.25     필드 수정
 *
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Itinerary;
import nadeuli.entity.Traveler;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TravelerDTO {
    private Integer id;
    private Long itineraryId;
    private String travelerName;

    // static factory method
    public static TravelerDTO of (Integer id, Long itineraryId, String travelerName) {
        return new TravelerDTO(id, itineraryId, travelerName);
    }

    public static TravelerDTO of (Long itineraryId, String travelerName) {
        return new TravelerDTO(null, itineraryId, travelerName);
    }

    // entity -> dto
    public static TravelerDTO from(Traveler traveler) {
        return new TravelerDTO(
                traveler.getId(),
                traveler.getIid().getId(),
                traveler.getTravelerName()
        );
    }

    // dto => entity
    public Traveler toEntity(Itinerary itinerary) {
        return Traveler.of(itinerary, travelerName);
    }

}
