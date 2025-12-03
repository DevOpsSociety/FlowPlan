package com.hanmo.flowplan.task.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ⭐️ 모든 필드가 null일 수 있습니다.
// (Gantt는 startDate/endDate만, Kanban은 status만 보냄)
public record UpdateTaskRequestDto(
    String name,
    String assigneeEmail,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    Integer progress
) {}