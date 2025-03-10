/* PlaceRepository.java
 * Place 레파지토리
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 *
 *
 * ========================================================
 */
package nadeuli.repository;

import nadeuli.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByGooglePlaceId(String googlePlaceId);

//    @Query(value = """
//    WITH scored_place AS (
//      SELECT p.*,
//             ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) AS distance,
//             CASE
//               WHEN ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) <= :radius
//               THEN (p.search_count * 0.6 + p.google_rating_count * 0.4)
//               ELSE (p.search_count * 0.6 + p.google_rating_count * 0.4) *
//                    (1 - (ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) - :radius) / (:radius * 0.5))
//             END AS final_score
//      FROM place p
//      WHERE ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) < :radius * 1.5
//    )
//    SELECT
//      pid, google_place_id, place_name, search_count, address,
//      latitude, longitude, description, google_rating,
//      google_rating_count, google_url, image_url, place_type,
//      regular_opening_hours, distance, final_score
//    FROM scored_place
//    WHERE (:cursorScore IS NULL OR (final_score < :cursorScore OR (final_score = :cursorScore AND pid > :cursorId)))
//    ORDER BY final_score DESC, pid ASC
//    LIMIT :pageSize
//""", nativeQuery = true)
//    List<Object[]> findPlacesWithCursorPaging(
//            @Param("userLng") double userLng,
//            @Param("userLat") double userLat,
//            @Param("radius") double radius,
//            @Param("cursorScore") Double cursorScore,
//            @Param("cursorId") Long cursorId,
//            @Param("pageSize") int pageSize
//    );

//
//@Query(value = """
//    WITH scored_place AS (
//      SELECT p.*,
//             ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) AS distance,
//             CASE
//               WHEN ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) <= :radius
//               THEN (p.search_count * 0.6 + p.google_rating_count * 0.4)
//               ELSE (p.search_count * 0.6 + p.google_rating_count * 0.4) *
//                    (1 - (ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) - :radius) / (:radius * 0.5))
//             END AS final_score
//      FROM place p
//      WHERE ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) < :radius * 1.5
//        AND (:placeTypesIsEmpty = true OR p.place_type IN (:placeTypes))
//    )
//    SELECT
//      pid, google_place_id, place_name, search_count, address,
//      latitude, longitude, description, google_rating,
//      google_rating_count, google_url, image_url, place_type,
//      regular_opening_hours, distance, final_score
//    FROM scored_place
//    WHERE (:cursorScore IS NULL OR (final_score < :cursorScore OR (final_score = :cursorScore AND pid > :cursorId)))
//    ORDER BY final_score DESC, pid ASC
//    LIMIT :pageSize
//""", nativeQuery = true)
//List<Object[]> findPlacesWithCursorPaging(
//        @Param("userLng") double userLng,
//        @Param("userLat") double userLat,
//        @Param("radius") double radius,
//        @Param("cursorScore") Double cursorScore,
//        @Param("cursorId") Long cursorId,
//        @Param("pageSize") int pageSize,
//        @Param("placeTypes") List<String> placeTypes,
//        @Param("placeTypesIsEmpty") boolean placeTypesIsEmpty
//);


}
