package com.hanmo.flowplan.project.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record CreateProjectWithSpecResponse(
    Long projectId,
    String markdownContent
) {}
