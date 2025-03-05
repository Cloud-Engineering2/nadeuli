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
    @MapsId("regionId") // 복합키의 일부(`regionId`)를 매핑
    @JoinColumn(name = "region_id")
    private Region region;

    public ItineraryRegion(Itinerary itinerary, Region region) {
        this.id = new ItineraryRegionId(itinerary.getId(), region.getId());
        this.itinerary = itinerary;
        this.region = region;
    }
}