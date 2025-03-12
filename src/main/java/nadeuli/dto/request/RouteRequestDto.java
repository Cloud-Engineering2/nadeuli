package nadeuli.dto.request;

import lombok.Data;

@Data
public class RouteRequestDto {
    private double originLatitude;
    private double originLongitude;
    private double destinationLatitude;
    private double destinationLongitude;
}
