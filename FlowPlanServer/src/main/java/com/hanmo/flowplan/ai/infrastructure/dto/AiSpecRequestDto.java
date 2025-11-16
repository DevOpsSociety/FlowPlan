package com.hanmo.flowplan.ai.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// ⭐️ 1. AI에게 "마크다운 명세서"를 요청하기 위한 DTO
public record AiSpecRequestDto(
    @JsonProperty("project_name")
    String projectName,

    @JsonProperty("project_type")
    String projectType,

    @JsonProperty("team_size")
    int teamSize,

    @JsonProperty("expected_duration_days")
    int expectedDurationDays,

    @JsonProperty("start_date")
    String startDate,

    @JsonProperty("end_date")
    String endDate,

    String budget,
    String priority,
    List<String> stakeholders,
    List<String> deliverables,
    List<String> risks,

    @JsonProperty("detailed_requirements")
    String detailedRequirements
) {}