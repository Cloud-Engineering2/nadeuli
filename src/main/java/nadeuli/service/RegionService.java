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
import nadeuli.dto.response.RegionImageResponseDTO;
import nadeuli.entity.Region;
import nadeuli.repository.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RegionService {

    private final RegionRepository regionRepository;
    private final S3Service s3Service;
    @Qualifier("regionImageRedisTemplate")
    private final RedisTemplate<String, Map<String, String>> regionImageRedisTemplate;

    public RegionService(RegionRepository regionRepository, S3Service s3Service, RedisTemplate<String, Map<String, String>> regionImageRedisTemplate) {
        this.regionRepository = regionRepository;
        this.s3Service = s3Service;
        this.regionImageRedisTemplate = regionImageRedisTemplate;
    }

    private static final String REGION_IMAGE_CACHE_KEY = "region:imageUrls";
    private static final String DEFAULT_IMAGE_URL = "https://picsum.photos/600/600";
    private static final Duration REGION_IMAGE_CACHE_TTL = Duration.ofHours(6);
    private static final boolean IS_CACHING_ENABLED = true;




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



    //지역 이미지 리스트 가져오기 (DTO 변환)
    public List<RegionImageResponseDTO> getRegionImageDtoListWithCache() {
        Map<Long, String> imageMap = getRegionImageUrlsWithCache();
        return imageMap.entrySet().stream()
                .map(entry -> new RegionImageResponseDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    //지역 이미지 리스트 가져오기
    public Map<Long, String> getRegionImageUrlsWithCache() {
        if (IS_CACHING_ENABLED) {
            // Redis 캐시에서 먼저 시도
            Map<String, String> cached = regionImageRedisTemplate.opsForValue().get(REGION_IMAGE_CACHE_KEY);
            if (cached != null) {
                System.out.println("REDIS : 캐시된 이미지 리스트 가져오기 완료");
                Map<Long, String> converted = cached.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> Long.parseLong(entry.getKey()),
                                Map.Entry::getValue
                        ));
                return converted;
            }

        }

        // 캐시에 없거나 캐싱 비활성화인 경우 새로 계산
        List<Region> allRegions = regionRepository.findAllWithParents();

        Map<Long, Region> regionMap = allRegions.stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()));

        Map<Long, String> result = new HashMap<>();
        for (Region region : allRegions) {
            result.put(region.getId(), resolveImageUrl(region, regionMap));
        }

        // 캐싱이 활성화되어 있다면 Redis에 저장
        if (IS_CACHING_ENABLED) {
            Map<String, String> redisValue = result.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            Map.Entry::getValue
                    ));

            regionImageRedisTemplate.opsForValue().set(REGION_IMAGE_CACHE_KEY, redisValue, REGION_IMAGE_CACHE_TTL);
            System.out.println("REDIS : 이미지 리스트 캐싱 완료");
        }

        return result;
    }

    private String resolveImageUrl(Region region, Map<Long, Region> regionMap) {
        if (region == null) return DEFAULT_IMAGE_URL;
        if (region.getImageUrl() != null && !region.getImageUrl().isBlank()) {
            return region.getImageUrl();
        }

        Region parent = region.getParent();
        if (parent != null && !regionMap.containsKey(parent.getId())) {
            return DEFAULT_IMAGE_URL;
        }

        return resolveImageUrl(parent, regionMap);
    }


}
