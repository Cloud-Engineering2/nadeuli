package nadeuli.entity.constant;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ItineraryRegionId implements Serializable {

    private Long iid; // 여행 일정 ID
    private Long regionId; // 지역 ID
}