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

  // TODO: 이 멤버의 프로젝트 내 "역할" (예: "PL", "개발자")을 저장할 컬럼 추가 가능
  // private String projectRole;

  @Builder
  public ProjectMember(Project project, User user) {
    this.project = project;
    this.user = user;
  }
}