package nadeuli.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceRequest {
    private String userId;
    private String googlePlaceId;
    private String placeName;
}
