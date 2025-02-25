/* ExpenseItem.java
 * ExpenseItem 엔티티
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
@Table(name = "expense_item")
public class ExpenseItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emid", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ebid", nullable = false)
    private ExpenseBook ebid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ieid", nullable = false)
    private ItineraryEvent ieid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "payer", nullable = false)
    private Traveler payer;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "expense", columnDefinition = "INT UNSIGNED not null")
    private Integer expense;


    // 생성자
    public ExpenseItem(ExpenseBook ebid, ItineraryEvent ieid, Traveler payer, String content, Integer expense) {

        // 초기화
        this.ebid = ebid;
        this.ieid = ieid;
        this.payer = payer;
        this.content = content;
        this.expense = expense;
    }

    // static factory method
    public static ExpenseItem of (ExpenseBook ebid, ItineraryEvent ieid, Traveler payer, String content, Integer expense) {
        return new ExpenseItem(ebid, ieid, payer, content, expense);
    }

}