package com.hanmo.flowplan.project;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.hanmo.flowplan.ai.application.AiDtoMapper;
import com.hanmo.flowplan.ai.application.AiService;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecResponseDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import com.hanmo.flowplan.project.application.ProjectService;
import com.hanmo.flowplan.project.application.dto.CreateProjectWithSpecResponse;
import com.hanmo.flowplan.project.application.dto.ProjectListResponse;
import com.hanmo.flowplan.project.application.validator.ProjectValidator;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectPriority;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.project.presentation.dto.CreateProjectRequest;
import com.hanmo.flowplan.project.presentation.dto.GenerateWbsRequestDto;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import com.hanmo.flowplan.task.application.TaskService;
import com.hanmo.flowplan.user.application.validator.UserValidator;
import com.hanmo.flowplan.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {
    @Mock
    ProjectRepository projectRepository;

    @Mock
    ProjectMemberRepository projectMemberRepository;

    @Mock
    UserValidator userValidator;

    @Mock
    ProjectValidator projectValidator;

    @Mock
    AiDtoMapper aiDtoMapper;

    @Mock
    AiService aiService;

    @Mock
    TaskService taskService;

    @InjectMocks
    ProjectService projectService;

    @Mock
    ProjectMemberValidator projectMemberValidator;

    @Test
    void 프로젝트_생성_및_스펙_생성_테스트() {
        // given
        String userId = "testUserId";

        User owner = User.builder()
                .email("testUser")
                .name("testUser")
                .googleId("testUser")
                .build();

        CreateProjectRequest createProjectRequest = new CreateProjectRequest(
                "projectName",
                "projectType",
                5,
                90,
                "2025-09-01",
                "2025-12-01",
                new BigDecimal("1000000"),
                "HIGH",
                List.of("member1", "member2"),
                List.of("Frontend", "Backend"),
                List.of("Risk1", "Risk2"),
                "세부 요구사항입니다."
        );

        Project savedProject = Project.builder()
                .owner(owner)
                .projectName("projectName")
                .projectType("projectType")
                .teamSize(5)
                .expectedDurationMonths(3)
                .startDate(LocalDate.parse("2025-09-01"))
                .endDate(LocalDate.parse("2025-12-01"))
                .budget(new BigDecimal("1000000"))
                .priority(ProjectPriority.HIGH)
                .stakeholders("member1, member2")
                .deliverables("Frontend, Backend")
                .risks("Risk1, Risk2")
                .detailedRequirements("세부 요구사항입니다.")
                .build();

        AiSpecRequestDto aiSpecRequestDto = new AiSpecRequestDto(
                "projectName",
                "projectType",
                5,
                90,
                "2025-09-01",
                "2025-12-01",
                "1000000",
                "HIGH",
                List.of("member1", "member2"),
                List.of("Frontend", "Backend"),
                List.of("Risk1", "Risk2"),
                "세부 요구사항입니다."
        );

        AiSpecResponseDto aiSpecResponseDto = new AiSpecResponseDto("markdownSpec");

        given(userValidator.validateAndGetUser(userId)).willReturn(owner);
        given(projectRepository.save(any(Project.class))).willReturn(savedProject);
        given(aiDtoMapper.toSpecRequestDto(savedProject)).willReturn(aiSpecRequestDto);
        given(aiService.generateMarkdownSpec(aiSpecRequestDto)).willReturn(aiSpecResponseDto);

        // when
        CreateProjectWithSpecResponse result = projectService.createProjectAndGenerateSpec(createProjectRequest, userId);

        // then
        assertThat(result.markdownContent()).isEqualTo("markdownSpec");
        then(projectRepository).should().save(any(Project.class));
    }

    @Test
    void WBS_생성_및_Task_저장_테스트() {
        // given
        String userId = "testUserId";
        Long projectId = 1L;
        String markdownContent = "markdownContent";

        GenerateWbsRequestDto generateWbsRequestDto =
                new GenerateWbsRequestDto(projectId, markdownContent);

        User user = User.builder()
                .email("testUser")
                .name("testUser")
                .googleId("testUser")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("projectName")
                .projectType("projectType")
                .teamSize(5)
                .expectedDurationMonths(3)
                .startDate(LocalDate.parse("2025-09-01"))
                .endDate(LocalDate.parse("2025-12-01"))
                .budget(new BigDecimal("1000000"))
                .priority(ProjectPriority.HIGH)
                .stakeholders("member1, member2")
                .deliverables("Frontend, Backend")
                .risks("Risk1, Risk2")
                .detailedRequirements("세부 요구사항입니다.")
                .build();

        AiWbsResponseDto wbsResponseDto = Mockito.mock(AiWbsResponseDto.class);

        ProjectMember projectMember = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.EDITOR)
                .build();

        given(projectMemberValidator.validatePermission(userId, projectId, ProjectRole.EDITOR)).willReturn(projectMember);
        given(aiService.generateWbsFromMarkdown(markdownContent)).willReturn(wbsResponseDto);

        // when
        projectService.generateWbsAndSaveTasks(generateWbsRequestDto, userId);

        // then
        then(projectMemberValidator)
                .should().validatePermission(userId, projectId, ProjectRole.EDITOR);
        then(aiService).should().generateWbsFromMarkdown(markdownContent);
        then(taskService).should().saveTasksFromAiResponse(user, project, wbsResponseDto);
    }


    @Test
    void 유저가_참여_중인_모든_프로젝트_조회_테스트() {
        // given
        String userId = "testUserId";

        User user = User.builder()
                .email("testUser")
                .name("testUser")
                .googleId("testUser")
                .build();

        Project project1 = Project.builder()
                .owner(user)
                .projectName("project1")
                .projectType("typeA")
                .teamSize(3)
                .expectedDurationMonths(1)
                .startDate(LocalDate.parse("2025-01-01"))
                .endDate(LocalDate.parse("2025-02-01"))
                .budget(new BigDecimal("500000"))
                .priority(ProjectPriority.MEDIUM)
                .stakeholders("member1")
                .deliverables("deliverableA")
                .risks("riskA")
                .detailedRequirements("reqA")
                .build();

        Project project2 = Project.builder()
                .owner(user)
                .projectName("project2")
                .projectType("typeB")
                .teamSize(5)
                .expectedDurationMonths(2)
                .startDate(LocalDate.parse("2025-03-01"))
                .endDate(LocalDate.parse("2025-04-01"))
                .budget(new BigDecimal("1000000"))
                .priority(ProjectPriority.HIGH)
                .stakeholders("member2")
                .deliverables("deliverableB")
                .risks("riskB")
                .detailedRequirements("reqB")
                .build();

        LocalDateTime updated1 = LocalDateTime.of(2025, 1, 10, 0, 0);
        LocalDateTime updated2 = LocalDateTime.of(2025, 2, 10, 0, 0);

        ReflectionTestUtils.setField(project1, "updatedAt", updated1);
        ReflectionTestUtils.setField(project2, "updatedAt", updated2);

        ProjectMember membership1 = ProjectMember.builder()
                .user(user)
                .project(project1)
                .build();

        ProjectMember membership2 = ProjectMember.builder()
                .user(user)
                .project(project2)
                .build();

        given(userValidator.validateAndGetUser(userId)).willReturn(user);
        given(projectMemberRepository.findAllByUser(user))
                .willReturn(List.of(membership1, membership2));

        // when
        List<ProjectListResponse> result = projectService.findAllProjects(userId);

        // then
        assertThat(result.size()).isEqualTo(2);
        then(projectMemberRepository).should().findAllByUser(user);
    }

    @Test
    void 프로젝트_삭제_테스트() {
        // given
        String userId = "testUserId";
        Long projectId = 1L;

        User user = User.builder()
                .email("testUser")
                .name("testUser")
                .googleId("testUser")
                .build();

        ReflectionTestUtils.setField(user, "id", 100L);

        Project project = Project.builder()
                .owner(user)
                .projectName("projectName")
                .projectType("projectType")
                .teamSize(5)
                .expectedDurationMonths(3)
                .startDate(LocalDate.parse("2025-09-01"))
                .endDate(LocalDate.parse("2025-12-01"))
                .budget(new BigDecimal("1000000"))
                .priority(ProjectPriority.HIGH)
                .stakeholders("member1, member2")
                .deliverables("Frontend, Backend")
                .risks("Risk1, Risk2")
                .detailedRequirements("세부 요구사항입니다.")
                .build();

        ProjectMember ownerMember = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.OWNER)
                .build();

        given(projectMemberValidator.validatePermission(userId, projectId, ProjectRole.OWNER)).willReturn(ownerMember);

        // when
        projectService.deleteProject(projectId, userId);

        // then
        then(projectMemberValidator).should().validatePermission(userId, projectId, ProjectRole.OWNER);
        then(projectRepository).should().delete(project);
    }
}
