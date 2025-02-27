/* ItineraryCreateRequestDTO.java
 * 일정표를 생성하기위한 RequestDTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.27     최초 작성
 *
 * ========================================================
 */

package nadeuli.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.response.ItineraryEventSimpleDTO;
import nadeuli.dto.response.ItineraryPerDaySimpleDTO;
import nadeuli.dto.response.ItineraryResponseDTO;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryCreateRequestDTO {
    private ItineraryDTO itinerary;
    private List<ItineraryPerDayDTO> itineraryPerDays;
}
