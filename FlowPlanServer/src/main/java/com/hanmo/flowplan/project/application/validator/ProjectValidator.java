package com.hanmo.flowplan.project.application.validator;

import com.hanmo.flowplan.global.error.ErrorCode;
import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectValidator {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  public Project validateAndGetProject(Long projectId) {
    return projectRepository.findById(projectId)
        .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));
  }

  public void validateNewMember(User user, Project project) {
    if (projectMemberRepository.existsByUserAndProject(user, project)) {
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 프로젝트의 멤버입니다.");
    }
  }
}
