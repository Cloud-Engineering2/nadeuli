/* PlaceDTO.java
 * nadeuli Service - 여행
 * Place 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * 박한철    2025.03.09     explanation, googleRating 등 필드 추가
 * 박한철    2025.03.12     explanation -> explanation
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Place;
import nadeuli.common.enums.PlaceCategory.PlaceType;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlaceDTO {
    private Long id;
    private String googlePlaceId;
    private String placeName;
    private String address;
    private double latitude;
    private double longitude;
    private int searchCount;
    private String explanation;
    private Double googleRating;
    private Integer googleRatingCount;
    private String googleURL;
    private String imageUrl;
    private PlaceType placeType;
    private String regularOpeningHours;

    public static PlaceDTO of(Long id, String googlePlaceId, String placeName, String address, double latitude, double longitude,
                              int searchCount, String explanation, Double googleRating, Integer googleRatingCount,
                              String googleURL, String imageUrl, PlaceType placeType, String regularOpeningHours) {
        return new PlaceDTO(id, googlePlaceId, placeName, address, latitude, longitude, searchCount, explanation, googleRating, googleRatingCount, googleURL, imageUrl, placeType, regularOpeningHours);
    }

    public static PlaceDTO of(String googlePlaceId, String placeName, String address, double latitude, double longitude,
                              String explanation, Double googleRating, Integer googleRatingCount, String googleURL, String imageUrl,
                              PlaceType placeType, String regularOpeningHours) {
        return new PlaceDTO(null, googlePlaceId, placeName, address, latitude, longitude, 1, explanation, googleRating, googleRatingCount, googleURL, imageUrl, placeType, regularOpeningHours);
    }

    // entity -> dto 변환
    public static PlaceDTO from(Place place) {
        return new PlaceDTO(
                place.getId(),
                place.getGooglePlaceId(),
                place.getPlaceName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getSearchCount(),
                place.getExplanation(),
                place.getGoogleRating(),
                place.getGoogleRatingCount(),
                place.getGoogleURL(),
                place.getImageUrl(),
                place.getPlaceType(),
                place.getRegularOpeningHours() // JSON 문자열 그대로 유지
        );
    }

    // dto -> entity 변환
    public Place toEntity() {
        return new Place(googlePlaceId, placeName, address, latitude, longitude, explanation, googleRating, googleRatingCount, googleURL, imageUrl, placeType, regularOpeningHours);
    }

    public String toPromptString() {
        return String.format(
                "{\"name\": \"%s\", \"type\": \"%s\", \"latitude\": %.4f, \"longitude\": %.4f, \"placeId\": \"%s\"}",
                placeName,
                placeType.name(),
                latitude,
                longitude,
                id
        );
    }

}
