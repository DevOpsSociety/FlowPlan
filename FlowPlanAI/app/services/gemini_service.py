from google import genai
from google.genai import types
from app.core.config import settings
from typing import Dict, Any
import asyncio


class GeminiService:
    """Google Gemini API 서비스"""

    def __init__(self):
        """Gemini API 초기화"""
        self.client = genai.Client(api_key=settings.GEMINI_API_KEY)
        self.model_name = settings.GEMINI_MODEL

    async def generate_markdown_spec(self, project_data: Dict[str, Any]) -> str:
        """
        프로젝트 정보를 마크다운 명세서로 변환
        
        Args:
            project_data: 프로젝트 정보 딕셔너리
            
        Returns:
            마크다운 형식의 프로젝트 명세서
        """
        prompt = self._build_markdown_prompt(project_data)
        response = await self._generate_content(prompt)
        return response

    async def generate_wbs_from_markdown(self, markdown_spec: str) -> str:
        """
        마크다운 명세서를 기반으로 WBS 생성
        
        Args:
            markdown_spec: 마크다운 형식의 프로젝트 명세서
            
        Returns:
            JSON 형식의 WBS 구조 문자열
        """
        prompt = self._build_wbs_from_markdown_prompt(markdown_spec)
        response = await self._generate_content(prompt)
        return response

    async def generate_wbs_structure(self, project_data: Dict[str, Any]) -> str:
        """
        프로젝트 정보를 기반으로 WBS 구조를 생성합니다.
        
        Args:
            project_data: 프로젝트 정보 딕셔너리
            
        Returns:
            JSON 형식의 WBS 구조 문자열
        """
        prompt = self._build_wbs_prompt(project_data)

        response = await self._generate_content(prompt)
        return response

    def _build_wbs_prompt(self, data: Dict[str, Any]) -> str:
        """WBS 생성을 위한 프롬프트 구성"""
        
        # ... (이 메서드의 다른 부분은 동일) ...
        
        date_info = ""
        if data.get('start_date') and data.get('end_date'):
            date_info = f"- 시작일: {data['start_date']}\n- 마감일: {data['end_date']}\n"
        date_info += f"- 전체 기간: {data['total_days']}일"
        
        additional_info = []
        if data.get('budget'):
            additional_info.append(f"- 예산: {data['budget']}")
        if data.get('priority'):
            additional_info.append(f"- 우선순위: {data['priority']}")
        if data.get('stakeholders'):
            additional_info.append(f"- 주요 이해관계자: {', '.join(data['stakeholders'])}")
        if data.get('deliverables'):
            additional_info.append(f"- 주요 산출물: {', '.join(data['deliverables'])}")
        if data.get('risks'):
            additional_info.append(f"- 예상 리스크: {', '.join(data['risks'])}")
        
        additional_section = "\n".join(additional_info) if additional_info else ""
        
        requirements_info = []
        if data.get('project_purpose'):
            requirements_info.append(f"- 프로젝트 목적: {data['project_purpose']}")
        if data.get('key_features'):
            requirements_info.append(f"- 주요 기능: {', '.join(data['key_features'])}")
        if data.get('detailed_requirements'):
            requirements_info.append(f"- 구체적 요구사항: {data['detailed_requirements']}")
        if data.get('constraints'):
            requirements_info.append(f"- 제약사항: {data['constraints']}")
        
        requirements_section = "\n".join(requirements_info) if requirements_info else ""
        
        additional_block = ""
        if additional_section:
            additional_block = f"\n## 💰 추가 정보\n{additional_section}\n"
        
        requirements_block = ""
        if requirements_section:
            requirements_block = f"\n## 🎯 요구사항\n{requirements_section}\n"
        
        prompt = f"""
당신은 프로젝트 관리 전문가입니다. 다음 프로젝트 정보를 기반으로 상세하고 현실적인 WBS(Work Breakdown Structure)를 생성해주세요.

## 📋 프로젝트 기본 정보
- 프로젝트명: {data['project_name']}
- 프로젝트 주제: {data['project_type']}
- 팀 규모: {data['team_size']}명
{date_info}{additional_block}{requirements_block}

## 📝 WBS 생성 지침
1. 프로젝트를 3-5개의 주요 단계(Phase)로 분해
2. 각 단계를 2-4개의 세부 작업(Task)으로 분해
3. 각 작업에 적절한 담당자 역할 배정 (PM, 기획자, 개발자, 디자이너, QA 등)
4. 작업 기간은 전체 프로젝트 기간 내에서 현실적으로 배분
5. 작업 간 의존성을 고려하여 순차적으로 배치
6. 예상 리스크를 고려한 여유 기간 포함
7. 주요 산출물 완성 시점을 마일스톤으로 표시

## 출력 형식
반드시 다음 JSON 형식으로만 응답해주세요. 다른 설명은 포함하지 마세요.

**중요**: 
- progress는 항상 0
- status는 항상 "할일"
- parent_id는 상위 작업의 task_id (최상위는 null)
- dependencies 필드는 사용하지 않음
# [수정됨] duration_days는 반드시 정수여야 한다는 규칙 추가
- 'duration_days'(일수) 필드는 **반드시 1, 2, 3과 같은 정수(integer)여야 하며,** 절대로 0.5, 0.2와 같은 소수점(float)을 사용하면 안 됩니다.

{{
  "project_name": "프로젝트명",
  "total_tasks": 총_작업_수,
  "total_duration_days": 전체_기간,
  "wbs_structure": [
    {{
      "task_id": "1.0",
      "parent_id": null,
      "name": "주요 단계명",
      "assignee": "담당자",
      "start_date": "YYYY-MM-DD",
      "end_date": "YYYY-MM-DD",
      "duration_days": 일수,
      "progress": 0,
      "status": "할일",
      "subtasks": [
        {{
          "task_id": "1.1",
          "parent_id": "1.0",
          "name": "세부 작업명",
          "assignee": "담당자",
          "start_date": "YYYY-MM-DD",
          "end_date": "YYYY-MM-DD",
          "duration_days": 일수,
          "progress": 0,
          "status": "할일",
          "subtasks": []
        }}
      ]
    }}
  ]
}}

JSON 형식만 출력하고, 마크다운 코드 블록(```)이나 다른 설명은 포함하지 마세요.
"""
        return prompt

    def _build_markdown_prompt(self, data: Dict[str, Any]) -> str:
        """마크다운 명세서 생성 프롬프트"""
        
        # ... (이 메서드는 WBS JSON을 만들지 않으므로 수정할 필요 없음) ...
        
        date_info = ""
        if data.get('start_date') and data.get('end_date'):
            date_info = f"- **기간**: {data['start_date']} ~ {data['end_date']} ({data['total_days']}일)\n"
        else:
            date_info = f"- **예상 기간**: {data['total_days']}일\n"
        
        prompt = f"""
당신은 프로젝트 관리 전문가입니다. 다음 프로젝트 정보를 상세하고 체계적인 마크다운 명세서로 작성해주세요.
사용자가 이 명세서를 수정하여 더 정확한 WBS를 생성할 수 있도록 편집하기 쉬운 형식으로 만들어주세요.

## 입력 정보:
- 프로젝트명: {data['project_name']}
- 프로젝트 주제: {data['project_type']}
- 팀 규모: {data['team_size']}명
- 기간: {data['total_days']}일
{f"- 예산: {data['budget']}" if data.get('budget') else ""}
{f"- 우선순위: {data['priority']}" if data.get('priority') else ""}
{f"- 이해관계자: {', '.join(data['stakeholders'])}" if data.get('stakeholders') else ""}
{f"- 산출물: {', '.join(data['deliverables'])}" if data.get('deliverables') else ""}
{f"- 리스크: {', '.join(data['risks'])}" if data.get('risks') else ""}
{f"- 프로젝트 목적: {data['project_purpose']}" if data.get('project_purpose') else ""}
{f"- 주요 기능: {', '.join(data['key_features'])}" if data.get('key_features') else ""}
{f"- 구체적 요구사항: {data['detailed_requirements']}" if data.get('detailed_requirements') else ""}
{f"- 제약사항: {data['constraints']}" if data.get('constraints') else ""}

## 출력 형식:
다음 구조로 마크다운 명세서를 작성하세요. 사용자가 각 섹션을 쉽게 수정할 수 있도록 명확하게 구분하세요.

```markdown
# 프로젝트 명세서: [프로젝트명]

## 📋 프로젝트 개요
- **프로젝트명**: 
- **프로젝트 주제**: 
- **팀 구성**: 
- **기간**: 
- **예산**: 
- **우선순위**: 

## 🎯 프로젝트 목적
[프로젝트의 목적과 배경을 상세히 설명]

## 💼 주요 이해관계자
- [이름/역할] - [책임사항]

## 📦 주요 산출물
1. [산출물 1] - [설명]
2. [산출물 2] - [설명]

## ⚠️ 예상 리스크 및 완화 방안
- **리스크**: [리스크 설명]
  - **완화 방안**: [대응 방안]

## ✨ 핵심 기능 및 요구사항
### [기능 1]
- [상세 설명]
- [기술 요구사항]

## 🔧 기술 요구사항
- [요구사항 1]
- [요구사항 2]

## 📐 제약사항 및 가이드라인
- [제약사항 1]
- [제약사항 2]

## 📅 주요 마일스톤 (선택)
- [날짜]: [마일스톤 설명]
```

마크다운 형식만 출력하고, 코드 블록 마커나 다른 설명은 포함하지 마세요.
"""
        return prompt

    def _build_wbs_from_markdown_prompt(self, markdown_spec: str) -> str:
        """마크다운 명세서로부터 WBS 생성 프롬프트"""
        
        prompt = f"""
당신은 프로젝트 관리 전문가입니다. 다음 마크다운 형식의 프로젝트 명세서를 분석하여 상세한 WBS(Work Breakdown Structure)를 생성해주세요.

명세서의 모든 내용을 꼼꼼히 읽고, 언급된 기능, 요구사항, 제약사항, 리스크를 모두 반영하여 현실적이고 실행 가능한 작업 분해 구조를 만들어주세요.

## 프로젝트 명세서:

{markdown_spec}

## WBS 생성 지침:
1. 명세서의 **핵심 기능**을 기준으로 주요 단계를 구성
2. **기술 요구사항**을 고려하여 세부 작업 생성
3. **제약사항**에 맞춰 작업 기간 배분
4. **리스크 완화 방안**을 작업에 반영
5. **마일스톤**이 있다면 중요 작업에 표시
6. **팀 구성**을 고려하여 담당자 배정

## 출력 형식:
반드시 다음 JSON 형식으로만 응답해주세요.

**중요 규칙**:
- progress는 항상 0 (초기 생성 시)
- status는 항상 "할일" (초기 생성 시)
- parent_id는 상위 작업의 task_id (최상위 작업은 null)
- dependencies 필드는 사용하지 않음
# [수정됨] duration_days는 반드시 정수여야 한다는 규칙 추가
- 'duration_days'(일수) 필드는 **반드시 1, 2, 3과 같은 정수(integer)여야 하며,** 절대로 0.5, 0.2와 같은 소수점(float)을 사용하면 안 됩니다.

{{
  "project_name": "프로젝트명",
  "total_tasks": 총_작업_수,
  "total_duration_days": 전체_기간,
  "wbs_structure": [
    {{
      "task_id": "1.0",
      "parent_id": null,
      "name": "주요 단계명",
      "assignee": "담당자",
      "start_date": "YYYY-MM-DD",
      "end_date": "YYYY-MM-DD",
      "duration_days": 일수,
      "progress": 0,
      "status": "할일",
      "subtasks": [
        {{
          "task_id": "1.1",
          "parent_id": "1.0",
          "name": "세부 작업명",
          "assignee": "담당자",
          "start_date": "YYYY-MM-DD",
          "end_date": "YYYY-MM-DD",
          "duration_days": 일수,
          "progress": 0,
          "status": "할일",
          "subtasks": []
        }}
      ]
    }}
  ]
}}

JSON 형식만 출력하고, 마크다운 코드 블록(```)이나 다른 설명은 포함하지 마세요.
"""
        return prompt

    async def _generate_content(self, prompt: str) -> str:
        """
        Gemini API를 호출하여 컨텐츠 생성
        
        Args:
            prompt: 생성 프롬프트
            
        Returns:
            생성된 텍스트
        """
        max_retries = 5
        backoff_seconds = 1

        for attempt in range(1, max_retries + 1):
            try:
                # Gemini client is blocking; run in thread to avoid blocking the event loop
                response = await asyncio.to_thread(
                    self.client.models.generate_content,
                    model=self.model_name,
                    contents=prompt,
                )
                return response.text

            except Exception as e:
                err_str = str(e)
                # If last attempt, raise
                if attempt == max_retries:
                    raise Exception(f"Gemini API 호출 실패 after {attempt} attempts: {err_str}")

                # For transient errors (503 / UNAVAILABLE) perform exponential backoff and retry
                # If error message contains 'UNAVAILABLE' or '503', treat as retryable
                if 'UNAVAILABLE' in err_str or '503' in err_str or 'overloaded' in err_str.lower():
                    wait = min(backoff_seconds, 30)
                    await asyncio.sleep(wait)
                    backoff_seconds *= 2
                    continue

                # Non-retryable error: raise immediately
                raise Exception(f"Gemini API 호출 실패: {err_str}")
