package com.hanmo.flowplan.global.jwt;

import com.hanmo.flowplan.user.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails  implements UserDetails {

  private final User user;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
  }

  // ⭐️ 우리가 원하는 'googleId'를 반환하는 커스텀 메서드
  public String getGoogleId() {
    return user.getGoogleId();
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {

    return user.getGoogleId();
  }

  // ⬇️ 계정 관련 정책 (모두 true로 설정)
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
