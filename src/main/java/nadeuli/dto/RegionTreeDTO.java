/* RegionTreeDTO.java
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

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String alias;
    private int level;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<RegionTreeDTO> children; // 하위 지역 리스트

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double latitude;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double longitude;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double radius;

    // entity -> dto 변환 (트리 구조로 변환)
    public static RegionTreeDTO from(Region region, List<Region> allRegions) {
        List<RegionTreeDTO> children = allRegions.stream()
                .filter(r -> r.getParent() != null && r.getParent().getId().equals(region.getId()))
                .map(r -> from(r, allRegions)) // 재귀적으로 트리 변환
                .collect(Collectors.toList());

        return new RegionTreeDTO(region.getId(), region.getName(),region.getAlias(), region.getLevel(), children, region.getLatitude(), region.getLongitude(), region.getRadius());
    }
}
