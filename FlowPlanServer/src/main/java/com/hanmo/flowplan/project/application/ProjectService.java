package com.hanmo.flowplan.project.application;

import com.hanmo.flowplan.ai.application.AiDtoMapper;
import com.hanmo.flowplan.ai.application.AiService;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecResponseDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import com.hanmo.flowplan.project.application.dto.CreateProjectWithSpecResponse;
import com.hanmo.flowplan.project.application.dto.ProjectListResponse;
import com.hanmo.flowplan.project.application.validator.ProjectValidator;
import com.hanmo.flowplan.project.domain.Project;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;

  private final UserValidator userValidator;
  private final ProjectValidator projectValidator;
  private final ProjectMemberValidator projectMemberValidator;
  private final AiDtoMapper aiDtoMapper; // (DTO 변환기)
  private final AiService aiService;     // (AI 호출 담당)
  private final TaskService taskService;   // (WBS 저장 담당)

  @Transactional
  public CreateProjectWithSpecResponse createProjectAndGenerateSpec(CreateProjectRequest createProjectRequest, String userId) {

    User owner = userValidator.validateAndGetUser(userId);

    // 1. (Project 저장)
    Project savedProject = projectRepository.save(createProjectRequest.toEntity(owner));

    // 프로젝트와 소유자 연결 저장
    ProjectMember projectMember = ProjectMember.builder()
        .user(savedProject.getOwner())
        .project(savedProject)
        .role(ProjectRole.OWNER)
        .build();

    projectMemberRepository.save(projectMember);

    // 2. (Project -> DTO 변환)
    AiSpecRequestDto specRequestDto = aiDtoMapper.toSpecRequestDto(savedProject);

    // 3. (AI 1단계 호출) - 명세서 생성
    AiSpecResponseDto specResponseDto = aiService.generateMarkdownSpec(specRequestDto);

    // 4. (결과 반환) - Project ID와 마크다운 반환
    return new CreateProjectWithSpecResponse(
        savedProject.getId(),
        specResponseDto.markdownSpec()
    );
  }


  @Transactional
  public void generateWbsAndSaveTasks(GenerateWbsRequestDto generateWbsRequestDto, String userId) {

    ProjectMember member = projectMemberValidator.validatePermission(
        userId,
        generateWbsRequestDto.projectId(),
        ProjectRole.EDITOR
    );
    // 1. (AI 2단계 호출) - WBS 생성
    AiWbsResponseDto wbsResponseDto = aiService.generateWbsFromMarkdown(generateWbsRequestDto.markdownContent());

    // 2. (WBS 저장) - TaskService를 통해 WBS 항목 저장
    taskService.saveTasksFromAiResponse(member.getUser(), member.getProject(), wbsResponseDto);
  }

  @Transactional(readOnly = true)
  public List<ProjectListResponse> findAllProjects(String userId) {
    User user = userValidator.validateAndGetUser(userId);

    // 1. 내가 멤버로 속한 모든 프로젝트 멤버십 조회
    List<ProjectMember> memberships = projectMemberRepository.findAllByUser(user);

    // 2. Project 엔티티 추출 -> DTO 변환 -> 최신순 정렬
    return memberships.stream()
        .map(ProjectMember::getProject) // Project 객체 꺼내기
        .sorted(Comparator.comparing(Project::getUpdatedAt).reversed()) // ⭐️ 최신 수정일 순 정렬
        .map(ProjectListResponse::from) // DTO 변환
        .collect(Collectors.toList());
  }

  // ============================================================
  // ⭐️ [추가] API 4: 프로젝트 삭제 (Owner만 가능)
  // ============================================================
  @Transactional
  public void deleteProject(Long projectId, String userId) {
    ProjectMember member = projectMemberValidator.validatePermission(userId, projectId, ProjectRole.OWNER);

    // 2. 프로젝트 삭제
    // (CascadeType.ALL 설정에 의해 연결된 Task, ProjectMember도 함께 삭제됨)
    projectRepository.delete(member.getProject());
  }


}
