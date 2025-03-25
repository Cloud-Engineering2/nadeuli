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
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItineraryCollaboratorRepository extends JpaRepository<ItineraryCollaborator, Integer> {
    List<ItineraryCollaborator> findByUser_Id(Long userId); // User의 id로 찾기 Collborator
//    List<ItineraryCollaborator> findByUserId(Long userId);

    Optional<ItineraryCollaborator> findByUserIdAndItineraryId(Long itineraryId, Long userId);
    Optional<ItineraryCollaborator> findFirstByItinerary(Itinerary itinerary);

    boolean existsByUserIdAndItineraryIdAndIcRole(Long userId, Long itineraryId, String icRole);
    boolean existsByUserIdAndItineraryId(Long userId, Long itineraryId);
    void deleteByUserIdAndItineraryId(Long userId, Long itineraryId);
    boolean existsByItineraryIdAndIcRole(Long itineraryId, String icRole);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT ic FROM ItineraryCollaborator ic WHERE ic.itinerary.id = :itineraryId")
    List<ItineraryCollaborator> findCollaboratorsByItineraryId(@Param("itineraryId") Long itineraryId);

    Optional<ItineraryCollaborator> findByItinerary_IdAndIcRole(Long itineraryId, String icRole);

    void deleteByItinerary(Itinerary itinerary);
}