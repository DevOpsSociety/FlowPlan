package com.hanmo.flowplan.global.config;

import com.hanmo.flowplan.global.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http
              .csrf(csrf -> csrf.disable())
              .formLogin(form -> form.disable())
              .httpBasic(basic -> basic.disable())
              .cors(Customizer.withDefaults())
              .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .authorizeHttpRequests(auth -> auth
                      .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                      // OAuth2 관련 경로는 공개
                      .requestMatchers(
                          "/api/swagger-ui/index.html",
                          "/api/auth/google/login",
                          "/swagger-ui.html",
                          "/swagger-ui/**",
                          "/v3/api-docs/**",
                          "/actuator/**"
                      ).permitAll()
                      // 나머지 요청은 인증 필요
                      .anyRequest().authenticated()

              ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);;

      // OAuth2 설정은 나중 단계에서 아래 블록을 추가
      // .oauth2Login(oauth -> oauth
      //     .userInfoEndpoint(u -> u.userService(oAuthUserService))
      //     .successHandler(oAuthSuccessHandler)
      // );
      return http.build();
  }


}
