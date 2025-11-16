package com.hanmo.flowplan.project.presentation;

import com.hanmo.flowplan.global.jwt.CustomUserDetails;
import com.hanmo.flowplan.project.application.ProjectService;
import com.hanmo.flowplan.project.application.dto.CreateProjectWithSpecResponse;
import com.hanmo.flowplan.project.presentation.dto.CreateProjectRequest;
import com.hanmo.flowplan.project.presentation.dto.GenerateWbsRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Project API", description = "프로젝트 관련 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;

  @PostMapping
  ResponseEntity<CreateProjectWithSpecResponse> createProject(@Valid @RequestBody CreateProjectRequest createProjectRequest,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
    String googleId = userDetails.getGoogleId();

    CreateProjectWithSpecResponse response = projectService.createProjectAndGenerateSpec(createProjectRequest, googleId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/wbs")
  ResponseEntity<Void> generateWbs(@RequestBody GenerateWbsRequestDto generateWbsRequestDto,
                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
    String googleId = userDetails.getGoogleId();
    projectService.generateWbsAndSaveTasks(generateWbsRequestDto, googleId);
    return ResponseEntity.ok().build();
  }
}
