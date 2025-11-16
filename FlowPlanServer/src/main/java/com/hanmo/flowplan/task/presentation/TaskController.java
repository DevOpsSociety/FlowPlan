package com.hanmo.flowplan.task.presentation;

import com.hanmo.flowplan.global.jwt.CustomUserDetails;
import com.hanmo.flowplan.task.application.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Task API", description = "Task 관련 API")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
  private final TaskService taskService;

  // ⭐️ WBS 조회 API ⭐️
  @GetMapping
  public ResponseEntity<List<TaskResponseDto>> getTasksForProject(@PathVariable Long projectId,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
    // (필요하다면) userDetails.getGoogleId()로 이 프로젝트 멤버가 맞는지
    // ProjectService나 ProjectMemberService를 통해 검증하는 로직 추가
    List<TaskResponseDto> tasks = taskService.getTasksByProjectId(projectId);
    return ResponseEntity.ok(tasks);
  }
}
