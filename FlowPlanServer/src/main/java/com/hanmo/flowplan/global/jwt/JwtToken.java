package com.hanmo.flowplan.global.jwt;

public record JwtToken(String grantType, String accessToken, String refreshToken) {}
