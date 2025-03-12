/* TravelerRepository.java
 * Traveler 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철   2025.02.25   Repository 생성
 * 고민정   2025.02.26   iid, travelerName으로 조회 메서드 추가
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.Itinerary;
import nadeuli.entity.Traveler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelerRepository extends JpaRepository<Traveler, Integer> {
    List<Traveler> findByIid(Itinerary itinerary);

    Optional<Traveler> findByTravelerName(String travelerName);

    List<Traveler> findAllByIid(Itinerary itinerary);

    @Query("SELECT t FROM Traveler t WHERE t.iid.id = :itineraryId AND t.travelerName IN :withWhomNames")
    List<Traveler> findByItineraryIdAndTravelerNames(@Param("itineraryId") Long itineraryId, @Param("withWhomNames") List<String> withWhomNames);

//    List<Traveler> findByIidAndTravelerNameIn(Itinerary itinerary, List<String> withWhomNames);

    @Query("SELECT t FROM Traveler t WHERE t.iid.id = :iid AND t.travelerName = :travelerName")
    Optional<Traveler> findTravelerByItineraryIdAndTravelerName(@Param("iid") Long iid, @Param("travelerName") String travelerName);
}