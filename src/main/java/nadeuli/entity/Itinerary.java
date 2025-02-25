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
 *
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.Instant;

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
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;


    // 생성자
    public Itinerary(String itineraryName, Instant startDate, Instant endDate) {

        // 초기화
        this.itineraryName = itineraryName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // static factory method
    public static Itinerary of (String itineraryName, Instant startDate, Instant endDate) {
        return new Itinerary(itineraryName, startDate, endDate);
    }


}