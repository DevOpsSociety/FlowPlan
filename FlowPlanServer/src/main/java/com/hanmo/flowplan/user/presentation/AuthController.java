package com.hanmo.flowplan.user.presentation;

import com.hanmo.flowplan.global.jwt.JwtToken;
import com.hanmo.flowplan.user.application.GoogleOAuthService;
import com.hanmo.flowplan.user.application.dto.AuthResult;
import com.hanmo.flowplan.user.presentation.dto.GoogleLoginDTO;
import com.hanmo.flowplan.user.presentation.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth API", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final GoogleOAuthService googleOAuthService;

    @Operation(summary = "구글 로그인", description = "구글 로그인으로 신규 가입 또는 로그인")
    @PostMapping("/google/login")
    public ResponseEntity<UserResponseDTO> login(@RequestBody GoogleLoginDTO request) {
        AuthResult result = googleOAuthService.loginWithGoogle(request.idToken());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + result.tokens().accessToken());
        headers.set("Refresh-Token", result.tokens().refreshToken());

        UserResponseDTO body = new UserResponseDTO(
                result.userId(),
                result.email(),
                result.name(),
                result.isNewUser()
        );

        return ResponseEntity.ok().headers(headers).body(body);
    }


    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(@RequestHeader("Refresh-Token") String rt) {
        JwtToken t = googleOAuthService.reissueTokens(rt);
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + t.accessToken());
        h.set("Refresh-Token", t.refreshToken());
        return ResponseEntity.ok().headers(h).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value="Authorization", required=false) String bearer) {
        String at = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
        googleOAuthService.logout(at);
        return ResponseEntity.noContent().build();
    }
}
