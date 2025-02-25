/* ItineraryResponseDTO.java
 * 수정시간, 생성시간을 포함한 일정을 리턴하기 위한 Response DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     최초 작성
 *
 * ========================================================
 */
package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Itinerary;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryResponseDTO {
    private Long id;
    private String itineraryName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    // entity -> response dto 변환
    public static ItineraryResponseDTO from(Itinerary itinerary) {
        return new ItineraryResponseDTO(
                itinerary.getId(),
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                itinerary.getCreatedDate(),
                itinerary.getModifiedDate()
        );
    }
}
