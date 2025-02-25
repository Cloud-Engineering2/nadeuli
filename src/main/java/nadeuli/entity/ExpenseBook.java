/* ExpenseBook.java
 * ExpenseBook 엔티티
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "expense_book")
public class ExpenseBook extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ebid", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "iid", nullable = false)
    private Itinerary iid;

    @Column(name = "total_budget", columnDefinition = "INT UNSIGNED not null default 0")
    @ColumnDefault("0")
    private Integer totalBudget;

    @Column(name = "total_expenses", columnDefinition = "INT UNSIGNED not null default 0")
    @ColumnDefault("0")
    private Integer totalExpenses;


    // 생성자
    public ExpenseBook(Itinerary iid, Integer totalBudget, Integer totalExpenses) {

        // 초기화
        this.iid = iid;
        this.totalBudget = totalBudget;
        this.totalExpenses = totalExpenses;
    }

    // static factory method
    public static ExpenseBook of (Itinerary iid, Integer totalBudget, Integer totalExpenses) {
        return new ExpenseBook(iid, totalBudget, totalExpenses);
    }


}