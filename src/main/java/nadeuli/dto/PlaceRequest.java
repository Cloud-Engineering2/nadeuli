package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlaceRequest {
    private String userId;
    private String PlaceId;
    private String placeName;
    private String MainText;
    private String address;
    private Long latitude;
    private Long longitude;
}
