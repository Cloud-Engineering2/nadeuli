/* WithWhom.java
 * WithWhom 엔티티
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "with_whom")
public class WithWhom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wid", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "emid", nullable = false)
    private ExpenseItem emid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "tid", nullable = false)
    private Traveler tid;

    // 생성자
    public WithWhom(ExpenseItem emid, Traveler tid) {

        // 초기화
        this.emid = emid;
        this.tid = tid;
    }

    // static factory method
    public static WithWhom of(ExpenseItem emid, Traveler tid) {
        return new WithWhom(emid, tid);
    }

}