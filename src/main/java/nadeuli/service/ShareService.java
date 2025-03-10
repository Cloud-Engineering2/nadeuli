/* ShareService.java
 * ShareService
 * 공유 정보 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-06
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철     2025.03.07    최초작성
 *
 * ========================================================
 */

package nadeuli.service;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.ItineraryDTO;
import nadeuli.dto.response.CollaboratorResponse;
import nadeuli.dto.response.ItineraryStatusResponse;
import nadeuli.entity.Itinerary;
import nadeuli.entity.ItineraryCollaborator;
import nadeuli.entity.ShareToken;
import nadeuli.entity.User;
import nadeuli.repository.ItineraryCollaboratorRepository;
import nadeuli.repository.ItineraryRepository;
import nadeuli.repository.ShareTokenRepository;
import nadeuli.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final HashIdService hashIdService;
    private final ShareTokenRepository shareTokenRepository;
    private final ItineraryCollaboratorRepository itineraryCollaboratorRepository;
    private final UserRepository userRepository;
    private final ItineraryRepository itineraryRepository;

    /**
     * 일정 공유 토큰 생성 (OWNER만 가능)
     * 기존 공유 링크가 있으면 기존 링크 반환
     */
    @Transactional
    public String createShareToken(Long itineraryId, Long userId) {
        // 1️⃣ 해당 유저가 OWNER인지 확인
        boolean isOwner = itineraryCollaboratorRepository.existsByUserIdAndItineraryIdAndIcRole(userId, itineraryId, "ROLE_OWNER");

        if (!isOwner) {
            throw new IllegalArgumentException("해당 일정의 OWNER가 아닙니다.");
        }

        // 2️⃣ 기존 공유 링크가 있는지 확인
        Optional<ShareToken> existingToken = shareTokenRepository.findByItineraryId(itineraryId);
        if (existingToken.isPresent()) {
            // 기존 토큰이 있으면 해당 토큰 반환
            return hashIdService.encode(itineraryId) + "-" + existingToken.get().getUuid();
        }

        // 3️⃣ 기존 토큰이 없으면 새로 생성
        String hashId = hashIdService.encode(itineraryId);
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // 4️⃣ 새 공유 토큰 저장 (TTL 24시간 설정 가능)
        ShareToken shareToken = new ShareToken(uuid, itineraryId, 1440); // 1440분 = 24시간
        shareTokenRepository.save(shareToken);

        // 5️⃣ "hashid-uuid" 형식의 토큰 반환
        return hashId + "-" + uuid;
    }

    /**
     * 공유 토큰 조회 (ROLE_OWNER 또는 ROLE_GUEST만 접근 가능)
     */
    public String getShareToken(Long itineraryId, Long userId) {
        // 1️⃣ 사용자가 일정의 OWNER 또는 GUEST인지 확인
        boolean isAuthorized = itineraryCollaboratorRepository.existsByUserIdAndItineraryIdAndIcRole(userId, itineraryId, "ROLE_OWNER") ||
                itineraryCollaboratorRepository.existsByUserIdAndItineraryIdAndIcRole(userId, itineraryId, "ROLE_GUEST");

        if (!isAuthorized) {
            throw new IllegalArgumentException("해당 일정의 공유 권한이 없습니다.");
        }

        // 2️⃣ 공유 토큰 조회
        Optional<ShareToken> existingToken = shareTokenRepository.findByItineraryId(itineraryId);
        if (existingToken.isEmpty()) {
            throw new IllegalArgumentException("해당 일정에 대한 공유 링크가 존재하지 않습니다.");
        }

        // 3️⃣ "hashid-uuid" 형식의 토큰 반환
        return hashIdService.encode(itineraryId) + "-" + existingToken.get().getUuid();
    }

    /**
     * 공유 토큰 삭제 (ROLE_OWNER만 가능)
     */
    public void deleteShareToken(Long itineraryId, Long userId) {
        // 1️⃣ 해당 사용자가 OWNER인지 확인
        boolean isOwner = itineraryCollaboratorRepository.existsByUserIdAndItineraryIdAndIcRole(userId, itineraryId, "ROLE_OWNER");

        if (!isOwner) {
            throw new IllegalArgumentException("해당 일정의 OWNER만 공유 링크를 삭제할 수 있습니다.");
        }

        // 2️⃣ 공유 토큰 존재 여부 확인
        Optional<ShareToken> existingToken = shareTokenRepository.findByItineraryId(itineraryId);
        if (existingToken.isEmpty()) {
            throw new IllegalArgumentException("해당 일정에 대한 공유 링크가 존재하지 않습니다.");
        }

        // 3️⃣ 공유 토큰 삭제
        shareTokenRepository.delete(existingToken.get());
    }

    /**
     * 공유 링크를 통해 Collaborator 등록 (ROLE_GUEST)
     */
    public String joinItinerary(String shareToken, Long userId) {
        // 1️⃣ 공유 토큰 검증
        String[] parts = shareToken.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("잘못된 공유 링크입니다.");
        }

        String hashId = parts[0];
        String uuid = parts[1];

        // 2️⃣ itineraryId 복원
        Long itineraryId = hashIdService.decode(hashId);

        // 3️⃣ 공유 토큰이 실제로 존재하는지 확인
        Optional<ShareToken> token = shareTokenRepository.findByItineraryId(itineraryId);
        if (token.isEmpty() || !token.get().getUuid().equals(uuid)) {
            throw new IllegalArgumentException("유효하지 않은 공유 링크입니다.");
        }

        // 4️⃣ 이미 등록된 Collaborator인지 확인
        if (itineraryCollaboratorRepository.existsByUserIdAndItineraryId(userId, itineraryId)) {
            throw new IllegalArgumentException("이미 일정에 참여 중입니다.");
        }

        // 5️⃣ 사용자 및 일정 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        // 6️⃣ Collaborator 추가 (ROLE_GUEST)
        ItineraryCollaborator collaborator = new ItineraryCollaborator(null, user, itinerary, "ROLE_GUEST");
        itineraryCollaboratorRepository.save(collaborator);

        return "일정에 성공적으로 참여하였습니다.";
    }
    /**
     * 일정에서 특정 사용자를 Collaborator에서 제거 (OWNER만 가능)
     */
    @Transactional
    public String removeCollaborator(Long itineraryId, Long userId, Long targetUserId) {

        // 1️⃣ 요청한 사용자가 OWNER인지 확인
        boolean isOwner = itineraryCollaboratorRepository.existsByUserIdAndItineraryIdAndIcRole(userId, itineraryId, "ROLE_OWNER");

        // 2️⃣ 대상 사용자가 일정에 참여 중인지 확인
        boolean isParticipant = itineraryCollaboratorRepository.existsByUserIdAndItineraryId(targetUserId, itineraryId);
        if (!isParticipant) {
            throw new IllegalArgumentException("해당 사용자는 일정에 참여하고 있지 않습니다.");
        }

        // 3️⃣ 대상 사용자가 OWNER인지 확인 (OWNER는 삭제 불가능)
        boolean isTargetOwner = itineraryCollaboratorRepository.existsByUserIdAndItineraryIdAndIcRole(targetUserId, itineraryId, "ROLE_OWNER");
        if (isTargetOwner) {
            throw new IllegalArgumentException("OWNER는 삭제할 수 없습니다.");
        }

        // 4️⃣ 자신을 삭제하려는 경우 허용 (GUEST가 자기 자신을 삭제 가능)
        if (userId.equals(targetUserId)) {
            itineraryCollaboratorRepository.deleteByUserIdAndItineraryId(targetUserId, itineraryId);
            return "스스로 일정에서 나갔습니다.";
        }

        // 5️⃣ OWNER만 다른 사용자를 삭제할 수 있음
        if (!isOwner) {
            throw new IllegalArgumentException("해당 일정의 OWNER만 다른 사용자를 삭제할 수 있습니다.");
        }

        // 6️⃣ 삭제 수행
        itineraryCollaboratorRepository.deleteByUserIdAndItineraryId(targetUserId, itineraryId);
        return "해당 사용자가 일정에서 삭제되었습니다.";
    }






    /**
     * 특정 일정(lid)의 공유 상태 & 게스트 존재 여부 조회
     */
    public ItineraryStatusResponse getItineraryStatus(Long userId, Long itineraryId) {
        // 1️⃣ 사용자가 OWNER 또는 GUEST인지 확인
        Optional<ItineraryCollaborator> userRoleOpt = itineraryCollaboratorRepository.findByUserIdAndItineraryId(userId, itineraryId);
        if (userRoleOpt.isEmpty()) {
            throw new IllegalArgumentException("해당 일정에 참여한 사용자만 조회할 수 있습니다.");
        }

        // 2️⃣ 현재 사용자의 역할
        String userRole = userRoleOpt.get().getIcRole();

        // 3️⃣ 공유 상태 및 게스트 존재 여부 조회
        boolean isShared = shareTokenRepository.existsByItineraryId(itineraryId);
        boolean hasGuest = itineraryCollaboratorRepository.existsByItineraryIdAndIcRole(itineraryId, "ROLE_GUEST");

        // 4️⃣ Collaborator 목록 조회
        List<CollaboratorResponse> collaborators = itineraryCollaboratorRepository.findCollaboratorsByItineraryId(itineraryId)
                .stream()
                .map(CollaboratorResponse::from)
                .collect(Collectors.toList());

        return new ItineraryStatusResponse(isShared, hasGuest, userRole, collaborators);
    }

    /**
     * 토큰으로 Itinerary 간단 정보 조회
     */
    public ItineraryDTO getItineraryFromToken(String shareToken) {
        String[] parts = shareToken.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("잘못된 공유 링크입니다.");
        }

        String hashId = parts[0];
        String uuid = parts[1];

        // 2️⃣ itineraryId 복원
        Long itineraryId = hashIdService.decode(hashId);

        Optional<ShareToken> token = shareTokenRepository.findByItineraryId(itineraryId);
        if (token.isEmpty() || !token.get().getUuid().equals(uuid)) {
            throw new IllegalArgumentException("유효하지 않은 공유 링크입니다.");
        }

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        return ItineraryDTO.from(itinerary);
    }


}
