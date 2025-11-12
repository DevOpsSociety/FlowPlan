package com.hanmo.flowplan.ai.application;

import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecResponseDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

  private final AiClient aiClient;

  public AiSpecResponseDto generateMarkdownSpec(AiSpecRequestDto specRequestDto) {
    // AiClient의 1번 API 호출
    return aiClient.generateMarkdownSpec(specRequestDto);
  }

  public AiWbsResponseDto generateWbsFromMarkdown(String markdownContent) {
    // AiClient의 2번 API 호출
    AiWbsRequestDto wbsRequestDto = new AiWbsRequestDto(markdownContent);
    return aiClient.generateWbsFromMarkdown(wbsRequestDto);
  }

}
