/* ItineraryCollaboratoer.java
 * ItineraryCollaboratoer 엔티티
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
@Table(name = "itinerary_collaborator")
public class ItineraryCollaborator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "icid", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uid", nullable = false)
    private User uid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "iid", nullable = false)
    private Itinerary iid;

    @Column(name = "ic_role", nullable = false, length = 20)
    private String icRole;


    // 생성자
    public ItineraryCollaborator(User uid, Itinerary iid) {

        // 초기화
        this.uid = uid;
        this.iid = iid;
        this.icRole = "ROLE_OWNER"; // default => ROLE_OWNER
    }

    // static factory method
    public static ItineraryCollaborator of (User uid, Itinerary iid) {
        return new ItineraryCollaborator(uid, iid);
    }
}