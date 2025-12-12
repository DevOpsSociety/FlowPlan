package com.hanmo.flowplan.task.domain;

import com.hanmo.flowplan.global.common.BaseTimeEntity;
import com.hanmo.flowplan.project.domain.Project; // Project 엔티티 import (경로 확인 필요)
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.task.presentation.dto.UpdateTaskRequestDto;
import com.hanmo.flowplan.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime; // ERD의 DATETIME 타입에 매핑
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "tasks") // ERD의 테이블 이름과 일치
@NoArgsConstructor
public class Task extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false) // 'project_id' 컬럼, NN (Not Null)
  private Project project;

  // 'parent_id' (상위/하위)
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
  private LocalDate startDate; // ERD의 DATETIME 타입
  @Column
  private LocalDate endDate;   // ERD의 DATETIME 타입

  @Column
  private int progress;

  @Enumerated(EnumType.STRING)
  private TaskStatus status;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Task> children = new ArrayList<>();

  @Builder
  public Task(Project project, Task parent, User assignee,
              String recommendedRole, String name, LocalDate startDate, LocalDate endDate,
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

  public void update(UpdateTaskRequestDto dto, User newAssignee, TaskStatus newStatus, boolean hasChildren) {
    // 1. 이름 수정
    if (dto.name() != null) {
      this.name = dto.name();
    }
    // 2. 날짜 수정 (간트차트)
    if (dto.startDate() != null) {
      this.startDate = dto.startDate();
    }
    if (dto.endDate() != null) {
      this.endDate = dto.endDate();
    }
    // 3. 상태 수정 (칸반보드)
    if (newStatus != null) {
      this.status = newStatus;
    }
    if (dto.progress() != null) {
      // ⭐️ 핵심: 자식이 없는(Leaf) 경우에만 진행률 수정을 허용합니다.
      // 자식이 있다면(hasChildren == true), 요청된 진행률 값(dto.progress)을 무시합니다.
      if (!hasChildren) {
        this.progress = dto.progress();
      }
    }
    // 4. 담당자 수정
    if (newAssignee != null) {
      this.assignee = newAssignee;
    }
    // 3. ⭐️ 상태 <-> 진행률 동기화 (핵심 로직)
    syncStatusAndProgress(newStatus != null, dto.progress() != null);
  }

  private void syncStatusAndProgress(boolean isStatusChanged, boolean isProgressChanged) {
    // Case 1: 상태를 직접 'DONE'이나 'TODO'로 바꿨을 때 -> 진행률 강제 변경
    if (isStatusChanged) {
      if (this.status == TaskStatus.DONE) {
        this.progress = 100;
      } else if (this.status == TaskStatus.TODO) {
        this.progress = 0;
      }
    }

    // Case 2: 진행률을 바꿨을 때 -> 상태 자동 변경
    // (단, 상태를 DONE으로 바꿨는데 진행률을 50으로 보내는 이상한 경우는 진행률 우선으로 처리하거나 정책에 따름.
    // 여기서는 진행률에 따라 상태를 재조정합니다.)
    if (this.progress == 100) {
      this.status = TaskStatus.DONE;
    } else if (this.progress == 0) {
      this.status = TaskStatus.TODO;
    } else {
      // 1 ~ 99 사이면 무조건 진행중
      this.status = TaskStatus.IN_PROGRESS;
    }
  }

  public void forceDone() {
    this.status = TaskStatus.DONE;
    this.progress = 100;
  }

  /**
   * 진행률 강제 설정 (하위 작업 평균으로 계산될 때 호출)
   */
  public void updateProgressFromChildren(int calculatedProgress) {
    this.progress = calculatedProgress;
    // 진행률이 변했으니 상태도 다시 맞춤
    syncStatusAndProgress(false, true);
  }

}