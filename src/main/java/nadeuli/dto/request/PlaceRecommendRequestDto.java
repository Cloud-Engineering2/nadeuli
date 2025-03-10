package nadeuli.dto.request;


import lombok.Getter;
import nadeuli.entity.constant.PlaceCategory;

import java.util.List;

@Getter
public class PlaceRecommendRequestDto {
    private double userLng;
    private double userLat;
    private double radius;
    private Double cursorScore; // null 가능
    private Long cursorId;      // null 가능
    private int pageSize = 10;  // 기본값
    private List<String> placeTypes; // 추가된 필드
    private boolean searchEnabled;
    private String searchQuery;
}
