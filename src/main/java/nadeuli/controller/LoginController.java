/*
 * LoginController.java
 * 로그인 및 마이페이지 View 매핑 컨트롤러
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성 - 로그인 및 마이페이지 매핑
 * ========================================================
 */
package nadeuli.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/mypage")
    public String myPage() {
        return "mypage";
    }
}
