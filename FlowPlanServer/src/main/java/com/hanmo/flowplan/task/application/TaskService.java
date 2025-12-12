package com.hanmo.flowplan.task.application;

import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import com.hanmo.flowplan.task.application.validator.TaskValidator;
import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskRepository;
import com.hanmo.flowplan.task.domain.TaskStatus;
import com.hanmo.flowplan.task.presentation.dto.CreateTaskRequestDto;
import com.hanmo.flowplan.task.presentation.dto.ProjectWithTasksResponseDto;
import com.hanmo.flowplan.task.presentation.dto.TaskFlatResponseDto;
import com.hanmo.flowplan.task.presentation.dto.UpdateTaskRequestDto;
import com.hanmo.flowplan.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  // AI 응답으로 WBS 저장 (내부 로직)
  @Transactional
  public void saveTasksFromAiResponse(Project project, AiWbsResponseDto wbsResponseDto) {

    if (wbsResponseDto == null || wbsResponseDto.tasks() == null || wbsResponseDto.tasks().isEmpty()) {
      log.warn("No tasks to save for project ID: {}", project.getId());
      return;
    }

    // 1. (1-Pass: 엔티티 생성 및 저장)
    Map<String, Task> taskMap = new HashMap<>();
    List<Task> allTasks = new ArrayList<>();

    for (AiWbsResponseDto.TaskDto dto : wbsResponseDto.tasks()) {
      Task task = Task.builder()
          .project(project)
          .name(dto.name())
          .progress(dto.progress())
          .status(TaskStatus.TODO) // 기본값 TODO
          .startDate(parseDate(dto.startDate()))
          .endDate(parseDate(dto.endDate()))
          .assignee(null) // 실제 담당자는 null
          .recommendedRole(dto.assignee()) // AI 추천 역할 저장
          .build();

      taskMap.put(dto.taskId(), task);
      allTasks.add(task);
    }

    // 2. (2-Pass: 부모-자식 관계 설정)
    for (AiWbsResponseDto.TaskDto dto : wbsResponseDto.tasks()) {
      String pId = dto.parentTaskId();
      // ⭐️ 핵심 수정: null 체크 강화 ("null" 문자열 방어)
      if (pId != null && !pId.equals("null") && !pId.isBlank()) {

        Task currentTask = taskMap.get(dto.taskId());
        Task parentTask = taskMap.get(pId);

        if (currentTask != null && parentTask != null) {
          currentTask.setParent(parentTask);
        }
      }
    }
    // 3. 일괄 저장 (Cascade 옵션 없이도 JPA가 알아서 순서 맞춰 저장함)
    taskRepository.saveAll(allTasks);

    projectRepository.updateLastModifiedDate(project.getId());
  }

  // 1. 작업 목록 조회 (최소 VIEWER 권한 필요)
  @Transactional(readOnly = true)
  public ProjectWithTasksResponseDto getProjectWithTasks(Long projectId, String userId) {
    // 권한 검증 (VIEWER 이상)
    ProjectMember member = projectMemberValidator.validatePermission(userId, projectId, ProjectRole.VIEWER);
    Project project = member.getProject();

    List<Task> allTasks = taskRepository.findAllByProjectId(project.getId());

    List<TaskFlatResponseDto> taskDtos = allTasks.stream()
        .map(TaskFlatResponseDto::from)
        .collect(Collectors.toList());

    return ProjectWithTasksResponseDto.of(project, taskDtos);
  }

  // 2. 신규 작업 생성 (최소 EDITOR 권한 필요)
  @Transactional
  public TaskFlatResponseDto createTask(Long projectId, CreateTaskRequestDto dto, String userId) {
    // 권한 검증 (EDITOR 이상)
    ProjectMember member = projectMemberValidator.validatePermission(userId, projectId, ProjectRole.EDITOR);
    Project project = member.getProject();

    Task parentTask = taskValidator.validateAndGetParentTask(dto.parentId());
    User assignee = taskValidator.validateAndGetAssignee(project, dto.assigneeEmail());
    TaskStatus statusEnum = convertStatus(dto.status());
    if (statusEnum == null) {
      statusEnum = TaskStatus.TODO;
    }


    Task task = Task.builder()
        .project(project)
        .name(dto.name())
        .parent(parentTask)
        .assignee(assignee)
        .startDate(parseDate(dto.startDate()))
        .endDate(parseDate(dto.endDate()))
        .status(statusEnum)
        .progress(dto.progress())
        .recommendedRole(null)
        .build();

    Task savedTask = taskRepository.save(task);

    // 하위 작업 생성 시 부모 진행률 재계산
    if (savedTask.getParent() != null) {
      taskRepository.flush();
      calculateParentProgress(savedTask.getParent());
    }
    project.updateLastModifiedDate();

    return TaskFlatResponseDto.from(savedTask);
  }

  // 3. 작업 수정 (최소 EDITOR 권한 필요)
  @Transactional
  public TaskFlatResponseDto updateTask(Long taskId, UpdateTaskRequestDto dto, String userId) {

    Task task = taskValidator.validateAndGetTask(taskId);

    // 권한 검증 (EDITOR 이상)
    Project project = task.getProject();
    projectMemberValidator.validatePermission(userId, project.getId(), ProjectRole.EDITOR);

    User newAssignee = taskValidator.validateAndGetAssignee(project, dto.assigneeEmail());
    TaskStatus newStatus = convertStatus(dto.status());

    boolean hasChildren = taskRepository.existsByParentId(task.getId());
    task.update(dto, newAssignee, newStatus, hasChildren);

    // 상태 및 진행률 전파 로직
    if (task.getStatus() == TaskStatus.DONE) {
      propagateDownToChildren(task);
    }
    if (task.getParent() != null) {
      calculateParentProgress(task.getParent());
    }

    taskRepository.saveAndFlush(task);
    projectRepository.updateLastModifiedDate(project.getId());

    return TaskFlatResponseDto.from(task);
  }

  // 4. 작업 삭제 (최소 EDITOR 권한 필요)
  @Transactional
  public void deleteTask(Long taskId, String userId) {

    Task task = taskValidator.validateAndGetTask(taskId);

    // 권한 검증 (EDITOR 이상)
    projectMemberValidator.validatePermission(userId, task.getProject().getId(), ProjectRole.EDITOR);

    Task parent = task.getParent();

    taskRepository.delete(task);
    taskRepository.flush();

    // 삭제 후 부모 진행률 재계산
    if (parent != null) {
      calculateParentProgress(parent);
    }

    projectRepository.updateLastModifiedDate(task.getProject().getId());
  }

  // --- Helper Methods ---

  private void propagateDownToChildren(Task parent) {
    List<Task> children = taskRepository.findAllByParentId(parent.getId());

    for (Task child : children) {
      child.forceDone(); // 상태 DONE, 진행률 100%
      propagateDownToChildren(child); // 재귀 호출
    }
    taskRepository.saveAll(children);
  }

  private void calculateParentProgress(Task parent) {
    List<Task> children = taskRepository.findAllByParentId(parent.getId());

    if (children.isEmpty()) return;

    double sum = children.stream().mapToInt(Task::getProgress).sum();
    int avgProgress = (int) Math.round(sum / children.size());

    parent.updateProgressFromChildren(avgProgress);
    taskRepository.save(parent);

    if (parent.getParent() != null) {
      calculateParentProgress(parent.getParent());
    }
  }

  private LocalDate parseDate(String dateString) {
    if (dateString == null || dateString.isBlank()) {
      return null;
    }
    try {
      if (dateString.length() > 10) {
        dateString = dateString.substring(0, 10);
      }
      return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (Exception e) {
      return null;
    }
  }

  private TaskStatus convertStatus(String statusString) {
    if (statusString == null || statusString.isBlank()) {
      return null; // ⭐️ 수정: 값이 없으면 null 반환 (변경 안 함)
    }
    try {
      return TaskStatus.valueOf(statusString.toUpperCase());
    } catch (IllegalArgumentException e) {
      return TaskStatus.TODO;
    }
  }
}