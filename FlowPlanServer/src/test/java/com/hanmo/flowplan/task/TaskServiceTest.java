package com.hanmo.flowplan.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectPriority;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import com.hanmo.flowplan.task.application.TaskService;
import com.hanmo.flowplan.task.application.validator.TaskValidator;
import com.hanmo.flowplan.task.domain.Task;
import com.hanmo.flowplan.task.domain.TaskRepository;
import com.hanmo.flowplan.task.domain.TaskStatus;
import com.hanmo.flowplan.task.presentation.dto.CreateTaskRequestDto;
import com.hanmo.flowplan.task.presentation.dto.ProjectWithTasksResponseDto;
import com.hanmo.flowplan.task.presentation.dto.TaskFlatResponseDto;
import com.hanmo.flowplan.task.presentation.dto.UpdateTaskRequestDto;
import com.hanmo.flowplan.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    TaskRepository taskRepository;

    @Mock
    ProjectMemberValidator projectMemberValidator;

    @Mock
    TaskValidator taskValidator;

    @InjectMocks
    TaskService taskService;

    @DisplayName("AI WBS 응답으로 작업을 생성하고, 부모-자식 관계를 설정한다.")
    @Test
    @SuppressWarnings("unchecked")
    void AI_WBS_작업_생성_및_부모자식_연결() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .name("user")
                .googleId("gid")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("proj")
                .projectType("type")
                .teamSize(3)
                .expectedDurationMonths(1)
                .startDate(LocalDate.parse("2025-01-01"))
                .endDate(LocalDate.parse("2025-02-01"))
                .budget(new BigDecimal("100000"))
                .priority(ProjectPriority.MEDIUM)
                .stakeholders("member1")
                .deliverables("deliverable")
                .risks("risk")
                .detailedRequirements("req")
                .build();
        ReflectionTestUtils.setField(project, "id", 1L);

        AiWbsResponseDto.TaskDto parentDto = Mockito.mock(AiWbsResponseDto.TaskDto.class);
        given(parentDto.taskId()).willReturn("T1");
        given(parentDto.parentTaskId()).willReturn(null);
        given(parentDto.name()).willReturn("Parent");
        given(parentDto.progress()).willReturn(0);
        given(parentDto.startDate()).willReturn("2025-01-01");
        given(parentDto.endDate()).willReturn("2025-01-10");
        given(parentDto.assignee()).willReturn("Backend");

        AiWbsResponseDto.TaskDto childDto = Mockito.mock(AiWbsResponseDto.TaskDto.class);
        given(childDto.taskId()).willReturn("T2");
        given(childDto.parentTaskId()).willReturn("T1");
        given(childDto.name()).willReturn("Child");
        given(childDto.progress()).willReturn(0);
        given(childDto.startDate()).willReturn("2025-01-02");
        given(childDto.endDate()).willReturn("2025-01-05");
        given(childDto.assignee()).willReturn("Frontend");

        AiWbsResponseDto wbs = Mockito.mock(AiWbsResponseDto.class);
        given(wbs.tasks()).willReturn(List.of(parentDto, childDto));

        ArgumentCaptor<List<Task>> saveCaptor = ArgumentCaptor.forClass(List.class);

        // when
        taskService.saveTasksFromAiResponse(project, wbs);

        // then
        then(taskRepository).should().saveAll(saveCaptor.capture());
        then(projectRepository).should().updateLastModifiedDate(project.getId());

        List<Task> saved = saveCaptor.getValue();
        assertThat(saved).hasSize(2);

        Task parent = saved.stream().filter(t -> t.getName().equals("Parent")).findFirst().orElseThrow();
        Task child = saved.stream().filter(t -> t.getName().equals("Child")).findFirst().orElseThrow();

        assertThat(child.getParent()).isSameAs(parent);
    }

    @DisplayName("AI WBS 응답이 비어 있는 경우, 아무 작업도 저장하지 않는다.")
    @Test
    void AI_WBS_비어있으면_저장안함() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .name("u")
                .googleId("gid")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("proj")
                .projectType("type")
                .teamSize(3)
                .expectedDurationMonths(1)
                .startDate(LocalDate.parse("2025-01-01"))
                .endDate(LocalDate.parse("2025-02-01"))
                .budget(new BigDecimal("100000"))
                .priority(ProjectPriority.MEDIUM)
                .stakeholders("member1")
                .deliverables("deliverable")
                .risks("risk")
                .detailedRequirements("req")
                .build();
        ReflectionTestUtils.setField(project, "id", 1L);

        AiWbsResponseDto wbs = Mockito.mock(AiWbsResponseDto.class);
        given(wbs.tasks()).willReturn(List.of());

        // when
        taskService.saveTasksFromAiResponse(project, wbs);

        // then
        then(taskRepository).should(never()).saveAll(anyList());
        then(projectRepository).should(never()).updateLastModifiedDate(anyLong());
    }

    @DisplayName("프로젝트와 모든 작업을 조회한다.")
    @Test
    void 프로젝트와_작업_조회() {
        // given
        String userId = "u1";
        Long projectId = 10L;

        User user = User.builder()
                .email("test")
                .name("n")
                .googleId("gid")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("proj")
                .projectType("typeA")
                .teamSize(3)
                .expectedDurationMonths(2)
                .startDate(LocalDate.parse("2025-01-01"))
                .endDate(LocalDate.parse("2025-03-01"))
                .budget(new BigDecimal("500000"))
                .priority(ProjectPriority.HIGH)
                .stakeholders("member1")
                .deliverables("deliverableA")
                .risks("riskA")
                .detailedRequirements("reqA")
                .build();
        ReflectionTestUtils.setField(project, "id", projectId);

        ProjectMember member = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.VIEWER)
                .build();

        Task t1 = Task.builder()
                .project(project)
                .name("T1")
                .status(TaskStatus.TODO)
                .progress(0)
                .build();
        Task t2 = Task.builder()
                .project(project)
                .name("T2")
                .status(TaskStatus.DONE)
                .progress(100)
                .build();

        given(projectMemberValidator.validatePermission(userId, projectId, ProjectRole.VIEWER))
                .willReturn(member);
        given(taskRepository.findAllByProjectId(projectId))
                .willReturn(List.of(t1, t2));

        // when
        ProjectWithTasksResponseDto result = taskService.getProjectWithTasks(projectId, userId);

        // then
        assertThat(result.tasks()).hasSize(2);
    }

    @DisplayName("작업 생성 후 부모가 존재하면 부모 진행률을 재계산한다.")
    @Test
    void 작업_생성시_부모_진행률_재계산_테스트() {
        // given
        String userId = "u1";
        Long projectId = 10L;

        User user = User.builder()
                .email("t")
                .name("n")
                .googleId("gid")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("p")
                .projectType("type")
                .teamSize(3)
                .expectedDurationMonths(3)
                .startDate(LocalDate.parse("2025-01-01"))
                .endDate(LocalDate.parse("2025-04-01"))
                .budget(new BigDecimal("700000"))
                .priority(ProjectPriority.MEDIUM)
                .stakeholders("member1")
                .deliverables("deliverable")
                .risks("risk")
                .detailedRequirements("req")
                .build();
        ReflectionTestUtils.setField(project, "id", projectId);

        ProjectMember member = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.EDITOR)
                .build();

        Task parent = Task.builder()
                .project(project)
                .name("parent")
                .status(TaskStatus.TODO)
                .progress(0)
                .build();
        ReflectionTestUtils.setField(parent, "id", 1L);

        CreateTaskRequestDto dto = new CreateTaskRequestDto(
                "child",
                1L,
                "test@test.com",
                "2025-01-02",
                "2025-01-10",
                "TODO",
                50
        );

        User assignee = user;

        Task savedTask = Task.builder()
                .project(project)
                .parent(parent)
                .name("child")
                .progress(50)
                .status(TaskStatus.TODO)
                .build();

        given(projectMemberValidator.validatePermission(userId, projectId, ProjectRole.EDITOR))
                .willReturn(member);
        given(taskValidator.validateAndGetParentTask(1L))
                .willReturn(parent);
        given(taskValidator.validateAndGetAssignee(project, "test@test.com"))
                .willReturn(assignee);
        given(taskRepository.save(any(Task.class)))
                .willReturn(savedTask);
        given(taskRepository.findAllByParentId(parent.getId()))
                .willReturn(List.of(savedTask));

        // when
        TaskFlatResponseDto result = taskService.createTask(projectId, dto, userId);

        // then
        then(taskRepository).should(atLeastOnce()).save(any(Task.class));
        then(taskRepository).should().flush();
        assertThat(parent.getProgress()).isEqualTo(50);
        assertThat(result.name()).isEqualTo("child");
    }

    @DisplayName("작업 완료(DONE) 시, 모든 하위 작업까지 DONE으로 변경한다.")
    @Test
    void 작업_DONE_변경시_자식_전체_DONE_변경() {
        // given
        String userId = "uid";
        Long taskId = 100L;

        User user = User.builder()
                .email("t")
                .name("n")
                .googleId("gid")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("proj")
                .projectType("type")
                .teamSize(4)
                .expectedDurationMonths(4)
                .startDate(LocalDate.parse("2025-02-01"))
                .endDate(LocalDate.parse("2025-06-01"))
                .budget(new BigDecimal("900000"))
                .priority(ProjectPriority.HIGH)
                .stakeholders("member2")
                .deliverables("deliverable2")
                .risks("risk2")
                .detailedRequirements("req2")
                .build();
        ReflectionTestUtils.setField(project, "id", 10L);

        ProjectMember member = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.EDITOR)
                .build();

        Task parent = Task.builder()
                .project(project)
                .name("parent")
                .status(TaskStatus.TODO)
                .progress(0)
                .build();
        ReflectionTestUtils.setField(parent, "id", taskId);

        Task c1 = Task.builder()
                .project(project)
                .parent(parent)
                .name("c1")
                .status(TaskStatus.IN_PROGRESS)
                .progress(40)
                .build();
        Task c2 = Task.builder()
                .project(project)
                .parent(parent)
                .name("c2")
                .status(TaskStatus.TODO)
                .progress(0)
                .build();

        UpdateTaskRequestDto dto = new UpdateTaskRequestDto(
                "parent",
                "test@test.com",
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-01-10"),
                "DONE",
                100
        );

        given(taskValidator.validateAndGetTask(taskId)).willReturn(parent);
        given(projectMemberValidator.validatePermission(userId, project.getId(), ProjectRole.EDITOR))
                .willReturn(member);
        given(taskValidator.validateAndGetAssignee(project, "test@test.com"))
                .willReturn(user);
        given(taskRepository.findAllByParentId(parent.getId()))
                .willReturn(List.of(c1, c2));

        // when
        taskService.updateTask(taskId, dto, userId);

        // then
        assertThat(c1.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(c1.getProgress()).isEqualTo(100);
        assertThat(c2.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(c2.getProgress()).isEqualTo(100);

        then(taskRepository).should(atLeastOnce()).saveAll(anyList());
        then(projectRepository).should().updateLastModifiedDate(project.getId());
    }

    @DisplayName("작업 삭제 후, 부모 진행률을 재계산한다.")
    @Test
    void 작업_삭제시_부모_진행률_재계산() {
        // given
        String userId = "uid";
        Long taskId = 200L;

        User user = User.builder()
                .email("t")
                .name("n")
                .googleId("gid")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("proj")
                .projectType("type")
                .teamSize(2)
                .expectedDurationMonths(2)
                .startDate(LocalDate.parse("2025-03-01"))
                .endDate(LocalDate.parse("2025-05-01"))
                .budget(new BigDecimal("300000"))
                .priority(ProjectPriority.MEDIUM)
                .stakeholders("member3")
                .deliverables("deliverable3")
                .risks("risk3")
                .detailedRequirements("req3")
                .build();
        ReflectionTestUtils.setField(project, "id", 10L);

        Task parent = Task.builder()
                .project(project)
                .name("parent")
                .progress(50)
                .status(TaskStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(parent, "id", 1L);

        Task task = Task.builder()
                .project(project)
                .parent(parent)
                .name("child")
                .progress(30)
                .status(TaskStatus.TODO)
                .build();
        ReflectionTestUtils.setField(task, "id", taskId);

        ProjectMember member = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.EDITOR)
                .build();

        given(taskValidator.validateAndGetTask(taskId)).willReturn(task);
        given(projectMemberValidator.validatePermission(userId, project.getId(), ProjectRole.EDITOR))
                .willReturn(member);
        given(taskRepository.findAllByParentId(parent.getId()))
                .willReturn(List.of());

        // when
        taskService.deleteTask(taskId, userId);

        // then
        then(taskRepository).should().delete(task);
        then(taskRepository).should().flush();
        then(projectRepository).should().updateLastModifiedDate(project.getId());
    }
}
