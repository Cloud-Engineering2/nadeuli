package nadeuli.service;

/* KakaoUserService.java
 * 카카오 OAuth 2.0 연동 - 사용자 서비스 탈퇴 시 DB에서 사용자 삭제
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

import nadeuli.repository.KakaoUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoUserService {

    private final KakaoUserRepository kakaoUserRepository;

    @Transactional
    public boolean deleteUserByEmail(String email) {
        if (kakaoUserRepository.findByEmail(email).isEmpty()) {
            System.err.println("삭제 실패: 해당 이메일이 데이터베이스에 없음 → " + email);
            return false;
        }

        kakaoUserRepository.deleteByEmail(email);
        System.out.println("삭제 완료: " + email);
        return true;
    }
}
