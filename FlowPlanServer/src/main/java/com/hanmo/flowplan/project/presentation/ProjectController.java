package com.hanmo.flowplan.project.presentation;

import com.hanmo.flowplan.project.application.ProjectService;
import com.hanmo.flowplan.project.application.dto.CreateProjectWithSpecResponse;
import com.hanmo.flowplan.project.presentation.dto.CreateProjectRequest;
import com.hanmo.flowplan.project.presentation.dto.GenerateWbsRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Project API", description = "프로젝트 관련 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;

  @PostMapping
  ResponseEntity<CreateProjectWithSpecResponse> createProject(@RequestBody CreateProjectRequest createProjectRequest, @RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(projectService.createProjectAndGenerateSpec(createProjectRequest, token));
  }

  @PostMapping("/wbs")
  ResponseEntity<Void> generateWbs(@RequestBody GenerateWbsRequestDto generateWbsRequestDto, @RequestHeader("Authorization") String token) {
    projectService.generateWbsAndSaveTasks(generateWbsRequestDto, token);
    return ResponseEntity.ok().build();
  }
}
