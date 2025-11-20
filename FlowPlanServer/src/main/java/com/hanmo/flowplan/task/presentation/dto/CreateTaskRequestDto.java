package com.hanmo.flowplan.task.presentation.dto;

import java.time.LocalDateTime;

/**
 * API 2: 신규 작업 생성을 위한 Request Body DTO
 * (TaskController에서 @RequestBody로 받습니다)
 */
public record CreateTaskRequestDto(
    String name,

    // ⭐️ 부모 Task ID (Task 엔티티의 PK, 최상위 작업이면 null)
    Long parentId,
    // ⭐️ 담당자 ID (User 엔티티의 PK, 할당 안 하면 null)
    Long assigneeId,

    String startDate,
    String endDate,

    String status,   // (예: "TODO", "IN_PROGRESS", "DONE")
    Integer progress // (기본값 0)
) {}