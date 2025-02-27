/* ItineraryEventSimpleDTO.java
 * ItineraryTotalResponseDTO(Event를 포함한 전체 일정표를 리턴하기 위한 Response DTO)에서 사용되는
 * ItineraryEvent 내에 중첩되는 ItineraryDto 를 제거한 버전의 DTO
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
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.PlaceDTO;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.ItineraryPerDay;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryPerDaySimpleDTO {
    private Long id;
    private int dayCount;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer dayOfWeek;

    // entity -> dto
    public static ItineraryPerDaySimpleDTO from(ItineraryPerDay itineraryPerDay) {
        return new ItineraryPerDaySimpleDTO(
                itineraryPerDay.getId(),
                itineraryPerDay.getDayCount(),
                itineraryPerDay.getStartTime(),
                itineraryPerDay.getEndTime(),
                itineraryPerDay.getDayOfWeek()
        );
    }
}