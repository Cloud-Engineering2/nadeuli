/* ItineraryPerDay.java
 * ItineraryPerDay 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-27
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.27     최초 작성 (DB 구조 수정)
 * 박한철    2025.02.28     업데이트용 메소드 updateFromDto 추가
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.ItineraryPerDayDTO;

import java.time.LocalTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "itinerary_per_day")
public class ItineraryPerDay extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ipdid", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iid", nullable = false)
    private Itinerary itinerary;

    @Column(name = "day_count", nullable = false)
    private int dayCount;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    public ItineraryPerDay(Itinerary itinerary, int dayCount, LocalTime startTime, Integer dayOfWeek) {
        this.itinerary = itinerary;
        this.dayCount = dayCount;
        this.startTime = startTime;
        this.dayOfWeek = dayOfWeek;
    }


    public void updateFromDto(ItineraryPerDayDTO dto) {
        this.dayCount = dto.getDayCount();
        this.startTime = dto.getStartTime();
        this.dayOfWeek = dto.getDayOfWeek();
    }

    public static ItineraryPerDay of(Itinerary itinerary, int dayCount, LocalTime startTime, Integer dayOfWeek) {
        return new ItineraryPerDay(itinerary, dayCount, startTime, dayOfWeek);
    }
}