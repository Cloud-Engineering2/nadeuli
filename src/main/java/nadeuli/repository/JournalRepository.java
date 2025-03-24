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
 * 박한철   2025.03.22    findJournalSimpleDTOsByItineraryId 추가
 * 박한철   2025.03.24    itineraryEvent 들로 모든 Journal 찾아내는  쿼리 추가
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.dto.JournalSimpleDTO;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    // ItineraryEvent - ieid 에 해당하는 Journal 조회
    Optional<Journal> findByIeid(ItineraryEvent ieid);

    @Query("SELECT new nadeuli.dto.JournalSimpleDTO(" +
            "j.id, ie.id, j.content, j.imageUrl, j.createdDate, j.modifiedDate) " +
            "FROM Journal j " +
            "JOIN j.ieid ie " +
            "JOIN ie.itineraryPerDay ipd " +
            "JOIN ipd.itinerary i " +
            "WHERE i.id = :itineraryId")
    List<JournalSimpleDTO> findJournalSimpleDTOsByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("SELECT j FROM Journal j WHERE j.ieid IN :events")
    List<Journal> findByItineraryEvents(@Param("events") List<ItineraryEvent> events);
}