package com.hanmo.flowplan.task.domain;

public enum TaskStatus {
  TODO("할일"),
  IN_PROGRESS("진행중"),
  DONE("완료");

  private final String koreanName;

  TaskStatus(String koreanName) {
    this.koreanName = koreanName;
  }
}
