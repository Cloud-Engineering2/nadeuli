/* ItineraryEventRepository.java
 * ItineraryEvent 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철      2025.02.25      findByItineraryPerDayIn 작성
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.ItineraryPerDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryEventRepository extends JpaRepository<ItineraryEvent, Long> {

    /**
     * 여러 개의 ItineraryPerDay(하루 일정)에 속하는 모든 ItineraryEvent(방문 장소 이벤트)를 조회하는 쿼리.
     *
     * - `ItineraryEvent`와 `ItineraryPerDay` 간의 관계에서 특정 `ItineraryPerDay` 목록에 속하는 이벤트들을 가져옴.
     * - `JOIN FETCH`를 사용하여 `ItineraryPerDay` 엔티티도 함께 가져오므로 Lazy Loading 문제를 방지.
     * - `WHERE p IN :perDays` 조건을 통해 여러 개의 `ItineraryPerDay`에 해당하는 이벤트들을 한 번에 조회.
     *
     * @param itineraryPerDays 조회할 ItineraryPerDay 리스트
     * @return 해당 ItineraryPerDay에 속한 ItineraryEvent 리스트
     */
    @Query("SELECT e FROM ItineraryEvent e JOIN FETCH e.itineraryPerDay p WHERE p IN :perDays")
    List<ItineraryEvent> findByItineraryPerDayIn(@Param("perDays") List<ItineraryPerDay> itineraryPerDays);

}