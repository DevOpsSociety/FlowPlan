package com.hanmo.flowplan.ai.infrastructure;

import com.hanmo.flowplan.ai.application.AiClient;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiSpecResponseDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsRequestDto;
import com.hanmo.flowplan.ai.infrastructure.dto.AiWbsResponseDto;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FlowPlanAiClientImpl implements AiClient {

  private final RestTemplate restTemplate;

  @Value("${AI_SERVICE_BASE_URL}")
  private String aiServiceBaseUrl;

  @Override
  public AiSpecResponseDto generateMarkdownSpec(AiSpecRequestDto requestDto) {
    String url = aiServiceBaseUrl + "/api/v1/wbs/generate-spec";

    try {
      return restTemplate.postForObject(url, requestDto, AiSpecResponseDto.class);
    } catch (Exception e) {
      // TODO: AI 서버 호출 실패 시 예외 처리
      throw new RuntimeException("AI 명세서 생성에 실패했습니다: " + e.getMessage());
    }

  }

  @Override
  public AiWbsResponseDto generateWbsFromMarkdown(AiWbsRequestDto requestDto) {
    // AI API 명세서의 엔드포인트
    String url = aiServiceBaseUrl + "/api/v1/wbs/generate-from-spec/flat";

    try {
      return restTemplate.postForObject(url, requestDto, AiWbsResponseDto.class);
    } catch (Exception e) {
      // TODO: AI 서버 호출 실패 시 예외 처리
      throw new RuntimeException("AI WBS 생성에 실패했습니다: " + e.getMessage());
    }
  }
}
