/* Journal.java
 * Journal 엔티티
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     생성자 + of() 추가
 * 이홍비    2025.02.25     columnDefinition = "TEXT" 명시
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
@Table(name = "journal")
public class Journal extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jid", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ieid", nullable = false)
    private ItineraryEvent ieid;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    // 생성자
    public Journal(ItineraryEvent ieid, String content, String imageUrl) {

        // 초기화
        this.ieid = ieid;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    // static factory method
    public static Journal of(ItineraryEvent ieid, String content, String imageUrl) {
        return new Journal(ieid, content, imageUrl);
    }

}