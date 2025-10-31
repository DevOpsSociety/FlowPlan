from datetime import date
from typing import List, Optional
from pydantic import BaseModel, Field, ConfigDict
from enum import Enum


class Priority(str, Enum):
    """우선순위"""
    HIGH = "높음"
    MEDIUM = "중간"
    LOW = "낮음"


class ProjectDuration(BaseModel):
    """프로젝트 기간"""
    start_date: date = Field(..., description="시작일")
    end_date: date = Field(..., description="종료일")


class WBSGenerateRequest(BaseModel):
    """WBS 생성 요청 모델
    
    필수 필드 4개: project_name, project_type, team_size, expected_duration_days
    나머지는 모두 선택사항입니다.
    """
    
    model_config = ConfigDict(
        json_schema_extra={
            "title": "WBS 생성 요청",
            "description": "필수: project_name, project_type, team_size, expected_duration_days"
        }
    )
    
    # ========================================
    # 필수 입력 (Required)
    # ========================================
    project_name: str = Field(
        ..., 
        description="[필수] 프로젝트명", 
        example="FlowPlan 앱 개발"
    )
    project_type: str = Field(
        ..., 
        description="[필수] 프로젝트 주제 (어떤 종류의 프로젝트인가요?)", 
        example="모바일 앱"
    )
    team_size: int = Field(
        ..., 
        gt=0, 
        description="[필수] 참여 인원 수", 
        example=5
    )
    expected_duration_days: int = Field(
        ..., 
        gt=0, 
        description="[필수] 예상 기간(일)", 
        example=60
    )
    
    # ========================================
    # 선택 입력 (Optional)
    # ========================================
    project_duration: Optional[ProjectDuration] = Field(
        None, 
        description="[선택] 시작일/마감일 (구체적인 날짜)"
    )
    budget: Optional[str] = Field(
        None, 
        description="[선택] 예산", 
        example="5000만원"
    )
    priority: Optional[Priority] = Field(
        Priority.MEDIUM, 
        description="[선택] 우선순위 (높음/중간/낮음)"
    )
    stakeholders: Optional[List[str]] = Field(
        default=[], 
        description="[선택] 주요 이해관계자", 
        example=["CEO", "마케팅팀"]
    )
    deliverables: Optional[List[str]] = Field(
        default=[], 
        description="[선택] 주요 산출물", 
        example=["앱 출시", "API 문서"]
    )
    risks: Optional[List[str]] = Field(
        default=[], 
        description="[선택] 예상 리스크", 
        example=["일정 지연"]
    )
    project_purpose: Optional[str] = Field(
        None, 
        description="[선택] 프로젝트 목적", 
        example="일정 관리 도구 개발"
    )
    key_features: Optional[List[str]] = Field(
        default=[], 
        description="[선택] 주요 기능", 
        example=["간트차트", "WBS"]
    )
    detailed_requirements: Optional[str] = Field(
        None, 
        description="[선택] 더 구체적인 요구사항",
        example="다크모드 지원, 오프라인 동기화"
    )
    constraints: Optional[str] = Field(
        None, 
        description="[선택] 제약 사항",
        example="애자일 방법론, 2주 스프린트"
    )
    

