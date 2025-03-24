/* Region.java
 * region 관련
 * 작성자 : 박한철
 * 최종 수정 날짜 : 2025.03.05
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.05     최초 작성
 * 김대환    2025.03.04     위도,경도 추가
 * 박한철    2025.03.09     장소세부정보 추가
 * 박한철    2025.03.12     description->explanation로 변경
 * ========================================================
 */
package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import nadeuli.entity.constant.PlaceCategory.PlaceType;

import java.io.Serializable;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "place")
public class Place implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pid", nullable = false)
    private Long id;

    @Column(name = "google_place_id", nullable = false, unique = true)
    private String googlePlaceId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "search_count", nullable = false)
    @ColumnDefault("0")
    private int searchCount = 0;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "google_rating")
    private Double googleRating;

    @Column(name = "google_rating_count")
    private Integer googleRatingCount;

    @Column(name = "google_url", length = 500)
    private String googleURL;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "place_type", nullable = false)
    private PlaceType placeType;

    @Column(name = "regular_opening_hours", columnDefinition = "TEXT") // JSON 문자열 저장
    private String regularOpeningHours;

    public Place(String googlePlaceId, String placeName, String address, double latitude, double longitude,
                 String explanation, Double googleRating, Integer googleRatingCount, String googleURL,
                 String imageUrl, PlaceType placeType, String regularOpeningHours) {
        this.googlePlaceId = googlePlaceId;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.searchCount = 1;
        this.explanation = explanation;
        this.googleRating = googleRating;
        this.googleRatingCount = googleRatingCount;
        this.googleURL = googleURL;
        this.imageUrl = imageUrl;
        this.placeType = placeType;
        this.regularOpeningHours = regularOpeningHours;
    }

    public void incrementSearchCount() {
        this.searchCount++;
    }

    public static Place of(String googlePlaceId, String placeName, String address, double latitude, double longitude,
                           String explanation, Double googleRating, Integer googleRatingCount, String googleURL,
                           String imageUrl, PlaceType placeType, String regularOpeningHours) {
        return new Place(googlePlaceId, placeName, address, latitude, longitude, explanation, googleRating, googleRatingCount, googleURL, imageUrl, placeType, regularOpeningHours);
    }

}
