package com.hanmo.flowplan.projectMember.presentation;

import com.hanmo.flowplan.global.annotation.CurrentUserId;
import com.hanmo.flowplan.projectMember.application.ProjectMemberService;
import com.hanmo.flowplan.projectMember.presentation.dto.ProjectMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Project Member API", description = "프로젝트 멤버 및 권한 관리 API")
@RestController
@RequestMapping("/api/projects/{projectId}/members") //프로젝트 ID를 경로 변수로 받습니다.
@RequiredArgsConstructor
public class ProjectMemberController {

  private final ProjectMemberService projectMemberService;

  @Operation(summary = "프로젝트 멤버 목록 조회", description = "현재 프로젝트에 참여 중인 모든 멤버의 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<List<ProjectMemberResponse>> getMembers(@PathVariable Long projectId,
                                                                @CurrentUserId String userId) {
    // Service 호출 (내부에서 VIEWER 권한 검증 및 목록 조회)
    List<ProjectMemberResponse> members = projectMemberService.getProjectMembers(projectId, userId);
    return ResponseEntity.ok(members);
  }

  @Operation(summary = "특정 멤버 추방", description = "프로젝트 소유자만 특정 멤버를 추방할 수 있습니다.")
  @DeleteMapping("/{memberId}")
  public ResponseEntity<Void> kickMember(@PathVariable Long projectId,
                                         @PathVariable Long memberId,
                                         @CurrentUserId String ownerId) {
    projectMemberService.kickMember(projectId, memberId, ownerId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "프로젝트 나가기", description = "소유자를 제외한 멤버가 프로젝트에서 스스로 탈퇴합니다.")
  @DeleteMapping("/me")
  public ResponseEntity<Void> leaveProject(@PathVariable Long projectId,
                                           @CurrentUserId String userId) {
    projectMemberService.leaveProject(projectId, userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "편집 권한 요청", description = "VIEWER 멤버가 EDITOR 권한으로 승급을 요청합니다. (소유자 승인 대기)")
  @PostMapping("/request-editor")
  public ResponseEntity<Void> requestEditorRole(@PathVariable Long projectId,
                                                @CurrentUserId String userId) {
    projectMemberService.requestRoleChange(projectId, userId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "권한 승인 및 승급", description = "소유자가 멤버의 PENDING 요청을 승인하여 EDITOR로 승급시킵니다.")
  @PatchMapping("/{memberId}/approve")
  public ResponseEntity<Void> approveEditorRole(@PathVariable Long projectId,
                                                @PathVariable Long memberId,
                                                @CurrentUserId String ownerId) {
    projectMemberService.approveRoleChange(projectId, memberId, ownerId);
    return ResponseEntity.ok().build();
  }
}