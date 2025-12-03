package com.hanmo.flowplan.projectMember.presentation.dto;

import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;

public record ProjectMemberResponse(
    Long memberId,     // ProjectMember 엔티티의 ID (삭제/추방 시 사용)
    Long userId,       // User 엔티티의 ID
    String userName,   // User 이름
    String userEmail,  // User 이메일
    ProjectRole role   // ⭐️ 멤버 권한 (OWNER, EDITOR, VIEWER 등)
) {
  /**
   * ProjectMember 엔티티를 DTO로 변환하는 정적 팩토리 메서드
   */
  public static ProjectMemberResponse from(ProjectMember member) {
    return new ProjectMemberResponse(
        member.getId(),
        member.getUser().getId(),
        member.getUser().getName(),
        member.getUser().getEmail(),
        member.getProjectRole() // ⭐️ ProjectMember 엔티티에서 role을 가져와 바로 사용
    );
  }
}