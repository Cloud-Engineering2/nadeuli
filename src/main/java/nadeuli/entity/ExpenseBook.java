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
 * 고민정    2025.02.25     생성자 접근수준, Itinerary 다대일->일대일 관계 수정
 * 고민정    2025.02.26     예산 설정 메서드 추가
 * 고민정    2025.03.04     totalExpenses setter 추가
 *
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "expense_book")
public class ExpenseBook extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ebid", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "iid", nullable = false)
    private Itinerary iid;

    @Column(name = "total_budget", columnDefinition = "INT UNSIGNED not null default 0")
    @ColumnDefault("0")
    private Long totalBudget;

    @Column(name = "total_expenses", columnDefinition = "INT UNSIGNED not null default 0")
    @ColumnDefault("0")
    private Long totalExpenses;


//

    // static factory method
    public static ExpenseBook of (Long id, Itinerary iid, Long totalBudget, Long totalExpenses) {
        return new ExpenseBook(id, iid, totalBudget, totalExpenses);
    }

    public static ExpenseBook of (Itinerary iid, Long totalBudget, Long totalExpenses) {
        return new ExpenseBook(null, iid, totalBudget, totalExpenses);
    }

    public void updateBudget(Long budget) {
        this.totalBudget = budget;
    }

    public void updateExpense(Long expense) {
        this.totalExpenses = expense;
    }


}