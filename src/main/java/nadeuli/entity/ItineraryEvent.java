/* ItineraryEvent.java
 * ItineraryEvent 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     생성자 + of() 추가
 * 박한철    2025.02.25     iid->itinerary, pid->place 로 변수명 수정
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "itinerary_event")
public class ItineraryEvent extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ieid", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "iid", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "pid", nullable = false)
    private Place place;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;


    // 생성자
    public ItineraryEvent(Itinerary itinerary, Place place, LocalDateTime startDate, LocalDateTime endDate) {

        // 초기화
        this.itinerary = itinerary;
        this.place = place;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // static factory method
    public static ItineraryEvent of(Itinerary itinerary, Place place, LocalDateTime startDate, LocalDateTime endDate) {
        return new ItineraryEvent(itinerary, place, startDate, endDate);
    }


}