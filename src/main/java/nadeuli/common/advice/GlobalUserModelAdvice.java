/*
 * GlobalUserModelAdvice.java
 * 로그인 사용자 정보를 모든 View에 공통 주입하는 ControllerAdvice
 * - 로그인 여부 및 사용자 이름, 이메일, 프로필 이미지, 권한(role) 정보를 Model에 추가
 * - 모든 페이지에서 별도 처리 없이 템플릿에서 로그인 사용자 정보 접근 가능
 *
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성 - View 공통 사용자 정보 주입 기능 구현
 * ========================================================
 */

package nadeuli.common.advice;

import nadeuli.entity.User;
import nadeuli.auth.oauth.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalUserModelAdvice {

    @ModelAttribute
    public void addUserInfo(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails userDetails) {
                User user = userDetails.getUser();

                model.addAttribute("loggedIn", true);
                model.addAttribute("userName", user.getUserName());
                model.addAttribute("userEmail", user.getUserEmail());
                model.addAttribute("profileImage", user.getProfileImage());
                model.addAttribute("roleName", user.getUserRole().name());
            } else {
                model.addAttribute("loggedIn", false);
            }
        } else {
            model.addAttribute("loggedIn", false);
        }
    }
}