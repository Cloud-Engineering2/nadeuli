package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Region;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RegionTreeDTO {

    private Long id;
    private String name;
    private int level;
    private List<RegionTreeDTO> children; // 하위 지역 리스트

    // entity -> dto 변환 (트리 구조로 변환)
    public static RegionTreeDTO from(Region region, List<Region> allRegions) {
        List<RegionTreeDTO> children = allRegions.stream()
                .filter(r -> r.getParent() != null && r.getParent().getId().equals(region.getId()))
                .map(r -> from(r, allRegions)) // 재귀적으로 트리 변환
                .collect(Collectors.toList());

        return new RegionTreeDTO(region.getId(), region.getName(), region.getLevel(), children);
    }
}
