/* ItineraryTotalResponseDTO.java
 * Event를 포함한 전체 일정표를 리턴하기 위한 Response DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     최초 작성
 * 박한철    2025.02.27     itineraryPerDay -> itineraryPerDays 변수명 수정
 * 박한철    2025.02.28     ItineraryTotalResponseDTO -> ItineraryTotalReadResponseDTO 변수명 수정
 * 박한철    2025.02.28     @JsonIgnore사용으로 중첩데이터 제거하여 SimpleDTO 대신 일반 DTO 사용가능
 * 이홍비    2025.03.10     @ToString 추가
 * ========================================================
 */

package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.ItineraryRegionDTO;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryTotalReadResponseDTO {
    private ItineraryResponseDTO itinerary;
    private List<ItineraryPerDayDTO> itineraryPerDays;
    private List<ItineraryEventSimpleDTO> itineraryEvents;
    private List<ItineraryRegionDTO> regions;
}
