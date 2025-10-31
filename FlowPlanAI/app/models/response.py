from datetime import date
from typing import List, Optional
from pydantic import BaseModel, Field
from enum import Enum


class TaskStatus(str, Enum):
    """작업 상태 (ERD 기준)"""
    TODO = "할일"
    IN_PROGRESS = "진행중"
    COMPLETED = "완료"


class WBSTask(BaseModel):
    """WBS 작업 항목 (스프링 서버 ERD 호환)"""
    
    task_id: str = Field(..., description="작업 ID (계층 구조)", example="1.1")
    parent_id: Optional[str] = Field(None, description="상위 작업 ID (1.0의 하위는 parent_id='1.0')", example="1.0")
    name: str = Field(..., description="작업명", example="요구사항 분석")
    assignee: str = Field(..., description="담당자 (assignee_id 매핑 필요)", example="PM")
    start_date: date = Field(..., description="시작일")
    end_date: date = Field(..., description="종료일")
    duration_days: int = Field(..., description="기간(일)", example=7)
    progress: int = Field(0, ge=0, le=100, description="진행률(%) - 기본값 0")
    status: TaskStatus = Field(TaskStatus.TODO, description="상태 - 기본값 '할일'")
    subtasks: Optional[List["WBSTask"]] = Field(default=[], description="하위 작업 (flat 구조 변환 가능)")


class WBSGenerateResponse(BaseModel):
    """WBS 생성 응답 모델"""
    
    project_name: str = Field(..., description="프로젝트명")
    total_tasks: int = Field(..., description="전체 작업 수")
    total_duration_days: int = Field(..., description="전체 기간(일)")
    wbs_structure: List[WBSTask] = Field(..., description="WBS 구조")
    
    class Config:
        json_schema_extra = {
            "example": {
                "project_name": "프로젝트 기획 시작하기",
                "total_tasks": 12,
                "total_duration_days": 21,
                "wbs_structure": [
                    {
                        "task_id": "1.0",
                        "task_name": "프로젝트 기획",
                        "assignee": "PM",
                        "start_date": "2024-01-01",
                        "end_date": "2024-01-21",
                        "duration_days": 21,
                        "progress": 85,
                        "status": "진행 중",
                        "dependencies": [],
                        "subtasks": []
                    }
                ]
            }
        }


# Pydantic v2에서 순환 참조 해결
WBSTask.model_rebuild()
