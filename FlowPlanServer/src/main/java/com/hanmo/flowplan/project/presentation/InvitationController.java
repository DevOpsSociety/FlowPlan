package com.hanmo.flowplan.project.presentation;

import com.hanmo.flowplan.global.annotation.CurrentUserId;
import com.hanmo.flowplan.project.application.InvitationService;
import com.hanmo.flowplan.project.presentation.dto.InviteUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invitation API", description = "프로젝트 초대 관련 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class InvitationController {

  private final InvitationService invitationService;

  /**
   * 1. 초대 보내기 API
   * [POST /api/projects/{projectId}/invite]
   */
  @Operation(summary = "팀원 초대 보내기", description = "입력받은 이메일로 프로젝트 초대장을 발송합니다. (프로젝트 멤버만 가능)")
  @PostMapping("/{projectId}/invite")
  public ResponseEntity<Void> inviteUser(@PathVariable Long projectId,
                                         @RequestBody InviteUserRequest request,
                                         @CurrentUserId String googleId) {
    // 서비스 호출
    invitationService.inviteUser(projectId, request.email(), googleId);

    // 성공 시 200 OK 반환
    return ResponseEntity.ok().build();
  }

  /**
   * 2. 초대 수락하기 API
   * [POST /api/projects/invite/accept]
   * (프론트엔드 '초대 수락 페이지'에서 호출)
   */
  @Operation(summary = "초대 수락하기", description = "이메일로 받은 초대 토큰을 검증하고, 프로젝트 멤버로 등록합니다.")
  @PostMapping("/invite/accept")
  public ResponseEntity<Void> acceptInvitation(@RequestParam String token,
                                               @CurrentUserId String googleId ) {
    // 서비스 호출
    invitationService.acceptInvitation(token, googleId);

    // 성공 시 200 OK 반환
    return ResponseEntity.ok().build();
  }
}