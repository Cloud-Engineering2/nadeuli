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
 * 박한철    2025.02.25   findByItineraryPerDayIn 작성
 * 박한철    2025.02.28   findByItineraryPerDayIn N+1 해결
 * 이홍비    2025.03.22   findItineraryEventIdsByItineraryId 작성
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.ItineraryPerDay;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryEventRepository extends JpaRepository<ItineraryEvent, Long> {

    //EntityGraph를 이용해서 N+1 해결
    @EntityGraph(attributePaths = {"itineraryPerDay", "place"})
    @Query("SELECT e FROM ItineraryEvent e WHERE e.itineraryPerDay IN :perDays")
    List<ItineraryEvent> findByItineraryPerDayIn(@Param("perDays") List<ItineraryPerDay> itineraryPerDays);

    List<ItineraryEvent> findByItineraryPerDay(ItineraryPerDay perDay);


    // itinerary 산하 itineraryEvent id 추출
    @EntityGraph(attributePaths = {"itineraryPerDay"})
    @Query("SELECT e.id FROM ItineraryEvent e WHERE e.itineraryPerDay.itinerary.id = :itineraryId")
    List<Long> findItineraryEventIdsByItineraryId(@Param("itineraryId") Long itineraryId);



}