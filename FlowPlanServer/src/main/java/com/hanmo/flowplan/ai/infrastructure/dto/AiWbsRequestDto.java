package com.hanmo.flowplan.ai.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// ⭐️ 3-1. "수정된 마크다운"을 AI에게 다시 보낼 때
public record AiWbsRequestDto(
    @JsonProperty("markdown_spec")
    String markdownSpec
) {}