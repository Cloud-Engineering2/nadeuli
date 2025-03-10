package nadeuli.repository;

import nadeuli.entity.Place;
import nadeuli.entity.constant.PlaceCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 DB 사용
@ExtendWith(SpringExtension.class)
class PlaceRepositoryTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Test
    @Transactional
    void testSaveAndFindPlace() {
        // Given: 테스트 장소 생성
        String googlePlaceId = "ChIJA3CU42aifDURaq-3csGXvuc";

        // DB에 중복된 값이 있는지 먼저 확인
        if (placeRepository.findByGooglePlaceId(googlePlaceId).isEmpty()) {
            Place place = new Place(
                    googlePlaceId,
                    "서울역",
                    "서울특별시 중구 한강대로 405",
                    37.555946,
                    126.972317,
                    "서울의 중심 기차역",
                    4.5,
                    5000,
                    "https://maps.google.com/?q=서울역",
                    "https://image.url/seoul_station.jpg",
                    PlaceCategory.PlaceType.TRANSPORTATION,
                    "{'weekday_text': ['월요일: 05:00~23:59', '화요일: 05:00~23:59']}"
            );

            placeRepository.saveAndFlush(place);
            System.out.println("새로운 Place 저장 완료");
        } else {
            System.out.println("중복 데이터 존재.");
        }

        // When: 저장된 데이터 조회
        Optional<Place> foundPlace = placeRepository.findByGooglePlaceId(googlePlaceId);

        // Then: 데이터 검증
        assertTrue(foundPlace.isPresent(), "Place가 DB에 저장되지 않음");
        assertEquals("서울역", foundPlace.get().getPlaceName());

        System.out.println("DB 저장 확인 완료: " + foundPlace.get());
    }
}
