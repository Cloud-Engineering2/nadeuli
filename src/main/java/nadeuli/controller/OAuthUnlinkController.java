/* OAuthUnlinkController.java
 * 구글 및 카카오 OAuth 2.0 연동 - 사용자 서비스 탈퇴
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      2.25        구글 및 카카오 통합 OAuth 2.0 언링크 컨트롤러 구현
 * 국경민      2.26         refreshToken 필드 추가
 * 국경민      2.27        Redis를 사용한 토큰 관리
 * ========================================================
 */

package nadeuli.controller;

import nadeuli.service.OAuthUserService;
import nadeuli.service.OAuthUnlinkService;
import nadeuli.util.JwtTokenProvider;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OAuthUnlinkController {

    private final OAuthUserService oAuthUserService;
    private final OAuthUnlinkService oAuthUnlinkService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @DeleteMapping("/admin/unlink/{email}")
    public ResponseEntity<String> adminUnlinkUserByEmail(@PathVariable String email) {

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String provider = user.getProvider();
            Long userId = user.getId();
            String refreshToken = user.getRefreshToken(); // 사용자 객체에서 refreshToken 가져오기

            boolean isUnlinked = oAuthUnlinkService.unlinkUser(userId, provider, refreshToken);

            if (isUnlinked) {
                // Redis에서 토큰 삭제
                jwtTokenProvider.deleteToken("accessToken:" + email);
                jwtTokenProvider.deleteToken("refreshToken:" + email);

                oAuthUserService.deleteUserByEmail(email);
                return ResponseEntity.ok("정상적으로 서비스 연결 해제 및 탈퇴 처리되었습니다.\n 사용자: " + email);
            } else {
                return ResponseEntity.status(500).body("서비스 연결 해제 실패 했습니다 (기타 오류)");
            }
        } else {
            return ResponseEntity.badRequest().body("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email);
        }
    }
}
