package com.hanmo.flowplan.project.presentation.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateProjectRequest {

  String projectName;
  String projectType;
  int teamSize;
  int expectedDurationDays;
  String startDate;
  String endDate;
  String budget;
  String priority;
  List<String> stakeholders;
  List<String> deliverables;
  List<String> risks;
  String detailedRequirements;
}
