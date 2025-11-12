package com.hanmo.flowplan.project.application;

import com.hanmo.flowplan.ai.application.AiDtoMapper;
import com.hanmo.flowplan.ai.application.AiService;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecResponseDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import com.hanmo.flowplan.global.jwt.JwtProvider;
import com.hanmo.flowplan.project.application.dto.CreateProjectWithSpecResponse;
import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectRepository;
import com.hanmo.flowplan.project.presentation.dto.CreateProjectRequest;
import com.hanmo.flowplan.project.presentation.dto.GenerateWbsRequestDto;
import com.hanmo.flowplan.projectMember.domain.ProjectMember;
import com.hanmo.flowplan.projectMember.domain.ProjectMemberRepository;
import com.hanmo.flowplan.task.application.TaskService;
import com.hanmo.flowplan.user.domain.User;
import com.hanmo.flowplan.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final JwtProvider jwtProvider;
  private final AiDtoMapper aiDtoMapper; // (DTO 변환기)
  private final AiService aiService;     // (AI 호출 담당)
  private final TaskService taskService;   // (WBS 저장 담당)

  @Transactional
  public CreateProjectWithSpecResponse createProjectAndGenerateSpec(CreateProjectRequest createProjectRequest, String token) {

    String googleId = jwtProvider.getGoogleIdFromToken(token);
    User owner = userRepository.findByGoogleId(googleId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + googleId));

    // 1. (Project 저장)
    Project savedProject = projectRepository.save(Project.createProject(createProjectRequest));

    // 프로젝트와 소유자 연결 저장
    ProjectMember projectMember = ProjectMember.builder()
        .user(owner)
        .project(savedProject)
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
  public void generateWbsAndSaveTasks(GenerateWbsRequestDto generateWbsRequestDto, String token) {

    String googleId = jwtProvider.getGoogleIdFromToken(token);

    User user = userRepository.findByGoogleId(googleId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + googleId));

    Project project = projectRepository.findById(generateWbsRequestDto.getProjectId())
        .orElseThrow(() -> new IllegalArgumentException("Project does not exist with id: " + generateWbsRequestDto.getProjectId()));

    ProjectMember projectMember = projectMemberRepository.findByUserAndProject(user, project)
        .orElseThrow(() -> new IllegalArgumentException("User is not a member of the project or project does not exist."));

    // 1. (AI 2단계 호출) - WBS 생성
    AiWbsResponseDto wbsResponseDto = aiService.generateWbsFromMarkdown(generateWbsRequestDto.getMarkdownContent());

    // 2. (WBS 저장) - TaskService를 통해 WBS 항목 저장
    taskService.saveTasksFromAiResponse(user, project, wbsResponseDto);

  }

}
