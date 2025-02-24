package nadeuli.controller;

/* KakaoOAuthUnlinkController.java
 * 카카오 OAuth 2.0 연동 - 사용자 서비스 탈퇴
 * 해당 파일 설명
 * 작성자 : 김대환
 * 최초 작성 날짜 : 2025-02-20
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */

import nadeuli.service.KakaoUserService;
import nadeuli.service.KakaoUnlinkService;
import nadeuli.entity.KakaoUser;
import nadeuli.repository.KakaoUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KakaoOAuthUnlinkController {

    private final KakaoUserService kakaoUserService;
    private final KakaoUnlinkService kakaoUnlinkService;
    private final KakaoUserRepository kakaoUserRepository;

    @DeleteMapping("/admin/unlink/{email}")
    public ResponseEntity<String> adminUnlinkUserByEmail(@PathVariable String email) {

        Optional<KakaoUser> kakaoUserOptional = kakaoUserRepository.findByEmail(email);

        if (kakaoUserOptional.isPresent()) {
            KakaoUser kakaoUser = kakaoUserOptional.get();
            Long userId = Long.parseLong(kakaoUser.getId());

            boolean isUnlinked = kakaoUnlinkService.unlinkUser(userId);

            if (isUnlinked) {
                kakaoUserService.deleteUserByEmail(email);
                return ResponseEntity.ok("정상적으로 카카오 서비스 연결 해제 및 탈퇴 처리되었습니다.\n 사용자: " + email);
            } else {
                return ResponseEntity.status(500).body("카카오 서비스 연결 해제 실패 했습니다 (기타오류)");
            }
        } else {
            return ResponseEntity.badRequest().body("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email);
        }
    }
}
