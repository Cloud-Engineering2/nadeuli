package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.RegionTreeDTO;
import nadeuli.entity.Region;
import nadeuli.service.RegionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    // 모든 지역 조회
    @GetMapping
    public ResponseEntity<List<Region>> getAllRegions() {
        return ResponseEntity.ok(regionService.getAllRegions());
    }

    // 특정 지역 ID 조회
    @GetMapping("/{rid}")
    public ResponseEntity<Region> getRegionById(@PathVariable Long rid) {
        return regionService.getRegionById(rid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 특정 레벨(시·도 or 시·군·구) 지역 조회
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Region>> getRegionsByLevel(@PathVariable int level) {
        return ResponseEntity.ok(regionService.getRegionsByLevel(level));
    }

    // 특정 시·도에 속하는 시·군·구 조회
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<Region>> getSubRegions(@PathVariable Long parentId) {
        return ResponseEntity.ok(regionService.getSubRegions(parentId));
    }

    // 새로운 지역 추가
    @PostMapping
    public ResponseEntity<Region> addRegion(@RequestParam String regionName,
                                            @RequestParam String alias,
                                            @RequestParam int level,
                                            @RequestParam(required = false) Long parentId) {
        return ResponseEntity.ok(regionService.addRegion(regionName, alias, level, parentId));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<RegionTreeDTO>> getRegionTree() {
        return ResponseEntity.ok(regionService.getRegionTree());
    }

}
