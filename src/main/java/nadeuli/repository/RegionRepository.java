package nadeuli.repository;

import nadeuli.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    // 특정 레벨(시·도 or 시·군·구) 조회
    List<Region> findByLevel(int level);

    // 특정 상위 지역(시·도)에 속하는 시·군·구 조회
    List<Region> findByParent(Region parent);
}