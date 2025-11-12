package com.hanmo.flowplan.task.domain;

import com.hanmo.flowplan.global.common.BaseTimeEntity;
import com.hanmo.flowplan.project.domain.Project; // Project 엔티티 import (경로 확인 필요)
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime; // ERD의 DATETIME 타입에 매핑
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Table(name = "tasks") // ERD의 테이블 이름과 일치
@NoArgsConstructor
public class Task extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false) // 'project_id' 컬럼, NN (Not Null)
  private Project project;

  // 'parent_id' (대분류/소분류)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Task parent; // 자기 자신을 참조 (셀프 조인)

  // 'assignee_id' (담당자)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private User assignee;

  @Column(name = "recommended_role")
  String recommendedRole;

  @Column(nullable = false) // NN (Not Null)
  private String name;

  @Column
  private LocalDateTime startDate; // ERD의 DATETIME 타입
  @Column
  private LocalDateTime endDate;   // ERD의 DATETIME 타입

  @Column
  private int progress;

  @Enumerated(EnumType.STRING)
  private TaskStatus status;

  @Builder
  public Task(Project project, Task parent, User assignee,
              String recommendedRole, String name, LocalDateTime startDate, LocalDateTime endDate,
              int progress, TaskStatus status) {
    this.project = project;
    this.parent = parent;
    this.assignee = assignee;
    this.recommendedRole = recommendedRole;
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
    this.progress = progress;
    this.status = status;

  }


  public void setParent(Task parent) {
    this.parent = parent;
  }

  private LocalDateTime parseDate(String dateString) {
    if (dateString == null || dateString.isBlank()) {
      return null;
    }
    return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
  }

}