package com.hanmo.flowplan.ai.application;

import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.project.domain.Project;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AiDtoMapper {
  /**
   * Project 엔티티를 AI 요청 DTO로 변환합니다.
   */
  public AiSpecRequestDto toSpecRequestDto(Project project) {
    List<String> stakeholdersList = (project.getStakeholders() != null && !project.getStakeholders().isBlank())
        ? List.of(project.getStakeholders().split(","))
        : List.of(); // 빈 리스트

    List<String> deliverablesList = (project.getDeliverables() != null && !project.getDeliverables().isBlank())
        ? List.of(project.getDeliverables().split(","))
        : List.of();

    List<String> risksList = (project.getRisks() != null && !project.getRisks().isBlank())
        ? List.of(project.getRisks().split(","))
        : List.of();

    String priorityKorean = (project.getPriority() != null)
        ? project.getPriority().getKoreanName() // ⬅️ switch문이 사라짐!
        : null;

    String startDateStr = (project.getStartDate() != null) ? project.getStartDate().toString() : null;

    // ⭐️ 수정: endDate도 동일하게 null 확인
    String endDateStr = (project.getEndDate() != null) ? project.getEndDate().toString() : null;

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
}