package com.hanmo.flowplan.user.application.dto;

import com.hanmo.flowplan.global.jwt.JwtToken;

public record AuthResult(
        Long userId,
        String email,
        String name,
        boolean isNewUser,
        JwtToken tokens
) {}
