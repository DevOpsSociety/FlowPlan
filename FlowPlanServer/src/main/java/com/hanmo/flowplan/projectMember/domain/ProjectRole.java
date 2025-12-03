package com.hanmo.flowplan.projectMember.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectRole {

  // 레벨(Level)이 높을수록 강력한 권한
  OWNER("소유자", 3),
  EDITOR("편집자", 2),
  VIEWER("뷰어", 1),
  PENDING("대기중", 0);

  private final String description;
  private final int level;

  /**
   * 내 권한(this)이 필요한 권한(required)보다 높은지 확인
   */
  public boolean hasPermission(ProjectRole requiredRole) {
    return this.level >= requiredRole.level;
  }
}