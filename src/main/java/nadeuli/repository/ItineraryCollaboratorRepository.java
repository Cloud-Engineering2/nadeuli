/* ItineraryCollaboratorRepository.java
 * ItineraryCollaborator 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     최초작성
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryCollaborator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItineraryCollaboratorRepository extends JpaRepository<ItineraryCollaborator, Integer> {
//    List<ItineraryCollaborator> findByUser_Id(Long userId); // User의 id로 찾기 Collborator

    Optional<ItineraryCollaborator> findByItinerary_IdAndUser_Id(Long itineraryId, Long userId);
}