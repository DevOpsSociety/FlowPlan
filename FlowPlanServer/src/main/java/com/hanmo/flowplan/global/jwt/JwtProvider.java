package com.hanmo.flowplan.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

import static com.hanmo.flowplan.global.jwt.JwtConstant.*;

@Component
public class JwtProvider {

    private final Key key;

    public JwtProvider(@Value("${jwt.secretKey}") String base64SecretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(base64SecretKey.trim());
        if (keyBytes.length < 32) { // HS256 최소 256비트
            throw new IllegalArgumentException("jwt.secretKey must be 256-bit (Base64).");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtToken issueToken(String username) {
        String at = createAccessToken(username, "ROLE_USER");
        String rt = createRefreshToken(username);
        return new JwtToken(GRANT_TYPE, at, rt);
    }

    public JwtToken reissue(String refreshToken) {
        assertRefreshToken(refreshToken);
        String username = getUsernameFromToken(refreshToken);
        return issueToken(username);
    }

    public String createAccessToken(String username, String authorities) {
        long now = System.currentTimeMillis();
        Date exp = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        return Jwts.builder()
                .setSubject(username)
                .claim("authorities", authorities)
                .claim("type", "access")
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String username) {
        long now = System.currentTimeMillis();
        Date exp = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);
        return Jwts.builder()
                .setSubject(username)
                .claim("type", "refresh")
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public void setHeaderAccessToken(HttpServletResponse response, String accessToken) {
        response.setHeader("Authorization", GRANT_TYPE + " " + accessToken);
    }

    public void setHeaderRefreshToken(HttpServletResponse response, String refreshToken) {
        response.setHeader("Refresh-Token", refreshToken);
    }

    public void assertRefreshToken(String token) {
        validate(token);
        String type = (String) parseClaims(token).get("type");
        if (!"refresh".equals(type)) throw new IllegalArgumentException("Not a refresh token.");
    }

    public boolean validate(String token) {
        parseClaims(token);
        return true;
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String resolveAccessToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith(GRANT_TYPE + " ")) return null;
        return bearer.substring((GRANT_TYPE + " ").length());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            throw new IllegalStateException("만료된 토큰");
        } catch (JwtException e) {
            throw new IllegalArgumentException("유효하지 않은 토큰");
        }
    }
}
