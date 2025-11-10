package com.hanmo.flowplan.ai.appilcation;

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
    // (AI 명세서에 맞게 필드를 매핑합니다. 예시입니다.)
    AiSpecRequestDto.ProjectDurationDto durationDto = new AiSpecRequestDto.ProjectDurationDto(
        project.getStartDate() != null ? project.getStartDate().toString() : null,
        project.getEndDate() != null ? project.getEndDate().toString() : null
    );

    return new AiSpecRequestDto(
        project.getName(),
        project.getDescription(), // ⭐️ projectType 필드가 없으므로 description 사용
        project.getTeamSize(),
        project.getDurationMonths(),
        durationDto,
        project.getBudget() != null ? project.getBudget().toString() : null,
        project.getPriority() != null ? project.getPriority().name() : null,
        project.getKeyStakeholders() != null ? List.of(project.getKeyStakeholders().split(",")) : List.of(),
        project.getKeyDeliverables() != null ? List.of(project.getKeyDeliverables().split(",")) : List.of(),
        project.getRisks() != null ? List.of(project.getRisks().split(",")) : List.of(),
        project.getDescription(), // ⭐️ projectPurpose 필드가 없으므로 description 사용
        List.of(), // keyFeatures 필드가 없음
        project.getDetailedRequirements(),
        null // constraints 필드가 없음
    );
  }
}