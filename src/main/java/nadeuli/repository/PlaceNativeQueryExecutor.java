/* PlaceNativeQueryExecutor.java
 * 나들이 장소 검색 geo 기반 + 추천점수 기반 Native 쿼리
 * Place 관련
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.03. 09
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.09     최초작성
 * ========================================================
 */

package nadeuli.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlaceNativeQueryExecutor {

    private final EntityManager entityManager;

    public List<Object[]> findPlacesWithDynamicQuery(
            double userLng, double userLat, double radius,
            Double cursorScore, Long cursorId, int pageSize,
            List<String> placeTypes,
            boolean searchEnabled,       // ✅ 명확히 받자
            String searchQuery
    )
 {
     boolean placeTypesIsEmpty = (placeTypes == null || placeTypes.isEmpty());

     boolean effectiveSearchEnabled = searchEnabled && searchQuery != null && !searchQuery.isBlank();


     StringBuilder sql = new StringBuilder("""
            WITH scored_place AS (
              SELECT p.*,
                     ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) AS distance,
                     CASE
                       WHEN ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) <= :radius
                       THEN (p.search_count * 0.6 + p.google_rating_count * 0.4)
                       ELSE (p.search_count * 0.6 + p.google_rating_count * 0.4) *
                            (1 - (ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) - :radius) / (:radius * 0.5))
                     END AS final_score
              FROM place p
              WHERE ST_Distance_Sphere(point(p.longitude, p.latitude), point(:userLng, :userLat)) < :radius * 1.5
            """);

        if (!placeTypesIsEmpty) {
            sql.append(" AND p.place_type IN (:placeTypes)");
        }

         List<String> tokens = new ArrayList<>();
         if (effectiveSearchEnabled) {
             tokens = Arrays.stream(searchQuery.trim().toLowerCase().split("\\s+"))
                     .filter(t -> !t.isBlank())
                     .toList();

             // SQL 조건 생성
             for (int i = 0; i < tokens.size(); i++) {
                 sql.append(" AND (LOWER(p.place_name) LIKE :token").append(i)
                         .append(" OR LOWER(p.explanation) LIKE :token").append(i)
                         .append(" OR LOWER(p.address) LIKE :token").append(i).append(")");
             }
         }


        sql.append("""
            )
            SELECT pid, google_place_id, place_name, search_count, address,
                   latitude, longitude, explanation, google_rating,
                   google_rating_count, google_url, image_url, place_type,
                   regular_opening_hours, distance, final_score
            FROM scored_place
            WHERE (:cursorScore IS NULL OR (final_score < :cursorScore OR (final_score = :cursorScore AND pid > :cursorId)))
            ORDER BY final_score DESC, pid ASC
            LIMIT :pageSize
        """);

        Query query = entityManager.createNativeQuery(sql.toString());

        query.setParameter("userLng", userLng);
        query.setParameter("userLat", userLat);
        query.setParameter("radius", radius);
        query.setParameter("cursorScore", cursorScore);
        query.setParameter("cursorId", cursorId);
        query.setParameter("pageSize", pageSize);

        if (!placeTypesIsEmpty) {
            query.setParameter("placeTypes", placeTypes);
        }

         for (int i = 0; i < tokens.size(); i++) {
             query.setParameter("token" + i, "%" + tokens.get(i) + "%");
         }



        return query.getResultList();
    }
}
