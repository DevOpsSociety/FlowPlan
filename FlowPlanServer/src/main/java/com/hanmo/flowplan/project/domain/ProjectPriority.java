package com.hanmo.flowplan.project.domain;

import lombok.Getter;

@Getter // ⭐️ 1. "getKoreanName()"을 만들기 위해 @Getter 추가
public enum ProjectPriority {

  // ⭐️ 2. 생성자를 통해 한글 값 지정
  HIGH("높음"),
  MEDIUM("중간"),
  LOW("낮음");

  // ⭐️ 3. 한글 값을 저장할 필드
  private final String koreanName;

  // ⭐️ 4. Enum 생성자
  ProjectPriority(String koreanName) {
    this.koreanName = koreanName;
  }
}