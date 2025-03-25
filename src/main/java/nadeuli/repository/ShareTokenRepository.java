/* ShareTokenRepository.java
 * ShareToken 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철     2025.03.07   최초 작성
 *
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.entity.ShareToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareTokenRepository extends JpaRepository<ShareToken, Long> {
    Optional<ShareToken> findByItineraryId(Long itineraryId);
    Optional<ShareToken> findByUuid(String uuid);
    boolean existsByItineraryId(Long itineraryId);

    void deleteByItineraryId(Long itineraryId);
}