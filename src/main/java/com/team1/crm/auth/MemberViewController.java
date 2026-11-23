
package com.team1.crm.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * + 회원 로그인 및 회원가입 화면 컨트롤러
 * 역할: 사용자의 로그인 및 회원가입 페이지 요청을 처리하고,
 *      해당하는 HTML 화면(Thymeleaf 템플릿)을 반환합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Controller // HTML 화면을 반환하는 스프링 MVC 컨트롤러
public class MemberViewController {

    /**
     * + 로그인 화면 이동
     * 역할: /login 주소로 요청이 들어오면
     *      로그인 화면(auth/login.html)을 반환합니다.
     */

    // 로그인 화면
    @GetMapping("/login") // GET /login
    public String login() {
        return "auth/login";
    } // templates/auth/login.html

    /**
     * + 회원가입 화면 이동
     * 역할: /signup 주소로 요청이 들어오면
     *      회원가입 화면(auth/signup.html)을 반환합니다.
     */
    @GetMapping("/signup") // GET /signup
    public String signup() {
        return "auth/signup";
    }  // templates/auth/signup.html
}
