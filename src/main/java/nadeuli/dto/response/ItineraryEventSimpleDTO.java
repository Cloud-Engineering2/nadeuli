/* ItineraryEvent.java
 * nadeuli Service - 여행
 * ItineraryEvent 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * 박한철    2025.02.25     엔티티의 변수명이 변경되어 getItinerary(),getPlace()로 getter 명 수정
 * 박한철    2025.02.27     DB 구조 수정 ItineraryDTO -> ItineraryPerDayDTO ,  *_date -> *_minute_since_start_day 수정
 *                         moving_minute_from_prev_place 추가
 * 박한철    2025.02.28     @JsonIgnore로 직렬화 시 중첩된 데이터 출력되지 않게 적용
 * 이홍비    2025.03.10     @ToString 추가
 * ========================================================
 */

package nadeuli.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nadeuli.dto.ItineraryPerDayDTO;
import nadeuli.dto.PlaceDTO;
import nadeuli.entity.ItineraryEvent;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryEventSimpleDTO {
    private Long id;
    private int dayCount;
    private PlaceDTO placeDTO;
    private int startMinuteSinceStartDay;
    private int endMinuteSinceStartDay;
    private int movingMinuteFromPrevPlace;


    // static factory method
    public static ItineraryEventSimpleDTO of(Long id, ItineraryPerDayDTO itineraryPerDayDTO, PlaceDTO placeDTO, int startMinuteSinceStartDay, int endMinuteSinceStartDay, int movingMinuteFromPrevPlace) {
        return new ItineraryEventSimpleDTO(id, itineraryPerDayDTO.getDayCount(), placeDTO, startMinuteSinceStartDay, endMinuteSinceStartDay, movingMinuteFromPrevPlace);
    }

    public static ItineraryEventSimpleDTO of(ItineraryPerDayDTO itineraryPerDayDTO, PlaceDTO placeDTO, int startMinuteSinceStartDay, int endMinuteSinceStartDay, int movingMinuteFromPrevPlace) {
        return new ItineraryEventSimpleDTO(null, itineraryPerDayDTO.getDayCount(), placeDTO, startMinuteSinceStartDay, endMinuteSinceStartDay, movingMinuteFromPrevPlace);
    }

    // entity -> dto
    public static ItineraryEventSimpleDTO from(ItineraryEvent itineraryEvent) {
        return new ItineraryEventSimpleDTO(
                itineraryEvent.getId(),
                ItineraryPerDayDTO.from(itineraryEvent.getItineraryPerDay()).getDayCount(),
                PlaceDTO.from(itineraryEvent.getPlace()),
                itineraryEvent.getStartMinuteSinceStartDay(),
                itineraryEvent.getEndMinuteSinceStartDay(),
                itineraryEvent.getMovingMinuteFromPrevPlace()
        );
    }

}
