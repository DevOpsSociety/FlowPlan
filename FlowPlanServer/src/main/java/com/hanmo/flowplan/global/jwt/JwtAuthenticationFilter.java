package com.hanmo.flowplan.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

    // 1. Request Header에서 Access Token 추출
    String at = jwtProvider.resolveAccessToken(request);
    // 2. 토큰이 유효한지 검증
    if (at != null) {
      try {
        jwtProvider.validate(at);
        Authentication auth = jwtProvider.getAuthentication(at);
        SecurityContextHolder.getContext().setAuthentication(auth);

      } catch (IllegalArgumentException | IllegalStateException e) {
        SecurityContextHolder.clearContext();
      }
    }
    // 6. 다음 필터로 진행
    filterChain.doFilter(request, response);
  }
}
