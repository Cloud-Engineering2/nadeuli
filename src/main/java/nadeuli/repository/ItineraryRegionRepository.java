/* ItineraryRegionRepository.java
 * ItineraryRegionRepository 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.14    리전 리스트 가져오기
 *
 * ========================================================
 */
package nadeuli.repository;


import nadeuli.entity.ItineraryRegion;
import nadeuli.entity.constant.ItineraryRegionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryRegionRepository extends JpaRepository<ItineraryRegion, ItineraryRegionId> {


    @Query("""
    SELECT ir FROM ItineraryRegion ir
    JOIN FETCH ir.region
    WHERE ir.itinerary.id IN :itineraryIds
""")
    List<ItineraryRegion> findByItineraryIdIn(@Param("itineraryIds") List<Long> itineraryIds);

    @Query("""
    SELECT ir FROM ItineraryRegion ir
    JOIN FETCH ir.region
    WHERE ir.itinerary.id = :itineraryId
""")
    List<ItineraryRegion> findByItineraryIdWithRegion(@Param("itineraryId") Long itineraryId);


}
