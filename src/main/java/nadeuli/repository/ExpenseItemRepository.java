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
 * 박한철    2025.03.17   이벤트별 합산금액 출력
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.ExpenseBook;
import nadeuli.entity.ExpenseItem;
import nadeuli.entity.ItineraryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
    List<ExpenseItem> findAllByIeid(ItineraryEvent itineraryEvent);

    @Query(value = """
    SELECT ei.ieid AS eventId, SUM(ei.expense) AS totalExpense
    FROM expense_item ei
    WHERE ei.ebid = :expenseBookId
    GROUP BY ei.ieid
""", nativeQuery = true)
    List<Object[]> findTotalExpenseByEventByExpenseBookId(@Param("expenseBookId") Long expenseBookId);


    List<ExpenseItem> findAllByEbid(ExpenseBook expenseBook);
}