package com.hanmo.flowplan.project.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


public record GenerateWbsRequestDto(
    @NotNull(message = "프로젝트 ID는 필수입니다.")
    Long projectId,

    @NotBlank(message = "마크다운 스펙 내용은 필수입니다.")
    String markdownContent // 필드명은 markdownSpec 등도 좋습니다.
) {
  @Builder
  public GenerateWbsRequestDto(Long projectId, String markdownContent) {
    this.projectId = projectId;
    this.markdownContent = markdownContent;
  }
}
