package nadeuli.dto.request;


import lombok.Getter;

@Getter
public class PlaceRecommendRequestDto {
    private double userLng;
    private double userLat;
    private double radius;
    private Double cursorScore; // null 가능
    private Long cursorId;      // null 가능
    private int pageSize = 10;  // 기본값
}
