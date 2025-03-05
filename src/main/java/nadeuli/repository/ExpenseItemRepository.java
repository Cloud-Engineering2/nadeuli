/* ExpenseItemRepository.java
 * ExpenseItem 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정    2025.02.27   Itinerary Event로 조회
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.ExpenseItem;
import nadeuli.entity.ItineraryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
    List<ExpenseItem> findAllByIeid(ItineraryEvent itineraryEvent);

}