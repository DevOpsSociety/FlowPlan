package com.hanmo.flowplan.project.domain;

import com.hanmo.flowplan.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "project_invitations")
public class ProjectInvitation extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(nullable = false)
  private String inviteeEmail; // 초대받는 사람 이메일

  @Column(nullable = false, unique = true)
  private String token; // 초대 링크에 들어갈 랜덤 토큰

  private LocalDateTime expireDate; // 만료 시간 (예: 24시간)

  private boolean used; // 사용 여부

  @Builder
  public ProjectInvitation(Project project, String inviteeEmail, String token, LocalDateTime expireDate) {
    this.project = project;
    this.inviteeEmail = inviteeEmail;
    this.token = token;
    this.expireDate = expireDate;
    this.used = false;
  }

  public void useToken() {
    this.used = true;
  }
}