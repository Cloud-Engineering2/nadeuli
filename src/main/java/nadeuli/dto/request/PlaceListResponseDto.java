package nadeuli.dto.request;



import lombok.Builder;
import lombok.Getter;
import nadeuli.dto.response.PlaceResponseDto;

import java.util.List;

@Getter
@Builder
public class PlaceListResponseDto {
    private List<PlaceResponseDto> places;
    private Double nextCursorScore;
    private Long nextCursorId;
}
