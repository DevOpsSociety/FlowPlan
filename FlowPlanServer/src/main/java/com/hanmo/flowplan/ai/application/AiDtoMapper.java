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
    return new AiSpecRequestDto(
        project.getName(),
        project.getProjectType(), // ⭐️ projectType 필드가 없으므로 description 사용
        project.getTeamSize(),
        project.getExpectedDurationDays(),
        project.getStartDate().toString(),
        project.getEndDate().toString(),
        project.getBudget() != null ? project.getBudget().toString() : null,
        project.getPriority() != null ? project.getPriority().name() : null,
        project.getStakeholders() != null ? List.of(project.getStakeholders().split(",")) : List.of(),
        project.getDeliverables() != null ? List.of(project.getDeliverables().split(",")): List.of(),
        project.getRisks() != null ? List.of(project.getRisks().split(",")) : List.of(),
        project.getDetailedRequirements()
    );
  }
}