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
  private String projectName; // 1. 프로젝트 명

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
  public Project(User owner, String projectName, String projectType, Integer teamSize, Integer expectedDurationDays,
                 LocalDate startDate, LocalDate endDate, BigDecimal budget, ProjectPriority priority,
                 String stakeholders, String deliverables, String risks, String detailedRequirements) {
    this.owner = owner;
    this.projectName = projectName;
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

  // com.hanmo.flowplan.project.domain.Project.java


  public static Project createProject(User owner, CreateProjectRequest createProjectRequest) {

    // --- Null에 안전하게 값 처리 ---

    // ⭐️ 1. 날짜 (필수 필드)
    // (DTO에서 @NotBlank를 걸었지만, 방어적으로 한 번 더 체크)
    LocalDate start = (createProjectRequest.getStartDate() != null && !createProjectRequest.getStartDate().isBlank())
        ? LocalDate.parse(createProjectRequest.getStartDate())
        : null;

    LocalDate end = (createProjectRequest.getEndDate() != null && !createProjectRequest.getEndDate().isBlank())
        ? LocalDate.parse(createProjectRequest.getEndDate())
        : null;

    // ⭐️ 2. 예산 (Budget) - null 체크
    BigDecimal budgetValue = createProjectRequest.getBudget(); // ⬅️ null이면 null이 그대로 들어감

    // ⭐️ 3. 우선순위 (Priority) - null 체크
    ProjectPriority priorityValue = (createProjectRequest.getPriority() != null && !createProjectRequest.getPriority().isBlank())
        ? ProjectPriority.valueOf(createProjectRequest.getPriority().toUpperCase())
        : null; // ⬅️ null이면 null로 저장

    // ⭐️ 4. 리스트 (Stakeholders, Deliverables, Risks) - null 체크
    // (List가 null일 경우, 빈 문자열 ""을 저장)
    String stakeholdersValue = (createProjectRequest.getStakeholders() != null)
        ? String.join(",", createProjectRequest.getStakeholders())
        : ""; // ⬅️ null이면 빈 문자열로 저장

    String deliverablesValue = (createProjectRequest.getDeliverables() != null)
        ? String.join(",", createProjectRequest.getDeliverables())
        : "";

    String risksValue = (createProjectRequest.getRisks() != null)
        ? String.join(",", createProjectRequest.getRisks())
        : "";

    // --- Builder에 안전한 값 주입 ---

    return Project.builder()
        .owner(owner)
        // (필수 값들)
        .projectName(createProjectRequest.getProjectName())
        .projectType(createProjectRequest.getProjectType())
        .teamSize(createProjectRequest.getTeamSize())
        .expectedDurationDays(createProjectRequest.getExpectedDurationDays())

        // (선택적 null 가능 값들)
        .startDate(start)
        .endDate(end)
        .budget(budgetValue)
        .priority(priorityValue)
        .stakeholders(stakeholdersValue)
        .deliverables(deliverablesValue)
        .risks(risksValue)

        .detailedRequirements(createProjectRequest.getDetailedRequirements()) // (String이라 null이어도 OK)
        .build();
  }



}
