package com.hanmo.flowplan.project.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateProjectWithSpecResponse {
  private Long projectId;
  private String markdownContent;
}
