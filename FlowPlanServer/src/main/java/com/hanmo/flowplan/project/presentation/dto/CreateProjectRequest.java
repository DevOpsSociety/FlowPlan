package com.hanmo.flowplan.project.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectPriority;
import com.hanmo.flowplan.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateProjectRequest(
    @NotBlank
    String projectName,

    @NotBlank
    String projectType,

    @NotNull
    int teamSize,

    @NotNull
    int expectedDurationMonths,

    String startDate,
    String endDate,
    BigDecimal budget,
    String priority,
    List<String> stakeholders,
    List<String> deliverables,
    List<String> risks,

    String detailedRequirements
) {

  /**
   * ⭐️ DTO -> Entity 변환 메서드
   * 레코드 내부에서도 메서드를 정의할 수 있습니다.
   */
  public Project toEntity(User owner) {
    // 1. 날짜 안전 파싱
    LocalDate start = parseSafeDate(this.startDate);
    LocalDate end = parseSafeDate(this.endDate);

    // 2. 우선순위 변환 (String -> Enum)
    ProjectPriority priorityEnum = null;
    if (this.priority != null && !this.priority.isBlank()) {
      try {
        priorityEnum = ProjectPriority.valueOf(this.priority.toUpperCase());
      } catch (IllegalArgumentException e) {
        priorityEnum = ProjectPriority.MEDIUM; // 기본값
      }
    }

    // 3. 리스트 -> 문자열 변환
    String stakeholdersStr = listToString(this.stakeholders);
    String deliverablesStr = listToString(this.deliverables);
    String risksStr = listToString(this.risks);

    return Project.builder()
        .owner(owner)
        .projectName(this.projectName)
        .projectType(this.projectType) // 주제를 description에 저장
        .teamSize(this.teamSize)
        .expectedDurationMonths(this.expectedDurationMonths) // 일 -> 개월 (단순 계산)
        .startDate(start)
        .endDate(end)
        .budget(this.budget)
        .priority(priorityEnum)
        .stakeholders(stakeholdersStr)
        .deliverables(deliverablesStr)
        .risks(risksStr)
        .detailedRequirements(this.detailedRequirements)
        .build();
  }

  // --- Private Helper Methods ---

  private LocalDate parseSafeDate(String dateString) {
    if (dateString == null || dateString.isBlank()) {
      return null;
    }
    try {
      // ISO 8601 Time 부분 제거 (앞 10자리만 사용)
      if (dateString.length() > 10) {
        dateString = dateString.substring(0, 10);
      }
      return LocalDate.parse(dateString);
    } catch (Exception e) {
      return null;
    }
  }

  private String listToString(List<String> list) {
    if (list == null || list.isEmpty()) {
      return "";
    }
    return String.join(",", list);
  }
}