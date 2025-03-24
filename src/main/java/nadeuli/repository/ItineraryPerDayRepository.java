/* ItineraryPerDayRepository.java
 * ItineraryPerDay 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.27    findByItinerary 수정
 * 고민정    2025.03.04    findByItineraryPerDay 추가
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryPerDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryPerDayRepository extends JpaRepository<ItineraryPerDay, Long> {
    /**
     * 특정 Itinerary(여행 일정)에 속하는 모든 ItineraryPerDay(하루 일정) 목록을 조회하는 쿼리.
     *
     * - `ItineraryPerDay`와 `Itinerary` 간의 관계에서 특정 `Itinerary`에 속한 `ItineraryPerDay`를 가져옴.
     * - `JOIN FETCH`를 사용하여 `Itinerary` 엔티티도 함께 가져오므로 Lazy Loading 문제를 방지.
     * - `WHERE p.itinerary = :itinerary` 조건을 통해 특정 `Itinerary`의 하루 일정을 필터링.
     *
     * @param itinerary 조회할 Itinerary 엔티티
     * @return 해당 Itinerary에 속한 ItineraryPerDay 리스트
     */
    @Query("SELECT p FROM ItineraryPerDay p JOIN FETCH p.itinerary WHERE p.itinerary = :itinerary")
    List<ItineraryPerDay> findByItinerary(@Param("itinerary") Itinerary itinerary);

    void deleteAllByItinerary(Itinerary itinerary);
}