/* WithWhomRepository.java
 * WithWhom 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정     2025.02.28  Emid로 모든 witWhom 조회 메서드 추가
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.ExpenseItem;
import nadeuli.entity.Traveler;
import nadeuli.entity.WithWhom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WithWhomRepository extends JpaRepository<WithWhom, Integer> {
    List<WithWhom> findAllByEmid(ExpenseItem expenseItem);
    @Query("""
    select distinct ww.tid.id
    from WithWhom ww
    join ww.emid ei
    join ei.ieid ie
    join ie.itineraryPerDay ipd
    where ipd.itinerary.id = :itineraryId
""")
    List<Integer> findConsumerIdsByItinerary(@Param("itineraryId") Long itineraryId);

    boolean existsByTid(Traveler traveler);
}