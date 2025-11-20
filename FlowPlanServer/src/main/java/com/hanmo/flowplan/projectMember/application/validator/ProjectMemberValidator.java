package com.hanmo.flowplan.projectMember.application.validator;

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
        .orElseThrow(() -> new UsernameNotFoundException("User not found with googleId: " + googleId));

    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));

    if (!projectMemberRepository.existsByUserAndProject(user, project)) {
      throw new AccessDeniedException("User is not a member of this project");
    }

    return project;
  }
}
