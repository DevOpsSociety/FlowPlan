package com.hanmo.flowplan.project.application;

import com.hanmo.flowplan.global.service.EmailService;
import com.hanmo.flowplan.project.application.validator.InvitationValidator;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectInvitation;
import com.hanmo.flowplan.project.domain.repository.ProjectInvitationRepository;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import com.hanmo.flowplan.user.application.validator.UserValidator;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

  private final ProjectInvitationRepository invitationRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final EmailService emailService;

  private final ProjectMemberValidator projectMemberValidator;
  private final InvitationValidator invitationValidator;
  private final UserValidator userValidator;

  // 1. 초대 보내기
  @Transactional
  public void inviteUser(Long projectId, String inviteeEmail, String ownerGoogleId, String baseUrl) {
    // 1. 권한 검증 (초대하는 사람이 프로젝트 멤버인지)
    Project project = projectMemberValidator.validateMembership(ownerGoogleId, projectId);

    // 2. 이미 멤버인지 확인
    projectMemberValidator.validateNotAlreadyMember(projectId, inviteeEmail);

    // 3. 초대 토큰 생성
    String token = UUID.randomUUID().toString();

    // 4. DB 저장
    ProjectInvitation invitation = ProjectInvitation.builder()
        .project(project)
        .inviteeEmail(inviteeEmail)
        .token(token)
        .expireDate(LocalDateTime.now().plusDays(1)) // 24시간 유효
        .build();
    invitationRepository.save(invitation);

    // 5. 이메일 발송 (프론트엔드 주소)
    // 예: https://flowplan-ai.vercel.app/invite/accept?token=...
    String inviteUrl = baseUrl + token;
    emailService.sendInvitationEmail(inviteeEmail, project.getProjectName(), inviteUrl);
  }

  // 2. 초대 수락하기
  @Transactional
  public void acceptInvitation(String token, String userGoogleId) {
    // 1. 초대장 검증 및 조회 (InvitationValidator 위임)
    ProjectInvitation invitation = invitationValidator.validateAndGetInvitation(token);

    // 2. 사용자 검증 및 조회 (UserValidator 위임 - 기존에 만드신 것)
    User user = userValidator.validateAndGetUser(userGoogleId);

    // 3. 이메일 일치 검증 (InvitationValidator 위임)
    invitationValidator.validateInviteeMatch(user, invitation);

    // 4. 중복 멤버 검증 (ProjectMemberValidator 위임)
    projectMemberValidator.validateNewMember(user, invitation.getProject());

    // 5. 멤버 추가 (핵심 로직)
    ProjectMember newMember = ProjectMember.builder()
        .project(invitation.getProject())
        .user(user)
        .role(ProjectRole.VIEWER)
        .build();
    projectMemberRepository.save(newMember);

    // 6. 토큰 사용 처리 (핵심 로직)
    invitation.useToken();
  }
}