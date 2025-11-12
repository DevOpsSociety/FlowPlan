package com.hanmo.flowplan.project.domain;

import com.hanmo.flowplan.project.presentation.dto.CreateProjectRequest;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "projects")
@NoArgsConstructor
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner; // 이 프로젝트를 생성한 소유자

  // --- UI 필드 매핑 ---
  @Column(nullable = false)
  private String name; // 1. 프로젝트 명

  @Lob // TEXT 타입
  private String projectType; // 2. 프로젝트 주제

  @Column(name = "team_size")
  private Integer teamSize; // 3. 참여 인원 (⭐️ 추가)

  @Column(name = "duration_months")
  private Integer expectedDurationDays; // 4. 예상 기간(개월) (⭐️ 추가)

  private LocalDate startDate; // 5. 시작일
  private LocalDate endDate; // 6. 마감일

  @Column(precision = 15, scale = 2) // DECIMAL(15, 2)
  private BigDecimal budget; // 7. 예산(만원)

  @Enumerated(EnumType.STRING)
  private ProjectPriority priority; // 8. 우선순위 (High, Medium, Low)

  @Lob
  private String stakeholders; // 9. 주요 이해관계자

  @Lob
  private String deliverables; // 10. 주요 산출물

  @Lob
  private String risks; // 11. 예상 리스크

  @Lob
  private String detailedRequirements; // 12. 더 구체적인 요구사항 (⭐️ 추가)

  // --- 연관 관계 ---
  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ProjectMember> projectMembers = new ArrayList<>();

  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Task> tasks = new ArrayList<>();

  @Builder
  public Project(String name, String projectType, Integer teamSize, Integer expectedDurationDays,
                 LocalDate startDate, LocalDate endDate, BigDecimal budget, ProjectPriority priority,
                 String stakeholders, String deliverables, String risks, String detailedRequirements) {
    this.name = name;
    this.projectType = projectType;
    this.teamSize = teamSize;
    this.expectedDurationDays = expectedDurationDays;
    this.startDate = startDate;
    this.endDate = endDate;
    this.budget = budget;
    this.priority = priority;
    this.stakeholders = stakeholders;
    this.deliverables = deliverables;
    this.risks = risks;
    this.detailedRequirements = detailedRequirements;
  }

  public static Project createProject(CreateProjectRequest createProjectRequest) {
    return Project.builder()
        .name(createProjectRequest.getProjectName())
        .projectType(createProjectRequest.getProjectType())
        .teamSize(createProjectRequest.getTeamSize())
        .expectedDurationDays(createProjectRequest.getExpectedDurationDays())
        .startDate(LocalDate.parse(createProjectRequest.getStartDate()))
        .endDate(LocalDate.parse(createProjectRequest.getEndDate()))
        .budget(new BigDecimal(createProjectRequest.getBudget()))
        .priority(ProjectPriority.valueOf(createProjectRequest.getPriority().toUpperCase()))
        .stakeholders(String.join(",", createProjectRequest.getStakeholders()))
        .deliverables(String.join(",", createProjectRequest.getDeliverables()))
        .risks(String.join(",", createProjectRequest.getRisks()))
        .detailedRequirements(createProjectRequest.getDetailedRequirements())
        .build();
  }



}
