/* JournalRepository.java
 * Journal 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 이홍비   2025.03.06    ItineraryEvent 를 이용하여 Journal 조회하는 함수 명시
 *
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    // ItineraryEvent - ieid 에 해당하는 Journal 조회
    Optional<Journal> findByIeid(ItineraryEvent ieid);
}