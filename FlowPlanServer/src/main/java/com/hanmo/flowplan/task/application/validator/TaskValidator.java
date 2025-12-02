package com.hanmo.flowplan.task.application.validator;

import com.hanmo.flowplan.global.error.ErrorCode;
import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskRepository;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component // ⭐️ Spring Bean으로 등록
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskValidator {

  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final ProjectMemberValidator projectMemberValidator; // ⭐️ 기존 검증기 재사용

  public Task validateAndGetTask(Long taskId) {
    return taskRepository.findById(taskId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
  }

  public Task validateAndGetParentTask(Long parentId) {
    if (parentId == null) {
      return null; // 부모가 없는 최상위 작업 (정상)
    }

    // 부모 ID가 있는데 DB에 존재하지 않으면 예외 발생
    return taskRepository.findById(parentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
  }

  public User validateAndGetAssignee(Project project, Long assigneeId) {
    if (assigneeId == null) {
      return null; // 담당자가 없음 (정상)
    }

    // 1. User 존재 확인
    User assignee = userRepository.findById(assigneeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 2. ⭐️ Project 멤버 검증 로직 위임
    projectMemberValidator.validateMembership(assignee, project);

    return assignee; // 모든 검증 통과
  }

}
