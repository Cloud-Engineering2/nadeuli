/*
 * ItineraryWithOwnerDTO.java
 * 공유하기 정보에 오너 이름을 포함한 DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성
 * ========================================================
 */
package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nadeuli.dto.ItineraryDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryWithOwnerDTO {
    private ItineraryDTO itinerary;
    private String ownerName;

    public static ItineraryWithOwnerDTO of(ItineraryDTO itineraryDTO, String ownerName) {
        return new ItineraryWithOwnerDTO(itineraryDTO, ownerName);
    }
}