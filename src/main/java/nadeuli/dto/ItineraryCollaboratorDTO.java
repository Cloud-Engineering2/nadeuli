/* ItineraryCollaboratorDTO.java
 * nadeuli Service - 여행
 * ItineraryCollaborator 관련 DTO
 * 작성자 : 이홍비
 * 최초 작성 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성
 * ========================================================
 */

package nadeuli.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nadeuli.entity.ItineraryCollaborator;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryCollaboratorDTO {
    private Integer id;
    private UserDTO userDTO;
    private ItineraryDTO itineraryDTO;
    private String icRole;


    // static factory method
    public static ItineraryCollaboratorDTO of (Integer id, UserDTO userDTO, ItineraryDTO itineraryDTO, String icRole) {
        return new ItineraryCollaboratorDTO(id, userDTO, itineraryDTO, icRole);
    }

    public static ItineraryCollaboratorDTO of (UserDTO userDTO, ItineraryDTO itineraryDTO, String icRole) {
        return new ItineraryCollaboratorDTO(null, userDTO, itineraryDTO, icRole);
    }

    // entity -> dto
    public static ItineraryCollaboratorDTO from(ItineraryCollaborator itineraryCollaborator) {
        return new ItineraryCollaboratorDTO(
                itineraryCollaborator.getId(),
                UserDTO.from(itineraryCollaborator.getUid()),
                ItineraryDTO.from(itineraryCollaborator.getIid()),
                itineraryCollaborator.getIcRole()
        );
    }

    // dto => entity
    public ItineraryCollaborator toEntity() {
        return ItineraryCollaborator.of(userDTO.toEntity(), itineraryDTO.toEntity());
    }
}
