package com.hanmo.flowplan.task.application;

import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectRepository;
import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskRepository;
import com.hanmo.flowplan.task.domain.TaskStatus;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final TaskRepository taskRepository;

  @Transactional
  public void saveTasksFromAiResponse(User user , Project project, AiWbsResponseDto wbsResponseDto) {

    if (wbsResponseDto == null || wbsResponseDto.tasks() == null || wbsResponseDto.tasks().isEmpty()) {
      log.warn("No tasks to save for project ID: {}", project.getId());
      return;
    }

    // ⭐️ 1. (1-Pass: 엔티티 생성 및 저장)
    // AI의 문자열 ID("1.0")와 실제 저장된 Task 엔티티를 매핑하는 Map
    Map<String, Task> taskMap = new HashMap<>();
    List<Task> tasksToSave = new ArrayList<>();

    for (AiWbsResponseDto.TaskDto dto : wbsResponseDto.tasks()) {
      Task task = Task.builder()
          .project(project)
          .name(dto.name())
          .progress(dto.progress())
          .status(TaskStatus.TODO) // TaskStatus.TODO 기본값
          .startDate(parseDate(dto.startDate()))
          .endDate(parseDate(dto.endDate()))
          .assignee(null) // 실제 담당자는 null
          .recommendedRole(dto.assignee()) // "PM", "개발자" 문자열 저장
          .build();

      tasksToSave.add(task);

      // 맵에 <"1.0", Task엔티티> 저장 (부모-자식 관계 설정 전)
      taskMap.put(dto.taskId(), task);
    }

    // ⭐️ 부모-자식 관계를 설정하기 전에 먼저 DB에 저장하여 ID(PK)를 생성
    taskRepository.saveAll(tasksToSave);

    // ⭐️ 2. (2-Pass: 부모-자식 관계 설정)
    // 저장된 엔티티를 다시 순회하며 부모 ID를 연결합니다.
    for (AiWbsResponseDto.TaskDto dto : wbsResponseDto.tasks()) {
      if (dto.parentTaskId() != null) {
        Task currentTask = taskMap.get(dto.taskId());
        Task parentTask = taskMap.get(dto.parentTaskId());

        if (currentTask != null && parentTask != null) {
          currentTask.setParent(parentTask);
        }
      }
    }
    // ⭐️ @Transactional이므로, 2-Pass에서 변경된 'parent' 필드는
    //    메서드가 끝날 때 자동으로 DB에 UPDATE 됩니다.

  }



  // 헬퍼 메서드: 날짜 파싱 (YYYY-MM-DD)
  private LocalDateTime parseDate(String dateString) {
    if (dateString == null || dateString.isBlank()) {
      return null;
    }
    // AI가 "YYYY-MM-DD" 형식으로 날짜를 반환
    return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
  }
}
