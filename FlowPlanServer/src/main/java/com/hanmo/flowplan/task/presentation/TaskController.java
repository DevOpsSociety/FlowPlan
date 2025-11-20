package com.hanmo.flowplan.task.presentation;

import com.hanmo.flowplan.task.application.TaskService;
// ⭐️ 1. DTO 클래스들을 import 합니다.
import com.hanmo.flowplan.task.presentation.dto.CreateTaskRequestDto;
import com.hanmo.flowplan.task.presentation.dto.TaskFlatResponseDto;
import com.hanmo.flowplan.task.presentation.dto.UpdateTaskRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  @GetMapping("/projects/{projectId}/tasks")
  public ResponseEntity<List<TaskFlatResponseDto>> getTasks(@PathVariable Long projectId,
                                                            @AuthenticationPrincipal String googleId) {
    // TaskService에 권한 검증 및 조회를 위임
    List<TaskFlatResponseDto> tasks = taskService.getTasks(projectId, googleId);
    return ResponseEntity.ok(tasks);
  }

  @PostMapping("/projects/{projectId}/tasks")
  public ResponseEntity<TaskFlatResponseDto> createTask(@PathVariable Long projectId,
                                                        @RequestBody CreateTaskRequestDto requestDto,
                                                        @AuthenticationPrincipal String googleId) {
    // TaskService에 권한 검증, 생성, 저장을 위임
    TaskFlatResponseDto createdTask = taskService.createTask(projectId, requestDto, googleId);

    // ⭐️ HTTP 201 Created 응답 반환
    return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
  }

  @PatchMapping("/tasks/{taskId}")
  public ResponseEntity<TaskFlatResponseDto> updateTask(@PathVariable Long taskId,
                                                        @RequestBody UpdateTaskRequestDto requestDto,
                                                        @AuthenticationPrincipal String googleId) {
    // TaskService에 권한 검증 및 수정을 위임
    TaskFlatResponseDto updatedTask = taskService.updateTask(taskId, requestDto, googleId);
    return ResponseEntity.ok(updatedTask);
  }

  @DeleteMapping("/tasks/{taskId}")
  public ResponseEntity<Void> deleteTask(@PathVariable Long taskId,
                                         @AuthenticationPrincipal String googleId  ) {
    // TaskService에 권한 검증 및 삭제를 위임
    taskService.deleteTask(taskId, googleId);

    // ⭐️ HTTP 204 No Content 응답 반환
    return ResponseEntity.noContent().build();
  }
}