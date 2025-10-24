package com.hanmo.flowplan.global.jwt;

public final class JwtConstant {
    private JwtConstant() {}
    public static final String GRANT_TYPE = "Bearer";
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 60 * 60 * 1000L;        // 1h
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L; // 7d
}
