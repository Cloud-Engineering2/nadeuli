/* OAuthUnlinkController.java
 * OAuth 계정 해제 및 회원 탈퇴 API
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       회원 탈퇴 API 초안
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.service.OAuthUnlinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuthUnlinkController {

    private final OAuthUnlinkService oAuthUnlinkService;

    /**
     * ✅ OAuth 계정 해제 및 회원 탈퇴 API
     */
    @DeleteMapping("/unlink/{email}")
    public ResponseEntity<Map<String, Object>> unlinkUser(@PathVariable String email) {
        boolean isDeleted = oAuthUnlinkService.unlinkAndDeleteUser(email);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("success", isDeleted);

        if (isDeleted) {
            response.put("message", "✅ OAuth 계정 해제 및 회원 탈퇴 완료");
            return ResponseEntity.ok(response);
        }

        response.put("message", "❌ 회원 탈퇴 실패");
        return ResponseEntity.status(500).body(response);
    }
}
