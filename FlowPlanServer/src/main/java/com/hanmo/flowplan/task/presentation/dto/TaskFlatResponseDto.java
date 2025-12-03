package com.hanmo.flowplan.task.presentation.dto;

import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * ⭐️ 작업 조회 응답용 DTO (Flat 구조, Record 형식)
 * 프론트엔드 간트차트/칸반 라이브러리에 최적화되었습니다.
 */
public record TaskFlatResponseDto(
    Long id, // ⭐️ 라이브러리가 사용할 "id"
    Long parent, // ⭐️ 라이브러리가 사용할 "parent_id"
    String name,
    LocalDate start,
    LocalDate end,
    int duration,
    int progress,
    TaskStatus status,
    String assigneeEmail    // 실제 담당자 이름
) {

  /**
   * ⭐️ 엔티티 -> DTO 변환 (재귀 없음)
   * Task 엔티티를 받아서 TaskFlatResponseDto 레코드를 생성하는 정적 팩토리 메서드
   */
  public static TaskFlatResponseDto from(Task task) {

    // ⭐️ 부모 Task가 있으면 부모의 ID를, 없으면 null
    Long parentId = (task.getParent() != null) ? task.getParent().getId() : null;

    // ⭐️ 담당자(User)가 할당되었으면 이름을, 없으면 null
    String assignee = (task.getAssignee() != null) ? task.getAssignee().getName() : task.getRecommendedRole();

    // duration 계산 (일 단위)
    int duration = 0;
    if (task.getStartDate() != null && task.getEndDate() != null) {
      duration = (int) ChronoUnit.DAYS.between(task.getStartDate(), task.getEndDate()) + 1; // +1은 시작일 포함
    }


    // ⭐️ 레코드의 생성자(Canonical Constructor) 호출
    return new TaskFlatResponseDto(
        task.getId(),
        parentId,
        task.getName(),
        task.getStartDate(),
        task.getEndDate(),
        duration,
        task.getProgress(),
        task.getStatus(),
        assignee
    );
  }
}