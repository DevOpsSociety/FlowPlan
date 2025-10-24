package com.hanmo.flowplan.user.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleIdTokenVerifierService {

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("유효하지 않은 Google ID Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String sub = payload.getSubject(); // googleId
            String email = payload.getEmail();

            return new GoogleUserInfo(sub, email);
        } catch (Exception e) {
            throw new IllegalStateException("Google ID Token 검증 실패", e);
        }
    }
}
