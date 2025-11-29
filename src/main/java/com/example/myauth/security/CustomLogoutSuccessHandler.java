package com.example.myauth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security 로그아웃 성공 핸들러
 * 로그아웃 성공 시 통일된 JSON 응답을 반환한다
 */
@Slf4j
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

  /**
   * 로그아웃 성공 시 호출된다
   * 200 OK와 함께 성공 메시지를 JSON 형식으로 반환
   *
   * @param request HTTP 요청 객체
   * @param response HTTP 응답 객체
   * @param authentication 인증 정보 (null일 수 있음)
   * @throws IOException I/O 오류 발생 시
   * @throws ServletException 서블릿 오류 발생 시
   */
  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    log.info("로그아웃 성공");

    // ApiResponse 형식의 JSON 응답 직접 생성
    String jsonResponse = "{\"success\":true,\"message\":\"로그아웃이 완료되었습니다.\",\"data\":null}";

    // JSON 응답 반환
    response.setStatus(HttpStatus.OK.value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(jsonResponse);
  }
}
