/* Region.java
 * region 관련
 * 작성자 : 박한철
 * 최종 수정 날짜 : 2025.03.05
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.05     최초 작성
 * 박한철    2025.03.12     description -> explanation
 * ========================================================
 */

package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "region")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "alias", length = 50)
    private String alias;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Region parent;  // 부모 지역 (시도 -> NULL, 구군 -> 시도 ID)

    @Column(name = "level", nullable = false)
    private int level; // 1: 시·도, 2: 시·군·구

    // 위도, 경도, 범위(radius)를 위한 컬럼 추가
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "radius")
    private Double radius;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(name = "explanation", length = 300)
    private String explanation;

    /**
     * 기존에 사용하던 생성자(위도, 경도, radius가 필요 없는 경우).
     * 필요시 남겨두거나, 오버로드된 생성자를 사용하는 식으로 구성할 수 있습니다.
     */
    public Region(String name, String alias, int level, Region parent) {
        this.name = name;
        this.alias = alias;
        this.level = level;
        this.parent = parent;
    }

    /**
     * 새로 추가한 생성자: 위도, 경도, radius까지 포함
     */
    public Region(String name, String alias, int level, Region parent,
                  Double latitude, Double longitude, Double radius, String imageUrl, String explanation) {
        this.name = name;
        this.alias = alias;
        this.level = level;
        this.parent = parent;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.imageUrl = imageUrl;
        this.explanation = explanation;
    }

    // 정적 팩토리 메서드 (위도/경도/반경 미포함)
    public static Region of(String name, String alias, int level, Region parent) {
        return new Region(name, alias, level, parent);
    }

    // 정적 팩토리 메서드 (위도/경도/반경 포함)
    public static Region of(String name, String alias, int level, Region parent,
                            Double latitude, Double longitude, Double radius, String imageUrl, String explanation) {
        return new Region(name, alias, level, parent, latitude, longitude, radius, imageUrl, explanation);
    }

    public void setImageUrl(String newImageUrl) {
        this.imageUrl = newImageUrl;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

}
