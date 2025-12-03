package com.hanmo.flowplan.projectMember.application;

import com.hanmo.flowplan.global.error.ErrorCode;
import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.presentation.dto.ProjectMemberResponse;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import com.hanmo.flowplan.user.application.validator.UserValidator;
import com.hanmo.flowplan.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

  // ============================================================
  // 1. 프로젝트 멤버 조회 (Read)
  // ============================================================
  public List<ProjectMemberResponse> getProjectMembers(Long projectId, String userId) {
    // 1. 권한 검증: 최소 VIEWER 권한이 있는지 확인
    // (조회 API이므로, validatePermission의 반환값(ProjectMember)은 사용하지 않음)
    projectMemberValidator.validatePermission(userId, projectId, ProjectRole.VIEWER);

    // 2. 프로젝트 ID로 모든 멤버 조회 (ProjectMember 엔티티의 fetch join 설정 권장)
    List<ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);

    // 3. DTO 변환
    return members.stream()
        .map(ProjectMemberResponse::from)
        .collect(Collectors.toList());
  }

  // ============================================================
  // 2. 멤버 추방 (Kick) - OWNER 전용
  // ============================================================
  @Transactional
  public void kickMember(Long projectId, Long memberIdToKick, String ownerGoogleId) {
    // 1. 요청자(Owner) 권한 검증: OWNER만 가능
    projectMemberValidator.validatePermission(ownerGoogleId, projectId, ProjectRole.OWNER);

    // 2. 추방 대상 멤버 조회
    ProjectMember targetMember = projectMemberRepository.findById(memberIdToKick)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "추방할 멤버를 찾을 수 없습니다."));

    // 3. 자기 자신 추방 금지 로직
    if (targetMember.getUser().getGoogleId().equals(ownerGoogleId)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자기 자신은 추방할 수 없습니다.");
    }

    // 4. 추방 실행
    projectMemberRepository.delete(targetMember);
  }

  // ============================================================
  // 3. 프로젝트 나가기 (Leave) - OWNER 제외
  // ============================================================
  @Transactional
  public void leaveProject(Long projectId, String userGoogleId) {
    // 1. 유저 조회
    User user = userValidator.validateAndGetUser(userGoogleId);

    // 2. 프로젝트 조회
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

    // 3. 멤버십 조회
    ProjectMember member = projectMemberRepository.findByUserAndProject(user, project)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PROJECT_MEMBER));

    // 4. OWNER는 나가기 금지 (Owner는 deleteProject로 프로젝트 자체를 삭제해야 함)
    if (member.getProjectRole() == ProjectRole.OWNER) {
      throw new BusinessException(ErrorCode.ACCESS_DENIED, "프로젝트 소유자는 나갈 수 없습니다. 프로젝트를 삭제하거나 소유권을 이전해야 합니다.");
    }

    // 5. 나가기 실행
    projectMemberRepository.delete(member);
  }

}