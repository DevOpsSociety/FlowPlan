package com.hanmo.flowplan.ai.appilcation;


import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecResponseDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;

public interface AiClient {
  // 마크다운 언어 생성
  AiSpecResponseDto generateMarkdownSpec(AiSpecRequestDto requestDto);

  // 마크다운 언어로 WBS 생성
  AiWbsResponseDto generateWbsFromMarkdown(AiWbsRequestDto requestDto);
}
