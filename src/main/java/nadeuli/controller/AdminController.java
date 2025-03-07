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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping()
    public String adminHome() {
        return "admin-template";
    }

    @GetMapping("/{page}")
    public String loadAdminPage(@PathVariable String page) {
        return "admin-" + page;
    }
}
