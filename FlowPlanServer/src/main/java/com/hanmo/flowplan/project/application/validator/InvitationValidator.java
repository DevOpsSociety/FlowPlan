package com.hanmo.flowplan.project.application.validator;

import com.hanmo.flowplan.global.error.ErrorCode;
import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.ProjectInvitation;
import com.hanmo.flowplan.project.domain.repository.ProjectInvitationRepository;
import com.hanmo.flowplan.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationValidator {

  private final ProjectInvitationRepository invitationRepository;

  /**
   * 1. 토큰으로 초대장을 조회하고 유효성(만료, 사용여부)을 검증합니다.
   */
  public ProjectInvitation validateAndGetInvitation(String token) {
    // 조회
    ProjectInvitation invitation = invitationRepository.findByToken(token)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 초대 링크입니다."));

    // 유효성 검사
    if (invitation.isUsed() || invitation.getExpireDate().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "만료되었거나 이미 사용된 초대입니다.");
    }

    return invitation;
  }

  /**
   * 2. 로그인한 유저의 이메일과 초대장의 이메일이 일치하는지 검증합니다.
   */
  public void validateInviteeMatch(User user, ProjectInvitation invitation) {
    if (!user.getEmail().equals(invitation.getInviteeEmail())) {
      throw new BusinessException(ErrorCode.ACCESS_DENIED, "초대받은 이메일 계정으로 로그인해주세요.");
    }
  }
}