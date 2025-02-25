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
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Place;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlaceDTO {
    private Long id;
    private String googlePlaceId;
    private String placeName;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;



    // static factory method
    public static PlaceDTO of (Long id, String googlePlaceId, String placeName, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        return new PlaceDTO(id, googlePlaceId, placeName, createdAt, modifiedAt);
    }

    public static PlaceDTO of (String googlePlaceId, String placeName, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        return new PlaceDTO(null, googlePlaceId, placeName, createdAt, modifiedAt);
    }

    // entity -> dto
    public static PlaceDTO from(Place place) {
        return new PlaceDTO(
                place.getId(),
                place.getGooglePlaceId(),
                place.getPlaceName(),
                place.getCreatedDate(),
                place.getModifiedDate()
        );
    }

    // dto => entity
    public Place toEntity() {
        return Place.of(googlePlaceId, placeName);
    }
}
