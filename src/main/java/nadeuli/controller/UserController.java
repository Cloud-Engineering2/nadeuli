package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.dto.UserDTO;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.security.CustomUserDetails;
import nadeuli.service.S3Service;
import nadeuli.common.PhotoType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/user")
public class UserController {

    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 🔹 현재 로그인된 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            log.warn("🚨 인증 정보 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        log.info("✅ 현재 로그인된 사용자: {}", user.getUserEmail());

        return ResponseEntity.ok(UserDTO.from(user));
    }

    /**
     * 🔹 회원 탈퇴 기능
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            log.warn("🚨 인증 정보 없음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        Optional<User> userOptional = userRepository.findByUserEmail(email);

        if (userOptional.isEmpty()) {
            log.warn("🚨 사용자를 찾을 수 없음: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }

        User user = userOptional.get();
        user.updateRefreshToken(null, null); // 🔹 회원 탈퇴 시 리프레시 토큰 삭제
        userRepository.save(user);
        userRepository.delete(user);
        log.info("✅ 회원 탈퇴 완료 (리프레시 토큰 삭제됨): {}", email);

        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
    }

    /**
     * 🔹 프로필 사진 업로드 기능
     */
    @PostMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == "anonymousUser") {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증되지 않은 사용자입니다."));
        }

        String email = authentication.getName();
        Optional<User> userOptional = userRepository.findByUserEmail(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }

        User user = userOptional.get();

        // 기존 프로필 사진 삭제 (기본 이미지가 아닐 경우)
        if (user.getProfileUrl() != null && !user.getProfileUrl().contains("default_profile.png")) {
            s3Service.deleteFile(user.getProfileUrl());
        }

        // 새로운 프로필 사진 업로드
        String newProfileUrl = s3Service.uploadFile(file, PhotoType.PROFILE);
        user.setProfileUrl(newProfileUrl);
        userRepository.save(user);

        log.info("✅ 프로필 사진 업데이트 완료 - 사용자: {}", email);

        return ResponseEntity.ok(Map.of("message", "프로필 사진이 업데이트되었습니다.", "profileUrl", newProfileUrl));
    }
}