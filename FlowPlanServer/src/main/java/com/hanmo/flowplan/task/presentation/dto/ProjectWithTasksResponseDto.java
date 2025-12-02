package com.hanmo.flowplan.task.presentation.dto;

import com.hanmo.flowplan.project.domain.Project;
import java.util.List;

public record ProjectWithTasksResponseDto(
    Long projectId,
    String projectName,
    String projectTopic,         // (엔티티의 description 등 매핑)
    int memberCount,
    int expectedDurationMonths,  // (엔티티의 durationMonths 매핑)
    List<TaskFlatResponseDto> tasks // ⭐️ 기존의 Task 리스트를 여기에 담음
) {
  // 정적 팩토리 메서드
  public static ProjectWithTasksResponseDto of(Project project, List<TaskFlatResponseDto> tasks) {
    return new ProjectWithTasksResponseDto(
        project.getId(),
        project.getProjectName(),
        project.getProjectType(), // 또는 project.getDescription()
        project.getProjectMembers() != null ? project.getProjectMembers().size() : 0,
        project.getExpectedDurationMonths(), // 일 -> 개월 변환 예시
        tasks
    );
  }
}