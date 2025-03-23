package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.security.CustomUserDetails;
import nadeuli.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile() {
        User user = extractAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(401).body(null);
        }

        String profileImage = user.getProfileImage();
        UserDTO userDTO = new UserDTO(user.getUserName(), user.getUserEmail(), profileImage);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * ✅ 프로필 사진 업로드
     */
    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        User user = extractAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        String newProfileUrl = s3Service.uploadProfileImage(file, user.getProfileImage());
        user.updateProfile(user.getUserName(), newProfileUrl, user.getProvider(), user.getUserToken(), user.getLastLoginAt());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "profileImage", newProfileUrl));
    }

    /**
     * ✅ 인증된 User 꺼내는 공통 메서드
     */
    private User extractAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        return null;
    }
}
