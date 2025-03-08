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
 * 박한철    2025.02.27     DB 구조 수정 iid -> ipdid ,  *_date -> *_minute_since_start_day
 *                         moving_minute_from_prev_place 추가
 * 박한철    2025.02.28     업데이트용 메소드 updateFromDto 추가
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.dto.request.ItineraryEventUpdateDTO;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @JoinColumn(name = "ipdid", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ItineraryPerDay itineraryPerDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "pid", nullable = false)
    private Place place;

    @Column(name = "start_minute_since_start_day", nullable = false)
    private int startMinuteSinceStartDay; // 하루 시작 기준 몇 분 후 시작

    @Column(name = "end_minute_since_start_day", nullable = false)
    private int endMinuteSinceStartDay; // 하루 시작 기준 몇 분 후 종료

    @Column(name = "moving_minute_from_prev_place", nullable = false)
    private int movingMinuteFromPrevPlace; // 이전 장소에서 이동 시간 (분)

    // 생성자
    public ItineraryEvent(ItineraryPerDay itineraryPerDay, Place place, int startMinuteSinceStartDay, int endMinuteSinceStartDay, int movingMinuteFromPrevPlace) {
        this.itineraryPerDay = itineraryPerDay;
        this.place = place;
        this.startMinuteSinceStartDay = startMinuteSinceStartDay;
        this.endMinuteSinceStartDay = endMinuteSinceStartDay;
        this.movingMinuteFromPrevPlace = movingMinuteFromPrevPlace;
    }

    public void updateFromDto(ItineraryEventUpdateDTO dto, ItineraryPerDay itineraryPerDay) {
        this.itineraryPerDay = itineraryPerDay; // 일정 날짜 업데이트
        this.startMinuteSinceStartDay = dto.getStartMinuteSinceStartDay();
        this.endMinuteSinceStartDay = dto.getEndMinuteSinceStartDay();
        this.movingMinuteFromPrevPlace = dto.getMovingMinuteFromPrevPlace();
    }




    // static factory method
    public static ItineraryEvent of(ItineraryPerDay itineraryPerDay, Place place, int startMinuteSinceStartDay, int endMinuteSinceStartDay, int movingMinuteFromPrevPlace) {
        return new ItineraryEvent(itineraryPerDay, place, startMinuteSinceStartDay, endMinuteSinceStartDay, movingMinuteFromPrevPlace);
    }


}