/* Travler.java
 * Travler 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     생성자 + of() 추가
 * 고민정    2025.02.25     생성자 접근수준 수정
 * 고민정    2025.03.11     total_budget, expense 필드 추가, budget update 메서드 추가
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "traveler")
public class Traveler {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tid", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "iid", nullable = false)
    private Itinerary iid;

    @Column(name = "traveler_name", length = 20)
    private String travelerName;

    @Column(name = "total_budget", nullable = false)
    @Min(0)
    private Long totalBudget;

    @Column(name = "total_expense", nullable = false)
    private Long totalExpense;

//    // 생성자
//    public Traveler(Itinerary iid, String travelerName) {
//
//        // 초기화
//        this.iid = iid;
//        this.travelerName = travelerName;
//    }

    // static factory method
    public static Traveler of(Itinerary iid, String travelerName, Long totalBudget) {
        return new Traveler(null, iid, travelerName, totalBudget, 0L);
    }

    public void updateTotalExpense(Long totalExpense) {
        this.totalExpense = totalExpense;
    }

    public void updateTotalBudget(Long totalBudget) {
        this.totalBudget = totalBudget;
    }

}