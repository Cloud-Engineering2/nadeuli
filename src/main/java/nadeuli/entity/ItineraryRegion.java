/* ItineraryRegion.java
 * ItineraryRegion
 * 플래너-지역
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-05
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철     2025.03.05    최초작성
 * 박한철     2025.03.17    region_id -> rid로 수정
 *
 * ========================================================
 */
package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.constant.ItineraryRegionId;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "itinerary_region")
public class ItineraryRegion {

    @EmbeddedId
    private ItineraryRegionId id = new ItineraryRegionId();

    @ManyToOne
    @MapsId("iid") // 복합키의 일부(`iid`)를 매핑
    @JoinColumn(name = "iid")
    private Itinerary itinerary;

    @ManyToOne
    @MapsId("rid") // 복합키의 일부(`rid`)를 매핑
    @JoinColumn(name = "rid")
    private Region region;

    public ItineraryRegion(Itinerary itinerary, Region region) {
        this.id = new ItineraryRegionId(itinerary.getId(), region.getId());
        this.itinerary = itinerary;
        this.region = region;
    }
}