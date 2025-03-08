/* ItineraryPerDayDTO.java
 * ItineraryPerDayDTO 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.27     최초 작성 (DB 구조 수정)
 * 박한철    2025.02.27     toEntity(itinerary) 추가
 * 박한철    2025.02.28     @JsonIgnore로 직렬화시 중첩된 데이터 출력되지 않게 적용
 * ========================================================
 */
package nadeuli.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryPerDay;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryPerDayDTO {
    private Long id;
    @JsonIgnore
    private ItineraryDTO itineraryDTO;
    private int dayCount;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer dayOfWeek;

    // entity -> dto
    public static ItineraryPerDayDTO from(ItineraryPerDay itineraryPerDay) {
        return new ItineraryPerDayDTO(
                itineraryPerDay.getId(),
                ItineraryDTO.from(itineraryPerDay.getItinerary()),
                itineraryPerDay.getDayCount(),
                itineraryPerDay.getStartTime(),
                itineraryPerDay.getEndTime(),
                itineraryPerDay.getDayOfWeek()
        );
    }

    // dto -> entity
    public ItineraryPerDay toEntity() {
        return new ItineraryPerDay(id, itineraryDTO.toEntity(), dayCount, startTime, endTime, dayOfWeek);
    }

    public ItineraryPerDay toEntity(Itinerary itinerary) {
        return new ItineraryPerDay(id, itinerary, dayCount, startTime, endTime, dayOfWeek);
    }

}
