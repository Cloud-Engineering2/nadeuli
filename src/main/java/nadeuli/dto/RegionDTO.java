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
    private Long parentId; // 부모 지역 ID (없으면 NULL)
    private String parentName; // 부모 지역명 (없으면 NULL)
    private int level; // 1: 시·도, 2: 시·군·구

    // static factory method
    public static RegionDTO of(Long id, String name, Long parentId, String parentName, int level) {
        return new RegionDTO(id, name, parentId, parentName, level);
    }

    public static RegionDTO of(String name, Long parentId, String parentName, int level) {
        return new RegionDTO(null, name, parentId, parentName, level);
    }

    // entity -> dto 변환
    public static RegionDTO from(Region region) {
        return new RegionDTO(
                region.getId(),
                region.getName(),
                region.getParent() != null ? region.getParent().getId() : null,
                region.getParent() != null ? region.getParent().getName() : null,
                region.getLevel() // level 추가
        );
    }

    // dto -> entity 변환
    public Region toEntity(Region parent) {
        return Region.of(name, level, parent); // level 포함
    }
}
