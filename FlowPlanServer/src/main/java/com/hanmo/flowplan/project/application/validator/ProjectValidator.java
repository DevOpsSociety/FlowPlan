package com.hanmo.flowplan.project.application.validator;

import com.hanmo.flowplan.project.domain.Project;
import com.hanmo.flowplan.project.domain.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectValidator {

  private final ProjectRepository projectRepository;

  public Project validateAndGetProject(Long projectId) {
    return projectRepository.findById(projectId)
        .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));
  }

}
