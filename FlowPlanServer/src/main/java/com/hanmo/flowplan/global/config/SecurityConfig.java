package com.hanmo.flowplan.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http
              .csrf(csrf -> csrf.disable())
              .formLogin(form -> form.disable())
              .httpBasic(basic -> basic.disable())
              .cors(Customizer.withDefaults())
              .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .authorizeHttpRequests(auth -> auth
                      // OAuth2 관련 경로는 공개
                      .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()
                      .requestMatchers(
                          "/swagger-ui.html",
                          "/swagger-ui/**",
                          "/v3/api-docs/**",
                          "/actuator/**"
                      ).permitAll()
                      // 나머지 요청은 인증 필요
                      .anyRequest().authenticated()
              );

      // OAuth2 설정은 나중 단계에서 아래 블록을 추가
      // .oauth2Login(oauth -> oauth
      //     .userInfoEndpoint(u -> u.userService(oAuthUserService))
      //     .successHandler(oAuthSuccessHandler)
      // );
      return http.build();
  }


}
