/* ItineraryTotalUpdateRequestDTO.java
 * Update에 필요한 데이터를 담기위한 Request DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.28
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.28     최초 작성
 * ========================================================
 */

package nadeuli.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryPerDayDTO;

import java.util.List;
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryTotalUpdateRequestDTO {

    private ItineraryDTO itinerary;
    private List<ItineraryPerDayDTO> itineraryPerDays;
    private List<ItineraryEventUpdateDTO> itineraryEvents;


}
