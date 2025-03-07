/* RegionService.java
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

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.common.PhotoType;
import nadeuli.dto.RegionTreeDTO;
import nadeuli.entity.Region;
import nadeuli.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;
    private final S3Service s3Service;


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
    public Region addRegion(String regionName, String alias, int level, Long parentId) {
        Region parent = (parentId != null) ? regionRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 지역 ID")) : null;

        Region newRegion = Region.of(regionName, alias, level, parent);
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




    @Transactional
    public String uploadRegionImage(Long regionId, MultipartFile file) {
        // 1. 기존 region 데이터 조회
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 ID입니다."));

        // 2. 기존 이미지 삭제 (기존 URL이 있으면 삭제)
        if (region.getImageUrl() != null && !region.getImageUrl().isEmpty()) {
            s3Service.deleteFile(region.getImageUrl());
        }

        // 3. 새 이미지 S3 업로드
        String newImageUrl = s3Service.uploadFile(file, PhotoType.REGION);

        System.out.println(newImageUrl);

        // 4. DB 업데이트
        region.setImageUrl(newImageUrl);
        regionRepository.save(region);

        return newImageUrl;
    }


}
