package com.hanmo.flowplan.project.presentation;

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

  @Operation(summary = "내 프로젝트 목록 조회", description = "내가 멤버로 참여중인 모든 프로젝트를 최신 수정순으로 조회합니다.")
  @GetMapping
  public ResponseEntity<List<ProjectListResponse>> getProjects(@AuthenticationPrincipal CustomUserDetails userDetails) {
    // CustomUserDetails에서 googleId 추출
    String googleId = userDetails.getGoogleId();

    // Service 호출
    List<ProjectListResponse> projects = projectService.findAllProjects(googleId);

    return ResponseEntity.ok(projects);
  }

  @Operation(summary = "프로젝트 생성 & AI 명세서 요청", description = "프로젝트 정보를 입력받아 저장하고, AI에게 마크다운 명세서를 요청합니다.")
  @PostMapping
  ResponseEntity<CreateProjectWithSpecResponse> createProject(@Valid @RequestBody CreateProjectRequest createProjectRequest,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
    String googleId = userDetails.getGoogleId();

    CreateProjectWithSpecResponse response = projectService.createProjectAndGenerateSpec(createProjectRequest, googleId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "WBS 생성 & Task 저장", description = "확정된 마크다운 명세서를 AI에게 보내 WBS를 생성하고 DB에 저장합니다.")
  @PostMapping("/wbs")
  ResponseEntity<Void> generateWbs(@RequestBody GenerateWbsRequestDto generateWbsRequestDto,
                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
    String googleId = userDetails.getGoogleId();
    projectService.generateWbsAndSaveTasks(generateWbsRequestDto, googleId);
    return ResponseEntity.ok().build();
  }


  @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제합니다. 프로젝트를 생성한 소유자(Owner)만 삭제할 수 있습니다.")
  @DeleteMapping("/{projectId}")
  public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

    String googleId = userDetails.getGoogleId();
    // 서비스 호출 (소유자 검증 및 삭제 로직 수행)
    projectService.deleteProject(projectId, googleId);

    // 삭제 성공 시 204 No Content 반환
    return ResponseEntity.noContent().build();
  }

}
