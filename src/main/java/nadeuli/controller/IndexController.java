/*
 * IndexController.java
 * nadeuli Service - 여행
 * 인덱스(메인) 페이지 컨트롤러
 * 회원 정보 전달을 위한 ControllerAdvice 대상 컨트롤러로 명시
 * 작성자 : 박한철
 * 최초 작성 일자 : 2025.03.19
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 박한철      2025.03.19     최초 작성 및 index 페이지 매핑
 * ========================================================
 */
package nadeuli.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequiredArgsConstructor
public class IndexController {

    @GetMapping("/")
    public String indexPage(Model model) {
        return "index";
    }
}
