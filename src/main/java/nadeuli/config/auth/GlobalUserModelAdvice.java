package nadeuli.config.auth;

import nadeuli.entity.User;
import nadeuli.security.CustomUserDetails;
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