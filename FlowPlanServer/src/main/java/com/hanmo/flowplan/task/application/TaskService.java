package com.hanmo.flowplan.task.application;

import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.task.application.validator.TaskValidator;
import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskRepository;
import com.hanmo.flowplan.task.domain.TaskStatus;
import com.hanmo.flowplan.task.presentation.dto.CreateTaskRequestDto;
import com.hanmo.flowplan.task.presentation.dto.ProjectWithTasksResponseDto;
import com.hanmo.flowplan.task.presentation.dto.TaskFlatResponseDto;
import com.hanmo.flowplan.task.presentation.dto.UpdateTaskRequestDto;
import com.hanmo.flowplan.user.domain.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;
  private final ProjectMemberValidator projectMemberValidator;
  private final TaskValidator taskValidator;

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
    projectRepository.updateLastModifiedDate(project.getId());
  }


  @Transactional
  public ProjectWithTasksResponseDto getProjectWithTasks(Long projectId, String userId) {
    // 1. 권한 검증
    Project project = projectMemberValidator.validateMembership(userId, projectId);

    // 2. ⭐️ 프로젝트의 "모든" Task 조회
    List<Task> allTasks = taskRepository.findAllByProjectId(project.getId());

    // 3. Task 엔티티 리스트 -> Task DTO 리스트 변환
    List<TaskFlatResponseDto> taskDtos = allTasks.stream()
        .map(TaskFlatResponseDto::from)
        .collect(Collectors.toList());

    // 3. ⭐️ DTO로 변환 (재귀 없음. Flat 리스트 -> Flat 리스트)
    return ProjectWithTasksResponseDto.of(project, taskDtos);
  }

  // API 2: 신규 작업 생성
  @Transactional
  public TaskFlatResponseDto createTask(Long projectId, CreateTaskRequestDto dto, String userId) {
    // ⭐️ 3. 검증 로직 위임 (Project 객체를 반환받음)
    Project project = projectMemberValidator.validateMembership(userId, projectId);

    // 2. ⭐️ (유효성 검증) 헬퍼 메서드 대신 Validator 호출
    Task parentTask = taskValidator.validateAndGetParentTask(dto.parentId());
    User assignee = taskValidator.validateAndGetAssignee(project, dto.assigneeId());

    TaskStatus statusEnum = convertStatus(dto.status());

    // ... (Task 생성 로직) ...
    Task task = Task.builder()
        .project(project)
        .name(dto.name())
        .parent(parentTask)
        .assignee(assignee)
        .startDate(parseDate(dto.startDate()))
        .endDate(parseDate(dto.endDate()))
        .status(statusEnum) // 기본값
        .progress(dto.progress())
        .recommendedRole(null)
        .build();

    Task savedTask = taskRepository.save(task);
    projectRepository.updateLastModifiedDate(projectId); // 프로젝트 수정일 업데이트
    return TaskFlatResponseDto.from(savedTask);
  }

  @Transactional
  public TaskFlatResponseDto updateTask(Long taskId, UpdateTaskRequestDto dto, String userId) {

    // 1. (권한 검증) Task 조회 및 사용자 권한 검증
    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    Project project = projectMemberValidator.validateMembership(userId, task.getProject().getId());

    // 2. ⭐️ (검증) 업데이트에 필요한 값들을 Validator를 통해 미리 준비
    User newAssignee = taskValidator.validateAndGetAssignee(project, dto.assigneeId());
    TaskStatus newStatus = convertStatus(dto.status()); // DTO의 String -> Enum 변환

    // 3. ⭐️ (핵심) 엔티티에게 "업데이트"를 명령 (Tell, Don't Ask)
    //    - 서비스는 더 이상 task.setName() 등을 직접 호출하지 않음
    task.update(dto, newAssignee, newStatus);
    taskRepository.saveAndFlush(task);

    projectRepository.updateLastModifiedDate(project.getId());
    return TaskFlatResponseDto.from(task); // 'from' 팩토리 메서드 사용
  }

  @Transactional
  public void deleteTask(Long taskId, String userId) {

    // 1. (엔티티 조회) 삭제할 Task를 DB에서 찾습니다.
    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));

    // 2. (권한 검증) 이 Task가 속한 프로젝트의 멤버가 맞는지 확인합니다.
    //    (validateMembership은 멤버가 아니면 AccessDeniedException을 던집니다)
    projectMemberValidator.validateMembership(userId, task.getProject().getId());

    // 3. (삭제) 권한 검증이 통과되면 엔티티를 삭제합니다.
    //    (주의: 이 Task를 부모로 가진 자식 Task들의 'parent_id' 처리가 필요할 수 있습니다)
    taskRepository.delete(task);
    taskRepository.flush();
    projectRepository.updateLastModifiedDate(task.getProject().getId());
  }



  // 헬퍼 메서드: 날짜 파싱 (YYYY-MM-DD)
  private LocalDate parseDate(String dateString) {
    if (dateString == null || dateString.isBlank()) {
      return null;
    }
    try {
      // ⭐️ [핵심 수정] 시간 정보(T...)가 붙어있으면 앞의 10자리(YYYY-MM-DD)만 잘라냅니다.
      if (dateString.length() > 10) {
        dateString = dateString.substring(0, 10);
      }
      return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (Exception e) {
      // 파싱 실패 시 null 반환 (로그를 남겨도 좋습니다)
      return null;
    }
  }

  private TaskStatus convertStatus(String statusString) {
    if (statusString == null || statusString.isBlank()) {
      return TaskStatus.TODO;
    }
    try {
      // "todo" -> "TODO" -> TaskStatus.TODO
      return TaskStatus.valueOf(statusString.toUpperCase());
    } catch (IllegalArgumentException e) {
      // "할일" 같이 Enum에 없는 값이 들어올 경우 기본값 처리
      return TaskStatus.TODO;
    }
  }

}
