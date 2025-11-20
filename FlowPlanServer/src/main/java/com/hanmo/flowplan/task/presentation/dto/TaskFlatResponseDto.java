package com.hanmo.flowplan.task.presentation.dto;

import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskStatus;
import java.time.LocalDateTime;

/**
 * ⭐️ 작업 조회 응답용 DTO (Flat 구조, Record 형식)
 * 프론트엔드 간트차트/칸반 라이브러리에 최적화되었습니다.
 */
public record TaskFlatResponseDto(
    Long id, // ⭐️ 라이브러리가 사용할 "id"
    Long parentId, // ⭐️ 라이브러리가 사용할 "parent_id"
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    int progress,
    TaskStatus status,
    String recommendedRole, // AI 추천 역할
    String assigneeName    // 실제 담당자 이름
) {

  /**
   * ⭐️ 엔티티 -> DTO 변환 (재귀 없음)
   * Task 엔티티를 받아서 TaskFlatResponseDto 레코드를 생성하는 정적 팩토리 메서드
   */
  public static TaskFlatResponseDto from(Task task) {

    // ⭐️ 부모 Task가 있으면 부모의 ID를, 없으면 null
    Long parentId = (task.getParent() != null) ? task.getParent().getId() : null;

    // ⭐️ 담당자(User)가 할당되었으면 이름을, 없으면 null
    String assigneeName = (task.getAssignee() != null) ? task.getAssignee().getName() : null;

    // ⭐️ 레코드의 생성자(Canonical Constructor) 호출
    return new TaskFlatResponseDto(
        task.getId(),
        parentId,
        task.getName(),
        task.getStartDate(),
        task.getEndDate(),
        task.getProgress(),
        task.getStatus(),
        task.getRecommendedRole(),
        assigneeName
    );
  }
}