package com.hanmo.flowplan.projectMember.domain;

import com.hanmo.flowplan.global.common.BaseTimeEntity;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "project_members")
public class ProjectMember extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // TODO: 이 멤버의 프로젝트 내 권한
  @Enumerated(EnumType.STRING)
  private ProjectRole projectRole;

  @Builder
  public ProjectMember(User user, Project project, ProjectRole role) {
    this.user = user;
    this.project = project;
    // role이 안 들어오면 기본값으로 VIEWRE나 EDITOR 설정
    this.projectRole = (role != null) ? role : ProjectRole.VIEWER;
  }

  //권한 변경용 메서드 (나중에 관리자 기능에 필요)
  public void updateRole(ProjectRole newRole) {
    this.projectRole = newRole;
  }
}