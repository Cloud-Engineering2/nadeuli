/* RegionDTO.java
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
 *
 * ========================================================
 */


package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Region;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RegionDTO {

    private Long id;
    private String name;
    private String alias;
    private Long parentId;   // 부모 지역 ID (없으면 NULL)
    private String parentName; // 부모 지역명 (없으면 NULL)
    private int level;       // 1: 시·도, 2: 시·군·구

    // 새로 추가한 필드
    private Double latitude;
    private Double longitude;
    private Double radius;

    // 모든 필드 포함하는 정적 팩토리 메서드
    public static RegionDTO of(Long id,
                               String name,
                               String alias,
                               Long parentId,
                               String parentName,
                               int level,
                               Double latitude,
                               Double longitude,
                               Double radius) {
        return new RegionDTO(id, name, alias, parentId, parentName, level,
                latitude, longitude, radius);
    }

    // ID 없이 만드는 경우
    public static RegionDTO of(String name,
                               String alias,
                               Long parentId,
                               String parentName,
                               int level,
                               Double latitude,
                               Double longitude,
                               Double radius) {
        return new RegionDTO(null, name, alias, parentId, parentName, level,
                latitude, longitude, radius);
    }

    // entity -> dto 변환
    public static RegionDTO from(Region region) {
        if (region == null) return null;

        return RegionDTO.of(
                region.getId(),
                region.getName(),
                region.getAlias(),
                region.getParent() != null ? region.getParent().getId() : null,
                region.getParent() != null ? region.getParent().getName() : null,
                region.getLevel(),
                region.getLatitude(),
                region.getLongitude(),
                region.getRadius()
        );
    }

    // dto -> entity 변환
    public Region toEntity(Region parent) {
        // 새로운 정적 팩토리 메서드 사용
        return Region.of(
                name,
                alias,
                level,
                parent,
                latitude,
                longitude,
                radius
        );
    }
}
