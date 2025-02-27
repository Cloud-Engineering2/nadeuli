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
 * 박한철    2025.02.27     DB 구조 수정 end_date -> totalDays로 카운팅하는식으로 변경
 *                         transportationType 추가
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
    private int totalDays;
    private int transportationType;


    // static factory method
    public static ItineraryDTO of(Long id, String itineraryName, LocalDateTime startDate, int totalDays, int transportationType) {
        return new ItineraryDTO(id, itineraryName, startDate, totalDays, transportationType);
    }

    public static ItineraryDTO of(String itineraryName, LocalDateTime startDate, int totalDays, int transportationType) {
        return new ItineraryDTO(null, itineraryName, startDate, totalDays, transportationType);
    }

    // entity -> dto
    public static ItineraryDTO from(Itinerary itinerary) {
        return new ItineraryDTO(itinerary.getId(), itinerary.getItineraryName(), itinerary.getStartDate(), itinerary.getTotalDays(), itinerary.getTransportationType());
    }


    // dto => entity
    public Itinerary toEntity() {
        return Itinerary.of(itineraryName, startDate, totalDays, transportationType);
    }
}
