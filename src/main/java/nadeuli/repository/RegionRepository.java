/* RegionRepository.java
 * region 관련
 * 작성자 : 박한철
 * 최종 수정 날짜 : 2025.03.05
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.05     최초 작성
 * 박한철    2025.03.14     리전 이미지 리스트 가져오기 위한 함수 추가
 * ========================================================
 */

package nadeuli.repository;

import nadeuli.entity.Region;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    // 특정 레벨(시·도 or 시·군·구) 조회
    List<Region> findByLevel(int level);

    // 특정 상위 지역(시·도)에 속하는 시·군·구 조회
    List<Region> findByParent(Region parent);

    @EntityGraph(attributePaths = {"parent", "parent.parent"})
    @Query("SELECT r FROM Region r")
    List<Region> findAllWithParents();
}