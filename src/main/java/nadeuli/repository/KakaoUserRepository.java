package nadeuli.repository;

/* KakaoUserRepository.java
 * 카카오 OAuth 2.0 연동
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

import nadeuli.entity.KakaoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface KakaoUserRepository extends JpaRepository<KakaoUser, String> {
    Optional<KakaoUser> findByEmail(String email);

    @Transactional
    void deleteByEmail(String email);
//
//    @Modifying
//    @Transactional
//    @Query("DELETE FROM KakaoUser k WHERE LOWER(k.email) = LOWER(:email)")
//    void deleteByEmail(@Param("email") String email);
}
