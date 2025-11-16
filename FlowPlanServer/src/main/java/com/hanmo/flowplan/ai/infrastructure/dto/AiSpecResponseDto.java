package com.hanmo.flowplan.ai.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// ⭐️ 2. AI로부터 "마크다운 명세서"를 돌려받는 DTO
public record AiSpecResponseDto(
    @JsonProperty("markdown_spec")
    String markdownSpec // ⭐️ 생성된 마크다운 텍스트
) {}