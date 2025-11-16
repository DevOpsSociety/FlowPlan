package com.hanmo.flowplan.project.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class CreateProjectRequest {

  @NotBlank
  String projectName;
  @NotBlank
  String projectType;
  @NotNull
  int teamSize;
  @NotNull
  int expectedDurationDays;

  // 추가 요구사항 필드
  String startDate;
  String endDate;
  BigDecimal budget;
  String priority;
  List<String> stakeholders;
  List<String> deliverables;
  List<String> risks;
  String detailedRequirements;
}
