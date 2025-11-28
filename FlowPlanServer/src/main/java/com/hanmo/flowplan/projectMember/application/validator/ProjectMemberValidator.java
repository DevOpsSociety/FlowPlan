package com.hanmo.flowplan.projectMember.application.validator;

import com.hanmo.flowplan.global.error.ErrorCode;
import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component // ⭐️ @Service 대신 @Component (역할이 "검증"이므로)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectMemberValidator {

  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  public Project validateMembership(String googleId, Long projectId) {
    User user = userRepository.findByGoogleId(googleId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

    if (!projectMemberRepository.existsByUserAndProject(user, project)) {
      throw new BusinessException(ErrorCode.NOT_PROJECT_MEMBER);
    }

    return project;
  }

  public void validateMembership(User user, Project project) {
    if (!projectMemberRepository.existsByUserAndProject(user, project)) {
      throw new BusinessException(ErrorCode.NOT_PROJECT_MEMBER);
    }
  }
}
