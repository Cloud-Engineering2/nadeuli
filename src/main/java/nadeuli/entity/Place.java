package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "place")
public class Place{
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
    private int searchCount;

    public Place(String googlePlaceId, String placeName) {
        this.googlePlaceId = googlePlaceId;
        this.placeName = placeName;
        this.searchCount = 1;
    }

    public void incrementSearchCount() {
        this.searchCount++;
    }

    public static Place of(String googlePlaceId, String placeName) {
        return new Place(googlePlaceId, placeName);
    }

}