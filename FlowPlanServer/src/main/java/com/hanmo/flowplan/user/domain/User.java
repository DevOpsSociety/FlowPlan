package com.hanmo.flowplan.user.domain;

import com.hanmo.flowplan.global.common.BaseTimeEntity;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectMember;
import com.hanmo.flowplan.task.domain.Task;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String name;

  @Column(name = "google_id", nullable = false)
  private String googleId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserRole role;


  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProjectMember> projectMembers = new ArrayList<>();


  @Builder
  public User(String email, String name, String googleId) {
      this.email = email;
      this.name = name;
      this.googleId = googleId;
      this.role = UserRole.USER; // 기본 역할 설정
  }
}
