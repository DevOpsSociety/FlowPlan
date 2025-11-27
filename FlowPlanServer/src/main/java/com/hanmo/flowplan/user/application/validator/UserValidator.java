package com.hanmo.flowplan.user.application.validator;

import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserValidator {

  private final UserRepository userRepository;

  public User validateAndGetUser(String googleId) {
    return userRepository.findByGoogleId(googleId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with googleId: " + googleId));
  }
}
