/* Place.java
 * Place 엔티티
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

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "place")
public class Place extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pid", nullable = false)
    private Long id;

    @Column(name = "google_place_id", nullable = false)
    private String googlePlaceId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    // 생성자
    public Place(String googlePlaceId, String placeName) {

        // 초기화
        this.googlePlaceId = googlePlaceId;
        this.placeName = placeName;
    }

    // static factory method
    public static Place of(String googlePlaceId, String placeName) {
        return new Place(googlePlaceId, placeName);
    }

}