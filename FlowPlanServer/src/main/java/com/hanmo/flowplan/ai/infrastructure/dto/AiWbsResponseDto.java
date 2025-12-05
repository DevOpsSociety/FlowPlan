package com.hanmo.flowplan.ai.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// ⭐️ 3-2. "Flat WBS 작업 목록"을 AI로부터 돌려받는 DTO (예시 4)
public record AiWbsResponseDto(
    @JsonProperty("project_name")
    String projectName,

    @JsonProperty("total_tasks")
    int totalTasks,

    List<TaskDto> tasks // ⭐️ Task 목록
) {
  // ⭐️ Flat WBS Task 1개 항목
  public record TaskDto(
      @JsonProperty("task_id")
      String taskId,

      @JsonProperty("parent_id")
      String parentTaskId,

      String name,
      String assignee,

      @JsonProperty("start_date")
      String startDate,

      @JsonProperty("end_date")
      String endDate,

      @JsonProperty("duration_days")
      int durationDays,

      int progress,
      String status
  ) {}
}