/* ItineraryResponseDTO.java
 * 수정시간, 생성시간을 포함한 일정을 리턴하기 위한 Response DTO
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철    2025.02.25     최초 작성
 * 박한철    2025.02.26     DTO 변환방식 오버로딩 방식으로 추가
 * ========================================================
 */
package nadeuli.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryCollaborator;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryResponseDTO {private Long id;
    private String itineraryName;
    private LocalDateTime startDate;
    private int totalDays;  // 기존 endDate 제거 -> totalDays 추가
    private int transportationType; // 교통수단 추가
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String role;


    // entity -> response dto 변환 (READ: 내 일정 리스트 조회)
    public static ItineraryResponseDTO from(Itinerary itinerary, String role) {
        return new ItineraryResponseDTO(
                itinerary.getId(),
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getTotalDays(),  // totalDays 사용
                itinerary.getTransportationType(), // transportationType 사용
                itinerary.getCreatedDate(),
                itinerary.getModifiedDate(),
                role
        );
    }

    // entity -> response dto 변환 (READ: 특정 일정 조회 - Events 포함)
    public static ItineraryResponseDTO from(ItineraryCollaborator collaborator) {
        Itinerary itinerary = collaborator.getItinerary();
        return new ItineraryResponseDTO(
                itinerary.getId(),
                itinerary.getItineraryName(),
                itinerary.getStartDate(),
                itinerary.getTotalDays(),  // totalDays 사용
                itinerary.getTransportationType(), // transportationType 사용
                itinerary.getCreatedDate(),
                itinerary.getModifiedDate(),
                collaborator.getIcRole()
        );
    }
}

