package com.hanmo.flowplan.projectMember;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.hanmo.flowplan.global.error.exception.BusinessException;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.repository.ProjectRepository;
import com.hanmo.flowplan.projectMember.application.ProjectMemberService;
import com.hanmo.flowplan.projectMember.application.validator.ProjectMemberValidator;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.projectMember.domain.ProjectRole;
import com.hanmo.flowplan.projectMember.presentation.dto.ProjectMemberResponse;
import com.hanmo.flowplan.user.application.validator.UserValidator;
import com.hanmo.flowplan.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    ProjectMemberRepository projectMemberRepository;

    @Mock
    ProjectRepository projectRepository;

    @Mock
    UserValidator userValidator;

    @Mock
    ProjectMemberValidator projectMemberValidator;

    @InjectMocks
    ProjectMemberService projectMemberService;

    @DisplayName("VIEWER 멤버가 권한 변경을 요청하면 PENDING 상태로 변경된다.")
    @Test
    void 권한_변경_요청_VIEWER에서_PENDING으로_변경_테스트() {
        // given
        Long projectId = 1L;
        String userId = "user-google-id";

        User user = User.builder()
                .googleId(userId)
                .email("test@test.com")
                .name("user")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("proj")
                .build();

        ProjectMember member = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.VIEWER)
                .build();

        given(userValidator.validateAndGetUser(userId)).willReturn(user);
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByUserAndProject(user, project))
                .willReturn(Optional.of(member));

        // when
        projectMemberService.requestRoleChange(projectId, userId);

        // then
        assertThat(member.getProjectRole()).isEqualTo(ProjectRole.PENDING);
    }

    @DisplayName("이미 EDITOR 이상인 경우 권한 변경 요청 시 예외가 발생한다.")
    @Test
    void 권한_변경_요청_이미_EDITOR면_예외_테스트() {
        // given
        Long projectId = 1L;
        String userId = "user-google-id";

        User user = User.builder()
                .googleId(userId)
                .email("test@test.com")
                .name("user")
                .build();

        Project project = Project.builder()
                .owner(user)
                .projectName("proj")
                .build();

        ProjectMember member = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.EDITOR)
                .build();

        given(userValidator.validateAndGetUser(userId)).willReturn(user);
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByUserAndProject(user, project))
                .willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> projectMemberService.requestRoleChange(projectId, userId))
                .isInstanceOf(BusinessException.class);
    }

    @DisplayName("OWNER가 PENDING 상태의 멤버 권한 변경을 승인하면 EDITOR로 승급된다.")
    @Test
    void 권한_변경_승인_시_PENDING에서_EDITOR로_변경_테스트() {
        // given
        Long projectId = 1L;
        Long memberId = 10L;
        String ownerId = "owner-google-id";

        User owner = User.builder()
                .googleId(ownerId)
                .email("owner@test.com")
                .name("owner")
                .build();

        Project project = Project.builder()
                .owner(owner)
                .projectName("proj")
                .build();

        ProjectMember ownerMember = ProjectMember.builder()
                .user(owner)
                .project(project)
                .role(ProjectRole.OWNER)
                .build();

        User targetUser = User.builder()
                .googleId("member-google-id")
                .email("member@test.com")
                .name("member")
                .build();

        ProjectMember pendingMember = ProjectMember.builder()
                .user(targetUser)
                .project(project)
                .role(ProjectRole.PENDING)
                .build();

        given(projectMemberValidator.validatePermission(ownerId, projectId, ProjectRole.OWNER))
                .willReturn(ownerMember);
        given(projectMemberRepository.findById(memberId))
                .willReturn(Optional.of(pendingMember));

        // when
        projectMemberService.approveRoleChange(projectId, memberId, ownerId);

        // then
        assertThat(pendingMember.getProjectRole()).isEqualTo(ProjectRole.EDITOR);
    }

    @DisplayName("프로젝트 멤버 조회 시, VIEWER 이상 권한 검증 후 모든 멤버를 반환한다.")
    @Test
    void 프로젝트_멤버_조회_테스트() {
        // given
        Long projectId = 1L;
        String userId = "viewer-google-id";

        User owner = User.builder()
                .googleId("owner")
                .email("owner@test.com")
                .name("owner")
                .build();

        Project project = Project.builder()
                .owner(owner)
                .projectName("proj")
                .build();

        User viewerUser = User.builder()
                .googleId(userId)
                .email("viewer@test.com")
                .name("viewer")
                .build();

        ProjectMember viewerMember = ProjectMember.builder()
                .user(viewerUser)
                .project(project)
                .role(ProjectRole.VIEWER)
                .build();

        ProjectMember editorMember = ProjectMember.builder()
                .user(User.builder().googleId("editor").email("editor@test.com").name("editor").build())
                .project(project)
                .role(ProjectRole.EDITOR)
                .build();

        given(projectMemberValidator.validatePermission(userId, projectId, ProjectRole.VIEWER))
                .willReturn(viewerMember);
        given(projectMemberRepository.findAllByProjectId(projectId))
                .willReturn(List.of(viewerMember, editorMember));

        // when
        List<ProjectMemberResponse> result = projectMemberService.getProjectMembers(projectId, userId);

        // then
        assertThat(result).hasSize(2);
    }

    @DisplayName("OWNER는 다른 멤버를 추방할 수 있다.")
    @Test
    void OWNER_다른_멤버_추방_테스트() {
        // given
        Long projectId = 1L;
        Long memberIdToKick = 100L;
        String ownerGoogleId = "owner-google-id";

        User owner = User.builder()
                .googleId(ownerGoogleId)
                .email("owner@test.com")
                .name("owner")
                .build();

        Project project = Project.builder()
                .owner(owner)
                .projectName("proj")
                .build();

        ProjectMember ownerMember = ProjectMember.builder()
                .user(owner)
                .project(project)
                .role(ProjectRole.OWNER)
                .build();

        User targetUser = User.builder()
                .googleId("target-google-id")
                .email("target@test.com")
                .name("target")
                .build();

        ProjectMember targetMember = ProjectMember.builder()
                .user(targetUser)
                .project(project)
                .role(ProjectRole.VIEWER)
                .build();

        given(projectMemberValidator.validatePermission(ownerGoogleId, projectId, ProjectRole.OWNER))
                .willReturn(ownerMember);
        given(projectMemberRepository.findById(memberIdToKick))
                .willReturn(Optional.of(targetMember));

        // when
        projectMemberService.kickMember(projectId, memberIdToKick, ownerGoogleId);

        // then
        then(projectMemberRepository).should().delete(targetMember);
    }

    @DisplayName("OWNER는 자기 자신을 추방할 수 없으며, 예외가 발생한다.")
    @Test
    void OWNER는_자기자신을_추방_예외_테스트() {
        // given
        Long projectId = 1L;
        Long memberIdToKick = 100L;
        String ownerGoogleId = "owner-google-id";

        User owner = User.builder()
                .googleId(ownerGoogleId)
                .email("owner@test.com")
                .name("owner")
                .build();

        Project project = Project.builder()
                .owner(owner)
                .projectName("proj")
                .build();

        ProjectMember ownerMember = ProjectMember.builder()
                .user(owner)
                .project(project)
                .role(ProjectRole.OWNER)
                .build();

        given(projectMemberValidator.validatePermission(ownerGoogleId, projectId, ProjectRole.OWNER))
                .willReturn(ownerMember);
        given(projectMemberRepository.findById(memberIdToKick))
                .willReturn(Optional.of(ownerMember));

        // when & then
        assertThatThrownBy(() -> projectMemberService.kickMember(projectId, memberIdToKick, ownerGoogleId))
                .isInstanceOf(BusinessException.class);

        then(projectMemberRepository).should(never()).delete(ownerMember);
    }

    @DisplayName("일반 멤버는 프로젝트를 나갈 수 있다.")
    @Test
    void 일반_멤버_프로젝트_나가기_테스트() {
        // given
        Long projectId = 1L;
        String userGoogleId = "member-google-id";

        User user = User.builder()
                .googleId(userGoogleId)
                .email("member@test.com")
                .name("member")
                .build();

        User owner = User.builder()
                .googleId("owner")
                .email("owner@test.com")
                .name("owner")
                .build();

        Project project = Project.builder()
                .owner(owner)
                .projectName("proj")
                .build();

        ProjectMember member = ProjectMember.builder()
                .user(user)
                .project(project)
                .role(ProjectRole.VIEWER)
                .build();

        given(userValidator.validateAndGetUser(userGoogleId)).willReturn(user);
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByUserAndProject(user, project))
                .willReturn(Optional.of(member));

        // when
        projectMemberService.leaveProject(projectId, userGoogleId);

        // then
        then(projectMemberRepository).should().delete(member);
    }

    @DisplayName("OWNER는 프로젝트를 나갈 수 없으며, 예외가 발생한다.")
    @Test
    void OWNER_프로젝트_나가기_예외_테스트() {
        // given
        Long projectId = 1L;
        String ownerGoogleId = "owner-google-id";

        User owner = User.builder()
                .googleId(ownerGoogleId)
                .email("owner@test.com")
                .name("owner")
                .build();

        Project project = Project.builder()
                .owner(owner)
                .projectName("proj")
                .build();

        ProjectMember ownerMember = ProjectMember.builder()
                .user(owner)
                .project(project)
                .role(ProjectRole.OWNER)
                .build();

        given(userValidator.validateAndGetUser(ownerGoogleId)).willReturn(owner);
        given(projectRepository.findById(projectId)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByUserAndProject(owner, project))
                .willReturn(Optional.of(ownerMember));

        // when & then
        assertThatThrownBy(() -> projectMemberService.leaveProject(projectId, ownerGoogleId))
                .isInstanceOf(BusinessException.class);

        then(projectMemberRepository).should(never()).delete(ownerMember);
    }
}
