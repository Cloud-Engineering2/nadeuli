package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor
public class ProfileController {

    private final S3Service s3Service;
    private final UserRepository userRepository;

    /**
     * ✅ 사용자 프로필 정보 가져오기
     * - `/auth/user/profile` 엔드포인트 사용 (기존 `/auth/user/me` 충돌 해결)
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(@AuthenticationPrincipal User user) {
        // 기본 프로필이 없으면 카카오/구글 기본 이미지 사용
        String profileImage = user.getProfileImage();
        if (profileImage == null || profileImage.trim().isEmpty()) {
            if ("KAKAO".equals(user.getProvider())) {
                profileImage = user.getProfileImage();
            } else if ("GOOGLE".equals(user.getProvider())) {
                profileImage = user.getProfileImage();
            }
        }

        UserDTO userDTO = new UserDTO(user.getUserName(), user.getUserEmail(), profileImage);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * ✅ 프로필 사진 업로드 (기존 사진 자동 삭제 후 새로운 사진 저장)
     */
    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {

        // S3에 프로필 업로드 (기존 사진 자동 삭제)
        String newProfileUrl = s3Service.uploadProfileImage(file, user.getProfileImage());

        // 프로필 정보 업데이트
        user.updateProfile(user.getUserName(), newProfileUrl, user.getProvider(), user.getUserToken(), user.getLastLoginAt());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "profileImage", newProfileUrl));
    }
}
