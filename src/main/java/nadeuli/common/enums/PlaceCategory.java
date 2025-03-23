/* PlaceCategory.java
 * nadeuli Service - 여행
 * 열거형 PlaceCategory
 * 작성자 : 박한철
 * 최종 수정 날짜 : 2025.03.09
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철   2025.03.09    최초 작성 : PlaceCategory
 * 이홍비   2025.03.23    어트랙션 => 여가 시설로 변경
 *
 * ========================================================
 */


package nadeuli.entity.enums;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceCategory {

    public enum PlaceType {
        LANDMARK, RESTAURANT, LODGING, CAFE, TRANSPORTATION, ATTRACTION, CONVENIENCE
    }


    // PlaceType 우선순위 정의
    private static final List<PlaceType> PRIORITY_ORDER = List.of(
            PlaceType.LANDMARK,
            PlaceType.RESTAURANT,
            PlaceType.LODGING,
            PlaceType.CAFE,
            PlaceType.TRANSPORTATION,
            PlaceType.ATTRACTION,
            PlaceType.CONVENIENCE
    );


    public static final Map<String, CategoryWeight> PLACE_TYPE_MAP = new HashMap<>();

    static {
        // 명소 (Landmark, Attraction)
        addPlaceType("tourist_attraction", PlaceType.LANDMARK, 10);
        addPlaceType("cultural_landmark", PlaceType.LANDMARK, 10);
        addPlaceType("historical_landmark", PlaceType.LANDMARK, 10);
        addPlaceType("monument", PlaceType.LANDMARK, 8);
        addPlaceType("museum", PlaceType.LANDMARK, 9);
        addPlaceType("art_gallery", PlaceType.LANDMARK, 8);
        addPlaceType("historical_place", PlaceType.LANDMARK, 8);
        addPlaceType("sculpture", PlaceType.LANDMARK, 7);
        addPlaceType("botanical_garden", PlaceType.LANDMARK, 7);
        addPlaceType("garden", PlaceType.LANDMARK, 6);
        addPlaceType("observation_deck", PlaceType.LANDMARK, 8);
        addPlaceType("national_park", PlaceType.LANDMARK, 9);
        addPlaceType("state_park", PlaceType.LANDMARK, 8);
        addPlaceType("park", PlaceType.LANDMARK, 6);
        addPlaceType("plaza", PlaceType.LANDMARK, 6);
        addPlaceType("zoo", PlaceType.LANDMARK, 9);
        addPlaceType("wildlife_park", PlaceType.LANDMARK, 8);
        addPlaceType("wildlife_refuge", PlaceType.LANDMARK, 7);
        addPlaceType("planetarium", PlaceType.LANDMARK, 7);
        addPlaceType("visitor_center", PlaceType.LANDMARK, 6);

        // 식당 (Food & Restaurant)
        addPlaceType("restaurant", PlaceType.RESTAURANT, 10);
        addPlaceType("barbecue_restaurant", PlaceType.RESTAURANT, 8);
        addPlaceType("buffet_restaurant", PlaceType.RESTAURANT, 7);
        addPlaceType("cafe", PlaceType.RESTAURANT, 8);
        addPlaceType("coffee_shop", PlaceType.RESTAURANT, 8);
        addPlaceType("dessert_restaurant", PlaceType.RESTAURANT, 7);
        addPlaceType("fast_food_restaurant", PlaceType.RESTAURANT, 7);
        addPlaceType("fine_dining_restaurant", PlaceType.RESTAURANT, 9);
        addPlaceType("food_court", PlaceType.RESTAURANT, 6);
        addPlaceType("hamburger_restaurant", PlaceType.RESTAURANT, 7);
        addPlaceType("ice_cream_shop", PlaceType.RESTAURANT, 6);
        addPlaceType("pizza_restaurant", PlaceType.RESTAURANT, 7);
        addPlaceType("ramen_restaurant", PlaceType.RESTAURANT, 7);
        addPlaceType("seafood_restaurant", PlaceType.RESTAURANT, 8);
        addPlaceType("steak_house", PlaceType.RESTAURANT, 8);
        addPlaceType("sushi_restaurant", PlaceType.RESTAURANT, 8);
        addPlaceType("tea_house", PlaceType.RESTAURANT, 7);
        addPlaceType("wine_bar", PlaceType.RESTAURANT, 7);

        // 숙소 (Lodging & Accommodation)
        addPlaceType("hotel", PlaceType.LODGING, 10);
        addPlaceType("resort_hotel", PlaceType.LODGING, 10);
        addPlaceType("motel", PlaceType.LODGING, 7);
        addPlaceType("hostel", PlaceType.LODGING, 8);
        addPlaceType("guest_house", PlaceType.LODGING, 7);
        addPlaceType("bed_and_breakfast", PlaceType.LODGING, 7);
        addPlaceType("campground", PlaceType.LODGING, 6);
        addPlaceType("lodging", PlaceType.LODGING, 9);
        addPlaceType("cottage", PlaceType.LODGING, 7);
        addPlaceType("japanese_inn", PlaceType.LODGING, 8);
        addPlaceType("ryokan", PlaceType.LODGING, 8);
        addPlaceType("rv_park", PlaceType.LODGING, 6);
        addPlaceType("farmstay", PlaceType.LODGING, 6);

        // 카페 (Cafe & Beverage)
        addPlaceType("cafe", PlaceType.CAFE, 10);
        addPlaceType("coffee_shop", PlaceType.CAFE, 10);
        addPlaceType("juice_shop", PlaceType.CAFE, 7);
        addPlaceType("tea_house", PlaceType.CAFE, 7);
        addPlaceType("cat_cafe", PlaceType.CAFE, 8);
        addPlaceType("dog_cafe", PlaceType.CAFE, 8);
        addPlaceType("wine_bar", PlaceType.CAFE, 6);

        // 교통 (Transportation)
        addPlaceType("airport", PlaceType.TRANSPORTATION, 10);
        addPlaceType("train_station", PlaceType.TRANSPORTATION, 10);
        addPlaceType("bus_station", PlaceType.TRANSPORTATION, 8);
        addPlaceType("subway_station", PlaceType.TRANSPORTATION, 9);
        addPlaceType("light_rail_station", PlaceType.TRANSPORTATION, 8);
        addPlaceType("ferry_terminal", PlaceType.TRANSPORTATION, 8);
        addPlaceType("parking", PlaceType.TRANSPORTATION, 6);
        addPlaceType("taxi_stand", PlaceType.TRANSPORTATION, 7);
        addPlaceType("gas_station", PlaceType.TRANSPORTATION, 6);
        addPlaceType("electric_vehicle_charging_station", PlaceType.TRANSPORTATION, 6);
        addPlaceType("car_rental", PlaceType.TRANSPORTATION, 7);
        addPlaceType("car_wash", PlaceType.TRANSPORTATION, 5);
        addPlaceType("rest_stop", PlaceType.TRANSPORTATION, 6);
        addPlaceType("truck_stop", PlaceType.TRANSPORTATION, 5);

        // 어트랙션 (Entertainment & Activity)
        addPlaceType("amusement_park", PlaceType.ATTRACTION, 20);
        addPlaceType("water_park", PlaceType.ATTRACTION, 20);
        addPlaceType("casino", PlaceType.ATTRACTION, 20);
        addPlaceType("ferris_wheel", PlaceType.ATTRACTION, 20);
        addPlaceType("movie_theater", PlaceType.ATTRACTION, 20);
        addPlaceType("concert_hall", PlaceType.ATTRACTION, 20);
        addPlaceType("performing_arts_theater", PlaceType.ATTRACTION, 20);
        addPlaceType("opera_house", PlaceType.ATTRACTION, 20);
        addPlaceType("ski_resort", PlaceType.ATTRACTION, 20);
        addPlaceType("stadium", PlaceType.ATTRACTION, 20);
        addPlaceType("golf_course", PlaceType.ATTRACTION, 20);
        addPlaceType("hiking_area", PlaceType.ATTRACTION, 20);
        addPlaceType("playground", PlaceType.ATTRACTION, 20);

        // 편의시설 (Convenience & Services)
        addPlaceType("bank", PlaceType.CONVENIENCE, 19);
        addPlaceType("public_bathroom", PlaceType.CONVENIENCE, 19);
        addPlaceType("public_bath", PlaceType.CONVENIENCE, 18);
        addPlaceType("laundry", PlaceType.CONVENIENCE, 17);
        addPlaceType("pharmacy", PlaceType.CONVENIENCE, 18);
        addPlaceType("supermarket", PlaceType.CONVENIENCE, 18);
        addPlaceType("convenience_store", PlaceType.CONVENIENCE, 17);
        addPlaceType("market", PlaceType.CONVENIENCE, 7);
        addPlaceType("grocery_store", PlaceType.CONVENIENCE, 7);
        addPlaceType("shopping_mall", PlaceType.CONVENIENCE, 7);
        addPlaceType("department_store", PlaceType.CONVENIENCE, 7);
        addPlaceType("book_store", PlaceType.CONVENIENCE, 6);
        addPlaceType("electronics_store", PlaceType.CONVENIENCE, 6);
        addPlaceType("hardware_store", PlaceType.CONVENIENCE, 6);
        addPlaceType("pet_store", PlaceType.CONVENIENCE, 6);
        addPlaceType("sporting_goods_store", PlaceType.CONVENIENCE, 6);
        addPlaceType("clothing_store", PlaceType.CONVENIENCE, 6);

    }

    public static PlaceType determinePlaceType(List<String> googleTags) {
        PlaceType bestCategory = PlaceType.LANDMARK; // 기본값
        int maxWeight = 0;

        System.out.println("=== Debugging determinePlaceType ===");
        System.out.println("Input tags: " + googleTags);

        for (String tag : googleTags) {
            System.out.println("Checking tag: " + tag);

            CategoryWeight categoryWeight = PLACE_TYPE_MAP.get(tag);
            if (categoryWeight != null) {
                System.out.println("Found category: " + categoryWeight.category + ", weight: " + categoryWeight.weight);

                if (categoryWeight.weight > maxWeight) {
                    System.out.println("Updating bestCategory (higher weight): " + bestCategory + " -> " + categoryWeight.category);
                    maxWeight = categoryWeight.weight;
                    bestCategory = categoryWeight.category;
                } else if (categoryWeight.weight == maxWeight) {
                    // 가중치가 같다면 우선순위 비교
                    int currentIndex = PRIORITY_ORDER.indexOf(categoryWeight.category);
                    int bestIndex = PRIORITY_ORDER.indexOf(bestCategory);
                    System.out.println("Same weight,comparing priority: " + categoryWeight.category + " (" + currentIndex + ") vs " + bestCategory + " (" + bestIndex + ")");

                    if (currentIndex < bestIndex) {
                        System.out.println("Updating bestCategory (higher priority): " + bestCategory + " -> " + categoryWeight.category);
                        bestCategory = categoryWeight.category;
                    }
                }
            } else {
                System.out.println("No mapping found for tag: " + tag);
            }
        }

        System.out.println("Final determined PlaceType: " + bestCategory);
        return bestCategory;
    }


    private static void addPlaceType(String type, PlaceType category, int weight) {
        PLACE_TYPE_MAP.put(type, new CategoryWeight(category, weight));
    }

    public static class CategoryWeight {
        public final PlaceType category;
        public final int weight;

        public CategoryWeight(PlaceType category, int weight) {
            this.category = category;
            this.weight = weight;
        }
    }
}