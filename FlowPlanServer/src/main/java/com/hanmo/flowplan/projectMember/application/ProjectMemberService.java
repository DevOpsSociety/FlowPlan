package com.hanmo.flowplan.projectMember.application;

import com.hanmo.flowplan.global.error.ErrorCode;
import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import com.hanmo.flowplan.user.application.validator.UserValidator;
import com.hanmo.flowplan.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectRepository projectRepository;
  private final UserValidator userValidator;
  private final ProjectMemberValidator projectMemberValidator;

  // 1. 권한 변경 요청 (멤버 -> 소유자에게)
  @Transactional
  public void requestRoleChange(Long projectId, String userId) {
    User user = userValidator.validateAndGetUser(userId);
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

    ProjectMember member = projectMemberRepository.findByUserAndProject(user, project)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));

    // 이미 EDITOR나 OWNER면 요청 불필요
    if (member.getProjectRole() == ProjectRole.EDITOR || member.getProjectRole() == ProjectRole.OWNER) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 편집 권한이 있습니다.");
    }

    // 역할을 PENDING (승인 대기) 상태로 변경
    // (또는 별도의 status 컬럼을 두는 방법도 있지만, 여기선 role을 활용)
    member.updateRole(ProjectRole.PENDING);
  }

  // 2. 권한 승인 (소유자 -> 멤버)
  @Transactional
  public void approveRoleChange(Long projectId, Long memberId, String ownerId) {
    // 1. 요청자(Owner) 권한 검증
    projectMemberValidator.validatePermission(ownerId, projectId, ProjectRole.OWNER);

    // 2. 승인 대상 멤버 조회
    ProjectMember targetMember = projectMemberRepository.findById(memberId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "멤버를 찾을 수 없습니다."));

    // 3. PENDING 상태인지 확인 (선택 사항)
    projectMemberValidator.validateRoleChangeStatus(targetMember, ProjectRole.PENDING);

    // 4. EDITOR로 승급
    targetMember.updateRole(ProjectRole.EDITOR);
  }
}