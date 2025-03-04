/* LogoutController.java
 * JWT 삭제 및 로그아웃 API
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       로그아웃 API 초안
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.service.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LogoutController {
    private final JwtTokenService jwtTokenService;

    /**
     * ✅ 로그아웃 API
     * - Redis에서 JWT 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String token) {
        // 1️⃣ JWT에서 사용자 이메일 추출
        String userEmail = jwtTokenService.getUserEmail(token);

        // 2️⃣ Redis에서 토큰 삭제
        jwtTokenService.deleteTokens(userEmail);

        // 3️⃣ 응답 반환
        return ResponseEntity.ok(Map.of("success", true, "message", "로그아웃 완료"));
    }
}
