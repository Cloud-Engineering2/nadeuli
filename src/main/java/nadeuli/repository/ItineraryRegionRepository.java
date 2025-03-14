/* ItineraryRepository.java
 * Itinerary 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.26    findByUserId를 findByUserIdWithRole로 JPQL 방식으로 변경
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryRegion;
import nadeuli.entity.constant.ItineraryRegionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryRegionRepository extends JpaRepository<ItineraryRegion, ItineraryRegionId> {
}
