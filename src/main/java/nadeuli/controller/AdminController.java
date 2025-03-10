/* AdminController.java
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-03-07
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자       날짜       수정 / 보완 내용
 * ========================================================
 * 박한철    2025.03.07   어드민 관리 페이지
 *
 * ========================================================
 */

package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import nadeuli.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping
    public String adminHome(Model model) {
        model.addAttribute("adminContent", null);
        model.addAttribute("adminScript", null); // 기본 페이지에서는 JS 없음
        model.addAttribute("adminStyle", null);  // 기본 페이지에서는 개별 CSS 없음
        return "/admin/admin-template";
    }

    @GetMapping("/region")
    public String loadRegionPage(Model model) {
        model.addAttribute("adminContent", "/admin/admin-region"); // 오른쪽 패널 변경
        model.addAttribute("adminScript", "/js/admin-region.js");  // JS 추가
        model.addAttribute("adminStyle", "/css/admin-region.css"); // 동적 CSS 추가
        return "/admin/admin-template";
    }


}
