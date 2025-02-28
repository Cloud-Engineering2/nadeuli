/* Itinerary.java
 * Itinerary 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     생성자 + of() 추가
 * 이홍비    2025.02.25     Date 쪽 자료형 변경
 * 박한철    2025.02.27     DB 구조 수정 end_date -> totalDays로 카운팅하는식으로 변경
 *                         transportationType 추가
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.ItineraryDTO;


import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "itinerary")
public class Itinerary extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iid", nullable = false)
    private Long id;

    @Column(name = "itinerary_name", nullable = false, length = 50)
    private String itineraryName;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "transportation_type", nullable = false)
    private int transportationType;

    // 생성자

    public Itinerary(String itineraryName, LocalDateTime startDate, int totalDays, int transportationType) {
        this.itineraryName = itineraryName;
        this.startDate = startDate;
        this.totalDays = totalDays;
        this.transportationType = transportationType;
    }

    // static factory method
    public static Itinerary of(String itineraryName, LocalDateTime startDate, int totalDays, int transportationType) {
        return new Itinerary(itineraryName, startDate, totalDays, transportationType);
    }

    public void updateFromDto(ItineraryDTO dto) {
        this.itineraryName = dto.getItineraryName();
        this.startDate = dto.getStartDate();
        this.totalDays = dto.getTotalDays();
        this.transportationType = dto.getTransportationType();
    }

}