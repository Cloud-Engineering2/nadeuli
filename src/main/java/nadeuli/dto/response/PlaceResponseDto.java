package nadeuli.dto.response;

import lombok.Builder;
import lombok.Getter;
import nadeuli.common.enums.PlaceCategory.PlaceType;

@Getter
@Builder
public class PlaceResponseDto {
    private Long id;
    private String googlePlaceId;
    private String placeName;
    private int searchCount;
    private String address;
    private double latitude;
    private double longitude;
    private String explanation;
    private Double googleRating;
    private Integer googleRatingCount;
    private String googleURL;
    private String imageUrl;
    private PlaceType placeType;
    private String regularOpeningHours;

    // 추가로 쿼리에서 계산된 값
    private Double distance;
    private Double finalScore;
}
