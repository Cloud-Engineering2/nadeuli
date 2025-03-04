package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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

    public Place(String googlePlaceId, String placeName, String address, double latitude, double longitude) {
        this.googlePlaceId = googlePlaceId;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.searchCount = 1;
    }

    public Place(String googlePlaceId, String placeName) {
        this.googlePlaceId = googlePlaceId;
        this.placeName = placeName;
    }

    public void incrementSearchCount() {
        this.searchCount++;
    }

    public static Place of(String googlePlaceId, String placeName, String address, double latitude, double longitude) {
        return new Place(googlePlaceId, placeName, address, latitude, longitude);
    }
}
