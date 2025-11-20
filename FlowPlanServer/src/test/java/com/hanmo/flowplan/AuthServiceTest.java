package com.hanmo.flowplan;

import com.hanmo.flowplan.global.jwt.JwtProvider;
import com.hanmo.flowplan.global.jwt.JwtToken;
import com.hanmo.flowplan.user.application.dto.AuthResult;
import com.hanmo.flowplan.user.application.GoogleIdTokenVerifierService;
import com.hanmo.flowplan.user.application.GoogleOAuthService;
import com.hanmo.flowplan.user.application.GoogleUserInfo;
import com.hanmo.flowplan.user.application.RefreshTokenStore;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtProvider jwtProvider;
    @Mock GoogleIdTokenVerifierService googleIdTokenVerifierService;
    @Mock RefreshTokenStore refreshTokenStore;

    @InjectMocks GoogleOAuthService service;

    private final String ID_TOKEN = "id-token";
    private final String GOOGLE_USER_ID = "gid-123";
    private final String EMAIL = "dh@example.com";
    private final String NAME = "dh";
    private final JwtToken JWT = new JwtToken("Bearer", "AT-xxx", "RT-yyy");

    @Test
    @DisplayName("[loginWithGoogle] 신규 사용자라면 유저를 저장하고 토큰 발급 및 RT 저장 후 AuthResult를 반환한다.")
    void loginWithGoogle_신규사용자면_저장하고_RT저장_토큰반환() {
        // given
        given(googleIdTokenVerifierService.verify(ID_TOKEN))
                .willReturn(new GoogleUserInfo(GOOGLE_USER_ID, EMAIL, NAME));

        User savedUser = User.builder()
                .googleId(GOOGLE_USER_ID)
                .email(EMAIL)
                .name(NAME)
                .build();

        given(userRepository.findByGoogleId(GOOGLE_USER_ID)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtProvider.issueToken(EMAIL)).willReturn(JWT);

        // when
        AuthResult result = service.loginWithGoogle(ID_TOKEN);

        // then: 저장 파라미터 검증
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User toSave = captor.getValue();
        assertThat(toSave.getGoogleId()).isEqualTo(GOOGLE_USER_ID);
        assertThat(toSave.getEmail()).isEqualTo(EMAIL);
        assertThat(toSave.getName()).isEqualTo(NAME);

        // RT 저장
        verify(refreshTokenStore).save(EMAIL, JWT.refreshToken());

        // 반환값 검증(헤더 X, 순수 결과만)
        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.name()).isEqualTo(NAME);
        assertThat(result.isNewUser()).isTrue();
        assertThat(result.tokens().accessToken()).isEqualTo("AT-xxx");
        assertThat(result.tokens().refreshToken()).isEqualTo("RT-yyy");
    }

    @Test
    @DisplayName("[loginWithGoogle] 기존 사용자라면 save() 없이 토큰만 발급하고 RT를 저장한다.")
    void loginWithGoogle_기존사용자면_save_없이_RT저장_토큰반환() {
        // given
        given(googleIdTokenVerifierService.verify(ID_TOKEN))
                .willReturn(new GoogleUserInfo(GOOGLE_USER_ID, EMAIL, NAME));

        User existing = User.builder()
                .googleId(GOOGLE_USER_ID)
                .email(EMAIL)
                .name(NAME)
                .build();

        given(userRepository.findByGoogleId(GOOGLE_USER_ID)).willReturn(Optional.of(existing));
        given(jwtProvider.issueToken(EMAIL)).willReturn(JWT);

        // when
        AuthResult result = service.loginWithGoogle(ID_TOKEN);

        // then
        verify(userRepository, never()).save(any());
        verify(refreshTokenStore).save(EMAIL, JWT.refreshToken());

        assertThat(result.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("[reissueTokens] 리프레시 토큰이 유효하면 새 토큰을 재발급하고 RT를 갱신한다.")
    void reissueTokens_정상_재발급_RT_갱신() {
        // given
        String oldRT = "RT-old";
        String newAT = "AT-new";
        String newRT = "RT-new";
        JwtToken newTokens = new JwtToken("Bearer", newAT, newRT);

        willDoNothing().given(jwtProvider).assertRefreshToken(oldRT);
        given(jwtProvider.getGoogleIdFromToken(oldRT)).willReturn(GOOGLE_USER_ID);
        given(refreshTokenStore.get(GOOGLE_USER_ID)).willReturn(oldRT);
        given(jwtProvider.reissue(oldRT)).willReturn(newTokens);

        // when
        JwtToken issued = service.reissueTokens(oldRT);

        // then
        assertThat(issued.accessToken()).isEqualTo(newAT);
        assertThat(issued.refreshToken()).isEqualTo(newRT);
        verify(refreshTokenStore).save(EMAIL, newRT);
    }

    @Test
    @DisplayName("[reissueTokens] 저장된 RT와 불일치하면 예외를 던진다.")
    void reissueTokens_RT_불일치면_예외() {
        // given
        String oldRT = "RT-old";
        willDoNothing().given(jwtProvider).assertRefreshToken(oldRT);
        given(jwtProvider.getGoogleIdFromToken(oldRT)).willReturn(GOOGLE_USER_ID);
        given(refreshTokenStore.get(EMAIL)).willReturn("DIFFERENT-RT");

        // when / then
        assertThatThrownBy(() -> service.reissueTokens(oldRT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("리프레시 토큰과 일치");

        verify(jwtProvider, never()).reissue(anyString());
        verify(refreshTokenStore, never()).save(anyString(), anyString());
    }

    @Test
    @DisplayName("[logout] AT가 존재하면 해당 이메일의 RT를 삭제한다.")
    void logout_AT있으면_RT삭제() {
        // given
        String accessToken = "AT-xxx";
        given(jwtProvider.getGoogleIdFromToken(accessToken)).willReturn(GOOGLE_USER_ID);

        // when
        service.logout(accessToken);

        // then
        verify(refreshTokenStore).delete(EMAIL);
    }

    @Test
    @DisplayName("[logout] AT가 null이면 아무 동작도 하지 않는다.")
    void logout_AT없으면_아무것도_안함() {
        // when
        service.logout(null);

        // then
        verifyNoInteractions(refreshTokenStore);
    }
}
