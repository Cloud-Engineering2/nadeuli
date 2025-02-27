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
 * 박한철    2025.02.25     uid->user, iid->itinerary로 변수명 수정
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
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "iid", nullable = false)
    private Itinerary itinerary;

    @Column(name = "ic_role", nullable = false, length = 20)
    private String icRole;


    // 생성자
    public ItineraryCollaborator(User user, Itinerary itinerary) {

        // 초기화
        this.user = user;
        this.itinerary = itinerary;
        this.icRole = "ROLE_OWNER"; // default => ROLE_OWNER
    }

    // static factory method
    public static ItineraryCollaborator of (User user, Itinerary itinerary) {
        return new ItineraryCollaborator(user, itinerary);
    }
}