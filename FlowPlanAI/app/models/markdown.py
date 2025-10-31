from pydantic import BaseModel, Field


class MarkdownSpecRequest(BaseModel):
    """마크다운 명세서 요청 모델 (기존 WBSGenerateRequest와 동일)"""
    pass  # WBSGenerateRequest를 그대로 사용


class MarkdownSpecResponse(BaseModel):
    """마크다운 프로젝트 명세서 응답 모델"""
    
    project_name: str = Field(..., description="프로젝트명")
    markdown_spec: str = Field(..., description="마크다운 형식의 프로젝트 명세서")
    
    class Config:
        json_schema_extra = {
            "example": {
                "project_name": "FlowPlan 모바일 앱 개발",
                "markdown_spec": "# 프로젝트 명세서: FlowPlan 모바일 앱 개발\n\n## 📋 프로젝트 개요\n..."
            }
        }


class WBSFromSpecRequest(BaseModel):
    """마크다운 명세서 기반 WBS 생성 요청"""
    
    markdown_spec: str = Field(
        ..., 
        description="프로젝트 명세서 (마크다운 형식)",
        example="""# 프로젝트 명세서: FlowPlan 앱 개발

## 📋 프로젝트 개요
- **프로젝트명**: FlowPlan 앱
- **기간**: 2024-01-01 ~ 2024-01-14 (14일)
- **팀 구성**: 5명

## 🎯 프로젝트 목적
일정 관리 도구 개발

## ✨ 핵심 기능
1. 간트차트
2. WBS 자동 생성
3. 칸반보드
"""
    )
