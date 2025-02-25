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
 * ========================================================
 */

package nadeuli.dto;

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
    private ItineraryDTO itineraryDTO;
    private PlaceDTO placeDTO;
    private LocalDateTime startDate;
    private LocalDateTime endDate;


    // static factory method
    public static ItineraryEventDTO of (Long id,ItineraryDTO itineraryDTO, PlaceDTO placeDTO, LocalDateTime startDate, LocalDateTime endDate) {
        return new ItineraryEventDTO(id, itineraryDTO, placeDTO, startDate, endDate);
    }

    public static ItineraryEventDTO of (ItineraryDTO itineraryDTO, PlaceDTO placeDTO, LocalDateTime startDate, LocalDateTime endDate) {
        return new ItineraryEventDTO(null, itineraryDTO, placeDTO, startDate, endDate);
    }

    // entity -> dto
    public static ItineraryEventDTO from(ItineraryEvent itineraryEvent) {
        return new ItineraryEventDTO(
                itineraryEvent.getId(),
                ItineraryDTO.from(itineraryEvent.getIid()),
                PlaceDTO.from(itineraryEvent.getPid()),
                itineraryEvent.getStartDate(),
                itineraryEvent.getEndDate()
        );
    }

    // dto => entity
    public ItineraryEvent toEntity() {
        return ItineraryEvent.of(itineraryDTO.toEntity(), placeDTO.toEntity(), startDate, endDate);
    }
}
