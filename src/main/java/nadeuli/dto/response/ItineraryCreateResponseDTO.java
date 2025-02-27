/* ItineraryCreateResponseDTO.java
 * 일정표를 생성하고나서의 ResponseDTO
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

package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryPerDay;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryCreateResponseDTO {
    private ItineraryDTO itinerary;
    private List<ItineraryPerDaySimpleDTO> itineraryPerDays;



    public static ItineraryCreateResponseDTO from(Itinerary itinerary, List<ItineraryPerDay> itineraryPerDayList) {
        return new ItineraryCreateResponseDTO(
                ItineraryDTO.from(itinerary),
                itineraryPerDayList.stream().map(ItineraryPerDaySimpleDTO::from).collect(Collectors.toList())
        );
    }
}
