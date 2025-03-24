/* UserService.java
 * User 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 고민정   2025.03.24    user 조회 메서드
 *
 * ========================================================
 */
package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.UserDTO;
import nadeuli.entity.ItineraryCollaborator;
import nadeuli.entity.User;
import nadeuli.repository.ItineraryCollaboratorRepository;
import nadeuli.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;

    public UserDTO retrieveUser(Integer iid) {
        Long itineraryId = Long.valueOf(iid);
        ItineraryCollaborator itineraryCollaborator = itineraryCollaboratorRepository.findCollaboratorsByItineraryId(itineraryId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 ItineraryCollaborator가 없습니다."));
        User user = itineraryCollaborator.getUser();
        return UserDTO.from(user);
    }
}
