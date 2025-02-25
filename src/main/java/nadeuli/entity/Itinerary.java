package nadeuli.entity;
/* Itinerary.java
 * Itinerary 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */
import jakarta.persistence.*;
import lombok.Getter;


import java.time.Instant;

@Getter
@Entity
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

}