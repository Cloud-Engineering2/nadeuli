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
 * 김대환       2.25      Entity 변경에 따른 코드 수정
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

    @GetMapping("/admin/unlink/{uid}")
    public ResponseEntity<String> adminUnlinkUserByUid(@PathVariable Long uid) {

        Optional<KakaoUser> kakaoUserOptional = kakaoUserRepository.findByUid(uid);

        if (kakaoUserOptional.isPresent()) {
            boolean isUnlinked = kakaoUnlinkService.unlinkUser(uid);

            if (isUnlinked) {
                kakaoUserService.deleteUserByUid(uid);
                return ResponseEntity.ok("정상적으로 카카오 서비스 연결 해제 및 탈퇴 처리되었습니다.\n 사용자: " + uid);
            } else {
                return ResponseEntity.status(500).body("카카오 서비스 연결 해제 실패 했습니다 (기타오류)");
            }
        } else {
            return ResponseEntity.badRequest().body("해당 UID를 가진 사용자를 찾을 수 없습니다: " + uid);
        }
    }
}
