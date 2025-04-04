/* ItineraryResponseDTO.java
 * 수정 시간, 생성 시간을 포함한 일정을 반환하기 위한 Response DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     최초 작성
 * 박한철    2025.02.26     DTO 변환 방식 오버로딩 방식으로 추가
 * 이홍비    2025.03.10     @ToString 추가
 * ========================================================
 */

package nadeuli.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryCollaborator;
import java.time.LocalDateTime;
import java.util.List;
import nadeuli.dto.ItineraryRegionDTO;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryResponseDTO {

    private Long id;
    private String itineraryName;
    private LocalDateTime startDate;
    private int totalDays;  // 기존 endDate 제거 -> totalDays 추가
    private int transportationType; // 교통수단 추가
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String role;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isShared;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean hasGuest;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ItineraryRegionDTO> regions;  // 지역 리스트 추가

    // entity -> response dto 변환 (READ: 내 일정 리스트 조회)
    public static ItineraryResponseDTO from(Itinerary itinerary, String role, boolean isShared, boolean hasGuest, List<ItineraryRegionDTO> regions) {
        return new ItineraryResponseDTO(
                itinerary.getId(),
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getTotalDays(),
                itinerary.getTransportationType(),
                itinerary.getCreatedDate(),
                itinerary.getModifiedDate(),
                role,
                isShared,
                hasGuest,
                regions
        );
    }

    // entity -> response dto 변환 (READ: 특정 일정 조회 - Events 포함)
    public static ItineraryResponseDTO from(ItineraryCollaborator collaborator, List<ItineraryRegionDTO> regions) {
        Itinerary itinerary = collaborator.getItinerary();
        return new ItineraryResponseDTO(
                itinerary.getId(),
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getTotalDays(),
                itinerary.getTransportationType(),
                itinerary.getCreatedDate(),
                itinerary.getModifiedDate(),
                collaborator.getIcRole(),
                null,
                null,
                regions
        );
    }

    // 기존 from 유지 (기존 호환성 필요 시)
    public static ItineraryResponseDTO from(Itinerary itinerary, String role, boolean isShared, boolean hasGuest) {
        return new ItineraryResponseDTO(
                itinerary.getId(),
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getTotalDays(),
                itinerary.getTransportationType(),
                itinerary.getCreatedDate(),
                itinerary.getModifiedDate(),
                role,
                isShared,
                hasGuest,
                null
        );
    }

    public static ItineraryResponseDTO from(ItineraryCollaborator collaborator) {
        Itinerary itinerary = collaborator.getItinerary();
        return new ItineraryResponseDTO(
                itinerary.getId(),
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getTotalDays(),
                itinerary.getTransportationType(),
                itinerary.getCreatedDate(),
                itinerary.getModifiedDate(),
                collaborator.getIcRole(),
                null,
                null,
                null
        );
    }
}

