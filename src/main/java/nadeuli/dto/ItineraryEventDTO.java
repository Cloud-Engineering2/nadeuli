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
 * 박한철    2025.02.28     @JsonIgnore로 직렬화시 중첩된 데이터 출력되지 않게 적용
 * ========================================================
 */

package nadeuli.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ItineraryEvent;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryEventDTO {
    private Long id;
    @JsonIgnore
    private ItineraryPerDayDTO itineraryPerDayDTO;
    private PlaceDTO placeDTO;
    private int startMinuteSinceStartDay;
    private int endMinuteSinceStartDay;
    private int movingMinuteFromPrevPlace;


    // static factory method
    public static ItineraryEventDTO of(Long id, ItineraryPerDayDTO itineraryPerDayDTO, PlaceDTO placeDTO, int startMinuteSinceStartDay, int endMinuteSinceStartDay, int movingMinuteFromPrevPlace) {
        return new ItineraryEventDTO(id, itineraryPerDayDTO, placeDTO, startMinuteSinceStartDay, endMinuteSinceStartDay, movingMinuteFromPrevPlace);
    }

    public static ItineraryEventDTO of(ItineraryPerDayDTO itineraryPerDayDTO, PlaceDTO placeDTO, int startMinuteSinceStartDay, int endMinuteSinceStartDay, int movingMinuteFromPrevPlace) {
        return new ItineraryEventDTO(null, itineraryPerDayDTO, placeDTO, startMinuteSinceStartDay, endMinuteSinceStartDay, movingMinuteFromPrevPlace);
    }

    // entity -> dto
    public static ItineraryEventDTO from(ItineraryEvent itineraryEvent) {
        return new ItineraryEventDTO(
                itineraryEvent.getId(),
                ItineraryPerDayDTO.from(itineraryEvent.getItineraryPerDay()),
                PlaceDTO.from(itineraryEvent.getPlace()),
                itineraryEvent.getStartMinuteSinceStartDay(),
                itineraryEvent.getEndMinuteSinceStartDay(),
                itineraryEvent.getMovingMinuteFromPrevPlace()
        );
    }

    // dto => entity
    public ItineraryEvent toEntity() {
        return ItineraryEvent.of(
                itineraryPerDayDTO.toEntity(),
                placeDTO.toEntity(),
                startMinuteSinceStartDay,
                endMinuteSinceStartDay,
                movingMinuteFromPrevPlace
        );
    }
}
