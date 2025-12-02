package com.hanmo.flowplan.projectMember.application.validator;

import com.hanmo.flowplan.global.error.ErrorCode;
import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
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

  public void validateNotAlreadyMember(Long projectId, String inviteeEmail) {
    // 1. 이메일로 가입된 유저가 있는지 찾음
    // (가입되지 않은 이메일이라면, 당연히 멤버도 아니므로 통과!)
    userRepository.findByEmail(inviteeEmail).ifPresent(user -> {

      // 2. 프로젝트 조회 (가벼운 조회를 위해 getReferenceById 사용 가능하지만, 안전하게 findById)
      Project project = projectRepository.findById(projectId)
          .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

      // 3. 이미 멤버인지 확인
      if (projectMemberRepository.existsByUserAndProject(user, project)) {
        throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 프로젝트의 멤버입니다.");
      }
    });
  }

  public void validateNewMember(User user, Project project) {
    if (projectMemberRepository.existsByUserAndProject(user, project)) {
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE); // "데이터가 이미 존재합니다."
    }
  }
}
