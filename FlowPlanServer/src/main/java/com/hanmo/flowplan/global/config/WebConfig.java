package com.hanmo.flowplan.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // 1. 프론트엔드 주소 허용 (배포된 URL 추가)
    config.setAllowedOrigins(List.of(
        "http://localhost:3000",
        "https://localhost:3000",
        "http://127.0.0.1:3000",
        "https://127.0.0.1:3000",
        "https://flowplan-ai.vercel.app"
    ));

    // 2. 허용할 HTTP 메소드
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    // 3. 허용할 요청 헤더
    config.setAllowedHeaders(List.of("*"));

    // 4. (핵심) 프론트가 읽을 수 있도록 노출할 헤더
    config.setExposedHeaders(List.of("Authorization", "Refresh-Token"));

    // 5. 쿠키/인증 정보 허용
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
