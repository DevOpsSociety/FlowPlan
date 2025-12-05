from groq import Groq
from app.core.config import settings
from typing import Dict, Any
import asyncio
from datetime import date


class GeminiService:
    """Groq API 서비스 (프롬프트 빌드 + 호출)

    단일, 일관된 구현만 포함합니다. 이 클래스는 세 가지 공개 메서드를 제공합니다:
    - generate_markdown_spec(project_data)
    - generate_wbs_from_markdown(markdown_spec)
    - generate_wbs_structure(project_data)
    
    Note: 클래스명은 호환성을 위해 GeminiService로 유지하지만 내부적으로 Groq API를 사용합니다.
    """

    def __init__(self) -> None:
        """Groq 클라이언트 초기화"""
        self.client = Groq(api_key=settings.GROQ_API_KEY)
        self.model_name = settings.GROQ_MODEL

    async def generate_markdown_spec(self, project_data: Dict[str, Any]) -> str:
        """프로젝트 정보를 받아 편집 가능한 마크다운 명세서를 반환합니다."""
        prompt = self._build_markdown_prompt(project_data)
        return await self._generate_content(prompt, json_mode=False)

    async def generate_wbs_from_markdown(self, markdown_spec: str) -> str:
        """마크다운 명세서로부터 JSON WBS 구조(문자열)를 생성합니다."""
        prompt = self._build_wbs_from_markdown_prompt(markdown_spec)
        return await self._generate_content(prompt, json_mode=True)

    async def generate_wbs_structure(self, project_data: Dict[str, Any]) -> str:
        """프로젝트 정보를 받아 JSON WBS 구조(문자열)를 생성합니다."""
        prompt = self._build_wbs_prompt(project_data)
        return await self._generate_content(prompt, json_mode=True)

    def _build_common_header(self, data: Dict[str, Any]) -> str:
        parts = [
            "당신은 프로젝트 관리 전문가입니다. 다음 정보를 기반으로 작업을 수행하세요.",
            f"현재 날짜(기준일): {date.today().isoformat()}",
            f"프로젝트명: {data.get('project_name')}",
            f"프로젝트 주제: {data.get('project_type')}",
            f"참여인원: {data.get('team_size')}",
        ]

        if data.get('start_date') and data.get('end_date'):
            parts.append(f"기간: {data.get('start_date')} ~ {data.get('end_date')} ({data.get('total_days')}일)")
        else:
            parts.append(f"예상 기간: {data.get('total_days')}일")

        if data.get('budget'):
            parts.append(f"예산: {data.get('budget')}")
        if data.get('priority'):
            parts.append(f"우선순위: {data.get('priority')}")
        if data.get('stakeholders'):
            parts.append(f"주요 이해관계자: {', '.join(data.get('stakeholders'))}")
        if data.get('deliverables'):
            parts.append(f"주요 산출물: {', '.join(data.get('deliverables'))}")
        if data.get('risks'):
            parts.append(f"예상 리스크: {', '.join(data.get('risks'))}")
        if data.get('detailed_requirements'):
            parts.append(f"구체적 요구사항: {data.get('detailed_requirements')}")

        return "\n".join(parts)

    def _build_markdown_prompt(self, data: Dict[str, Any]) -> str:
        header = self._build_common_header(data)
        
        markdown_example = """# 프로젝트 명세서: {프로젝트명}

## 📋 프로젝트 개요
- **프로젝트명**: {프로젝트명}
- **프로젝트 유형**: {프로젝트 유형}
- **기간**: {시작일} ~ {종료일} (총 {일수}일)
- **팀 구성**: 총 {인원}명

## 🎯 프로젝트 목적
이 프로젝트의 주요 목적과 배경을 작성하세요.

## 🔑 핵심 기능
### 1. 기능명
- 상세 설명

## 📦 주요 산출물
- 산출물 1
- 산출물 2

## 👥 이해관계자
- 이해관계자 목록

## ⚠️ 리스크 및 제약사항
- 예상 리스크
- 기술적 제약사항

## 📝 상세 요구사항
구체적인 요구사항과 기능 명세를 작성하세요."""
        
        prompt = f"""{header}

위 프로젝트 정보를 기반으로 **마크다운 형식의 상세한 프로젝트 명세서**를 작성하세요.

**중요 지침**:
1. 반드시 마크다운(Markdown) 문법으로 작성 (JSON 형식 사용 금지)
2. 제목은 #, ##, ### 등의 마크다운 헤딩 사용
3. 리스트는 -, * 또는 번호 사용
4. 프로젝트 정보를 명확하고 읽기 쉽게 구조화
5. 사용자가 나중에 수정할 수 있도록 충분히 상세하게 작성
6. 다른 설명 없이 마크다운 명세서만 출력
7. 시작일과 종료일이 주어지지 않은 경우, '현재 날짜'를 시작일로 하고 '예상 기간'을 더해 종료일을 계산하여 기입하세요.

마크다운 예시 구조:
{markdown_example}

위 구조를 참고하여 프로젝트 명세서를 마크다운 형식으로 작성하세요."""
        
        return prompt

    def _build_wbs_prompt(self, data: Dict[str, Any]) -> str:
        header = self._build_common_header(data)
        
        schema_example = '''{
  "project_name": "프로젝트명",
  "total_tasks": 10,
  "total_duration_days": 30,
  "wbs_structure": [
    {
      "task_id": "1.0",
      "parent_id": null,
      "name": "메인 작업",
      "assignee": "PM",
      "start_date": "2024-01-01",
      "end_date": "2024-01-10",
      "duration_days": 10,
      "progress": 0,
      "status": "할일",
      "subtasks": []
    }
  ]
}'''
        
        instructions = f"""
위 프로젝트 정보를 기반으로 WBS(Work Breakdown Structure)를 생성하세요.

**중요: 반드시 아래 JSON 스키마를 정확히 따라야 합니다.**

필수 규칙:
1. 모든 필드는 영문 키 이름 사용 (project_name, total_tasks, wbs_structure 등)
2. task_id는 "1.0", "1.1" 형식 (최대 2단계). 1.1.1과 같은 3단계 작업은 절대 생성하지 마세요.
3. progress는 항상 0
4. status는 항상 "할일"
5. duration_days는 정수(integer)
6. start_date, end_date는 "YYYY-MM-DD" 형식
7. subtasks는 배열 (하위 작업이 없으면 빈 배열 []). 단, 2단계 작업(1.1 등)의 subtasks는 항상 비어 있어야 합니다.
8. JSON만 출력 (설명, 코드블록 금지)

JSON 스키마:
{schema_example}

위 스키마를 따라 WBS를 생성하세요."""
        
        return "\n\n".join([header, instructions])

    def _build_wbs_from_markdown_prompt(self, markdown_spec: str) -> str:
        schema_example = '''{
  "project_name": "프로젝트명",
  "total_tasks": 10,
  "total_duration_days": 30,
  "wbs_structure": [
    {
      "task_id": "1.0",
      "parent_id": null,
      "name": "프로젝트 계획",
      "assignee": "PM",
      "start_date": "2024-01-01",
      "end_date": "2024-01-10",
      "duration_days": 10,
      "progress": 0,
      "status": "할일",
      "subtasks": [
        {
          "task_id": "1.1",
          "parent_id": "1.0",
          "name": "요구사항 분석",
          "assignee": "개발자",
          "start_date": "2024-01-01",
          "end_date": "2024-01-05",
          "duration_days": 5,
          "progress": 0,
          "status": "할일",
          "subtasks": []
        }
      ]
    }
  ]
}'''
        
        prompt = f"""다음 마크다운 명세서를 분석하여 WBS(Work Breakdown Structure)를 생성하세요.

**중요: 반드시 아래 JSON 스키마를 정확히 따라야 합니다. 키 이름을 변경하거나 한글로 번역하지 마세요.**

필수 규칙:
1. 모든 필드는 영문 키 이름 사용 (project_name, total_tasks, wbs_structure 등)
2. task_id는 "1.0", "1.1" 형식 (최대 2단계). 1.1.1과 같은 3단계 작업은 절대 생성하지 마세요.
3. progress는 항상 0
4. status는 항상 "할일"
5. duration_days는 정수(integer)
6. subtasks는 배열 (하위 작업이 없으면 빈 배열 []). 단, 2단계 작업(1.1 등)의 subtasks는 항상 비어 있어야 합니다.
7. JSON 코드블록(```)으로 감싸지 말고 순수 JSON만 출력

JSON 스키마 예시:
{schema_example}

마크다운 명세서:
{markdown_spec}

위 명세서를 분석하여 JSON 형식의 WBS를 생성하세요. 다른 설명 없이 JSON만 출력하세요."""
        
        return prompt

    async def _generate_content(self, prompt: str, json_mode: bool = False) -> str:
        """Groq API를 호출하여 텍스트를 반환합니다. 재시도 로직 포함.
        
        Args:
            prompt: AI에게 전달할 프롬프트
            json_mode: True이면 JSON만 출력하도록 강제, False이면 일반 텍스트
        """
        max_retries = 5
        backoff_seconds = 1

        for attempt in range(1, max_retries + 1):
            try:
                # 시스템 메시지 설정
                if json_mode:
                    system_message = "You are a project management expert. You must respond EXACTLY in the requested format. When asked for JSON, output ONLY valid JSON without any explanations, markdown code blocks, or additional text. Use the exact field names specified in the schema."
                else:
                    system_message = "You are a project management expert. You must respond in the requested format. When asked for markdown, output well-structured markdown content."
                
                # API 호출 파라미터 준비
                api_params = {
                    "model": self.model_name,
                    "messages": [
                        {
                            "role": "system",
                            "content": system_message
                        },
                        {
                            "role": "user",
                            "content": prompt
                        }
                    ],
                    "temperature": 0.3,
                    "max_tokens": 8000,
                }
                
                # JSON 모드일 때만 response_format 추가
                if json_mode:
                    api_params["response_format"] = {"type": "json_object"}
                
                # Groq API 호출 (chat completions 형식)
                response = await asyncio.to_thread(
                    self.client.chat.completions.create,
                    **api_params
                )
                
                # 응답 텍스트 추출
                if response.choices and len(response.choices) > 0:
                    return response.choices[0].message.content
                else:
                    raise Exception("Groq API로부터 유효한 응답을 받지 못했습니다.")
                    
            except Exception as e:
                err_str = str(e)
                if attempt == max_retries:
                    raise Exception(f"Groq API 호출 실패 after {attempt} attempts: {err_str}")
                # transient retry conditions
                if 'UNAVAILABLE' in err_str or '503' in err_str or 'overloaded' in err_str.lower() or 'rate_limit' in err_str.lower():
                    await asyncio.sleep(backoff_seconds)
                    backoff_seconds = min(backoff_seconds * 2, 30)
                    continue
                raise Exception(f"Groq API 호출 실패: {err_str}")
