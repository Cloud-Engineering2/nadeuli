package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ItineraryRegion;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryRegionDTO {

    private Long itineraryId;
    private Long regionId;
    private String regionName; // 지역 이름

    // Entity → DTO 변환
    public static ItineraryRegionDTO from(ItineraryRegion entity) {
        return new ItineraryRegionDTO(
                entity.getItinerary().getId(),
                entity.getRegion().getId(),
                entity.getRegion().getName()
        );
    }
}