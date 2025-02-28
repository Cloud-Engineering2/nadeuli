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
 * ========================================================
 */

package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryTotalResponseDTO {
    private ItineraryResponseDTO itinerary;
    private List<ItineraryPerDaySimpleDTO> itineraryPerDays;
    private List<ItineraryEventSimpleDTO> itineraryEvents;
}
