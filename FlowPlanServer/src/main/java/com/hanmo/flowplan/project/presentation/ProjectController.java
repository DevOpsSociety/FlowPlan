package com.hanmo.flowplan.project.presentation;

import com.hanmo.flowplan.global.annotation.CurrentUserId;
import com.hanmo.flowplan.global.jwt.CustomUserDetails;
import com.hanmo.flowplan.project.application.ProjectService;
import com.hanmo.flowplan.project.application.dto.CreateProjectWithSpecResponse;
import com.hanmo.flowplan.project.application.dto.ProjectListResponse;
import com.hanmo.flowplan.project.presentation.dto.CreateProjectRequest;
import com.hanmo.flowplan.project.presentation.dto.GenerateWbsRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Project API", description = "프로젝트 관련 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;

  @Operation(summary = "내 프로젝트 목록 조회", description = "내가 참여 중인 프로젝트를 최근 수정된 순서(Touch 반영)대로 조회합니다.")
  @GetMapping
  public ResponseEntity<List<ProjectListResponse>> getProjects(@CurrentUserId String userId) {
    List<ProjectListResponse> projects = projectService.findAllProjects(userId);
    return ResponseEntity.ok(projects);
  }

  @Operation(summary = "프로젝트 생성 & AI 명세서 요청", description = "프로젝트 정보를 입력받아 저장하고, AI에게 마크다운 명세서를 요청합니다.")
  @PostMapping
  ResponseEntity<CreateProjectWithSpecResponse> createProject(@Valid @RequestBody CreateProjectRequest createProjectRequest,
                                                              @CurrentUserId String userId) {
    CreateProjectWithSpecResponse response = projectService.createProjectAndGenerateSpec(createProjectRequest, userId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "WBS 생성 & Task 저장", description = "확정된 마크다운 명세서를 AI에게 보내 WBS를 생성하고 DB에 저장합니다.")
  @PostMapping("/wbs")
  ResponseEntity<Void> generateWbs(@RequestBody GenerateWbsRequestDto generateWbsRequestDto,
                                   @CurrentUserId String userId) {
    projectService.generateWbsAndSaveTasks(generateWbsRequestDto, userId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "프로젝트 삭제", description = "프로젝트 소유자(Owner)만 삭제할 수 있습니다.")
  @DeleteMapping("/{projectId}")
  public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                            @CurrentUserId String userId) {
    projectService.deleteProject(projectId, userId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

}
