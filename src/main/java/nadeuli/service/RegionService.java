package nadeuli.service;
import lombok.RequiredArgsConstructor;
import nadeuli.dto.RegionTreeDTO;
import nadeuli.entity.Region;
import nadeuli.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    // 모든 지역 조회
    public List<Region> getAllRegions() {
        return regionRepository.findAll();
    }

    // 특정 ID로 지역 조회
    public Optional<Region> getRegionById(Long rid) {
        return regionRepository.findById(rid);
    }

    // 특정 레벨(시·도 or 시·군·구) 지역 조회
    public List<Region> getRegionsByLevel(int level) {
        return regionRepository.findByLevel(level);
    }

    // 특정 상위 지역(시·도)에 속하는 시·군·구 조회
    public List<Region> getSubRegions(Long parentId) {
        Optional<Region> parentRegion = regionRepository.findById(parentId);
        return parentRegion.map(regionRepository::findByParent)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 ID"));
    }

    // 새로운 지역 추가
    @Transactional
    public Region addRegion(String regionName, int level, Long parentId) {
        Region parent = (parentId != null) ? regionRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 지역 ID")) : null;

        Region newRegion = Region.of(regionName, level, parent);
        return regionRepository.save(newRegion);
    }

    // 모든 지역을 가져와 트리 구조로 변환
    public List<RegionTreeDTO> getRegionTree() {
        List<Region> allRegions = regionRepository.findAll();

        // 최상위 지역 (시·도)만 선택하여 트리 변환
        return allRegions.stream()
                .filter(region -> region.getParent() == null) // 최상위 지역 (시·도)
                .map(region -> RegionTreeDTO.from(region, allRegions))
                .collect(Collectors.toList());
    }
}
