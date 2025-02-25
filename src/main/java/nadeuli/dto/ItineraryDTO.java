/* ItineraryDTO.java
 * nadeuli Service - 여행
 * Itinerary 관련 DTO
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
import nadeuli.entity.Itinerary;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryDTO {
    private Long id;
    private String itineraryName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;


    // static factory method
    public static ItineraryDTO of (Long id, String itineraryName, LocalDateTime startDate, LocalDateTime endDate) {
        return new ItineraryDTO(id, itineraryName, startDate, endDate);
    }

    public static ItineraryDTO of (String itineraryName, LocalDateTime startDate, LocalDateTime endDate) {
        return new ItineraryDTO(null, itineraryName, startDate, endDate);
    }

    // entity -> dto
    public static ItineraryDTO from(Itinerary Itinerary) {
        return new ItineraryDTO(
                Itinerary.getId(),
                Itinerary.getItineraryName(),
                Itinerary.getStartDate(),
                Itinerary.getEndDate()
        );
    }

    // dto => entity
    public Itinerary toEntity() {
        return Itinerary.of(itineraryName, startDate, endDate);
    }
}
