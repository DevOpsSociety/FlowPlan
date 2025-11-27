package com.hanmo.flowplan.global.annotation;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) // 파라미터에만 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 유지됨
@AuthenticationPrincipal(expression = "googleId")
@Parameter(hidden = true)
public @interface CurrentUserId {
}
