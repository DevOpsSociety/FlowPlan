package com.hanmo.flowplan.ai.application;

import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.project.domain.Project;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiDtoMapper {
  /**
   * Project 엔티티를 AI 요청 DTO로 변환합니다.
   */
  public AiSpecRequestDto toSpecRequestDto(Project project) {

    String startDateStr = (project.getStartDate() != null) ? project.getStartDate().toString() : null;
    String endDateStr = (project.getEndDate() != null) ? project.getEndDate().toString() : null;

    List<String> stakeholdersList = stringToList(project.getStakeholders());
    List<String> deliverablesList = stringToList(project.getDeliverables());
    List<String> risksList = stringToList(project.getRisks());

    String priorityKorean = (project.getPriority() != null)
        ? project.getPriority().getKoreanName() // ⬅️ switch문이 사라짐!
        : null;


    return new AiSpecRequestDto(
        project.getProjectName(),
        project.getProjectType(), // ⭐️ projectType 필드가 없으므로 description 사용

        project.getTeamSize(),
        project.getExpectedDurationDays(),

        startDateStr,
        endDateStr,

        project.getBudget() != null ? project.getBudget().toString() : null,
        priorityKorean,

        stakeholdersList,
        deliverablesList,

        risksList,
        project.getDetailedRequirements()
    );
  }

  private List<String> stringToList(String text) {
    if (text == null || text.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(text.split(","))
        .map(String::trim) // 공백 제거
        .collect(Collectors.toList());
  }
}