/* AdminRestController.java
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.07   어드민 권한용 Rest 컨트롤러
 *
 * ========================================================
 */

package nadeuli.controller;


import lombok.RequiredArgsConstructor;
import nadeuli.dto.request.AdminRegionUpdateRequestDTO;
import nadeuli.service.RegionService;
import nadeuli.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRestController {

    private final RegionService regionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/region/upload/photo")
    public ResponseEntity<String> uploadRegionImage(
            @RequestParam("regionId") Long regionId,
            @RequestParam("file") MultipartFile file) {

        String newImageUrl = regionService.uploadRegionImage(regionId, file);
        return ResponseEntity.ok(newImageUrl);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/region/update")
    public ResponseEntity<Void> updateRegion(@RequestBody AdminRegionUpdateRequestDTO dto) {
        regionService.updateRegionPosition(dto);
        return ResponseEntity.ok().build();
    }
}
