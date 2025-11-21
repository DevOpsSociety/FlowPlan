package com.hanmo.flowplan.project.application.dto;

import com.hanmo.flowplan.project.domain.Project;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectListResponse(
    Long id,
    String projectName,
    String projectType,
    LocalDate startDate,
    LocalDate endDate,
    int memberCount,
    LocalDateTime updatedAt // 최신순 정렬 기준
) {
  public static ProjectListResponse from(Project project) {
    return new ProjectListResponse(
        project.getId(),
        project.getProjectName(),
        project.getProjectType(),
        project.getStartDate(),
        project.getEndDate(),
        project.getProjectMembers().size(),
        project.getUpdatedAt()
    );
  }
}