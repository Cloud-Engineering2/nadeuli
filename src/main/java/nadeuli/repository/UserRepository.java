/* UserRepository.java
 * 사용자 정보를 관리하는 JPA Repository
 * 작성자 : 국경민
 * 최초 작성 날짜 : 2025-03-04
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 국경민      03-04       사용자 조회 및 삭제 메서드
 * 국경민      03-04       db 토큰사용 제거
 * 국경민      03-07       비활성 사용자 및 Refresh Token 만료 사용자 조회 추가
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserEmail(String email);


}
