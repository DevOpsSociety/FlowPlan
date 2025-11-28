package com.hanmo.flowplan.user.application;

import com.hanmo.flowplan.global.jwt.JwtProvider;
import com.hanmo.flowplan.global.jwt.JwtToken;
import com.hanmo.flowplan.user.application.dto.AuthResult;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final GoogleIdTokenVerifierService googleIdTokenVerifierService;
    private final RefreshTokenStore refreshTokenStore; // 아래에 간단 구현 포함

    @Transactional
    public AuthResult loginWithGoogle(String idToken) {
        GoogleUserInfo info = googleIdTokenVerifierService.verify(idToken);

        var existing = userRepository.findByGoogleId(info.userId());
        boolean isNew = existing.isEmpty();

        User user = existing.orElseGet(() -> userRepository.save(
                User.builder()
                        .googleId(info.userId())
                        .email(info.email())
                        .name(info.name())
                        .build()
        ));

        JwtToken jwt = jwtProvider.issueToken(user.getGoogleId());
        refreshTokenStore.save(user.getGoogleId(), jwt.refreshToken());

        return new AuthResult(user.getId(), user.getEmail(), user.getName(), isNew, jwt);
    }

    // 단순 이름 추출(임시). 필요 시 Google UserInfo에 name 추가 검증해서 넣자.
    private String extractNameFromEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    // 재발급
    public JwtToken reissueTokens(String refreshToken) {
        jwtProvider.assertRefreshToken(refreshToken);
        String googleId = jwtProvider.getGoogleIdFromToken(refreshToken);

        String stored = refreshTokenStore.get(googleId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new IllegalArgumentException("저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        JwtToken t = jwtProvider.reissue(refreshToken);
        refreshTokenStore.save(googleId, t.refreshToken());
        return t;
    }

    // 로그아웃 (AT 무효화는 보통 블랙리스트 or 만료 대기. 여기선 RT 삭제만)
    public void logout(String accessToken) {
        if (accessToken == null) return;
        String googleId = jwtProvider.getGoogleIdFromToken(accessToken);
        refreshTokenStore.delete(googleId);
        // TODO: 필요 시 AccessToken 블랙리스트 처리
    }
}
