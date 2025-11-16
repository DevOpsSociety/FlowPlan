from typing import Dict, Any
from fastapi import APIRouter, HTTPException, status, Body
from app.models.request import WBSGenerateRequest
from app.models.response import WBSGenerateResponse
from app.models.markdown import MarkdownSpecResponse, WBSFromSpecRequest
from app.services.markdown_generator import MarkdownSpecGenerator
from app.services.wbs_from_markdown import WBSFromMarkdownGenerator
from app.utils.wbs_converter import flatten_wbs_for_spring

router = APIRouter(prefix="/wbs", tags=["WBS"])
@router.post(
    "/generate-spec",
    response_model=MarkdownSpecResponse,
    status_code=status.HTTP_200_OK,
    summary="프로젝트 명세서 생성 (마크다운)",
    description="""
    프로젝트 정보를 마크다운 형식의 상세 명세서로 변환합니다.
    
    **워크플로우**:
    1. 이 API로 마크다운 명세서 생성
    2. 사용자가 명세서를 검토하고 수정
    3. `/generate-from-spec` API로 WBS 생성
    
    이 방식을 사용하면 더 정확하고 상세한 WBS를 얻을 수 있습니다.
    """
)
async def generate_markdown_spec(
    request: WBSGenerateRequest = Body(
        ...,
        examples={
            "simple": {
                "summary": "간단한 예시",
                "value": {
                    "project_name": "신규 앱 개발",
                    "project_type": "모바일 앱",
                    "team_size": 5,
                    "expected_duration_days": 30
                }
            }
            ,
            "full": {
                "summary": "전체 입력 예시 (12개 필드)",
                "value": {
                    "project_name": "FlowPlan 플랫폼 개발",
                    "project_type": "웹 서비스",
                    "team_size": 7,
                    "expected_duration_days": 90,
                    "start_date": "2024-01-01",
                    "end_date": "2024-03-31",
                    "budget": "100000000",
                    "priority": "높음",
                    "stakeholders": ["CEO", "CTO"],
                    "deliverables": ["API 문서", "웹앱"],
                    "risks": ["일정 지연", "기술 부채"],
                    "detailed_requirements": "다크모드, 반응형 디자인, OAuth2 로그인"
                }
            }
        }
    )
) -> MarkdownSpecResponse:
    """프로젝트 명세서 생성 (1단계)"""
    try:
        spec_generator = MarkdownSpecGenerator()
        markdown_spec = await spec_generator.generate_spec(request)
        
        return MarkdownSpecResponse(
            project_name=request.project_name,
            markdown_spec=markdown_spec
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"명세서 생성 중 오류 발생: {str(e)}"
        )


@router.post(
    "/generate-from-spec",
    response_model=WBSGenerateResponse,
    status_code=status.HTTP_200_OK,
    summary="마크다운 명세서로부터 WBS 생성",
    description="""
    마크다운 형식의 프로젝트 명세서를 분석하여 WBS를 생성합니다.
    
    **권장 워크플로우**:
    1. `/generate-spec`으로 초기 명세서 생성
    2. 사용자가 명세서를 상세히 수정
    3. 이 API로 정확한 WBS 생성
    
    명세서에 작성된 모든 요구사항, 기능, 제약사항이 WBS에 반영됩니다.
    """
)
async def generate_wbs_from_spec(
    request: WBSFromSpecRequest = Body(
        ...,
        example={
            "markdown_spec": """# 프로젝트 명세서: FlowPlan 앱 개발

## 프로젝트 개요
- **프로젝트명**: FlowPlan 앱
- **기간**: 2024-01-01 ~ 2024-01-30 (30일)
- **팀 구성**: 5명 (PM 1, 개발자 3, 디자이너 1)

## 프로젝트 목적
일정 관리 및 협업을 위한 모바일 앱 개발

## 핵심 기능
### 1. 간트차트
- 드래그앤드롭으로 일정 조정
- 마일스톤 표시

### 2. WBS 자동 생성
- AI 기반 작업 분해

### 3. 칸반보드
- 작업 상태 관리
"""
        }
    )
) -> WBSGenerateResponse:
    """마크다운 명세서로부터 WBS 생성 (2단계)"""
    try:
        wbs_generator = WBSFromMarkdownGenerator()
        result = await wbs_generator.generate_wbs(request.markdown_spec)
        return result
        
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"WBS 생성 중 데이터 검증 오류: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"WBS 생성 중 오류 발생: {str(e)}"
        )


@router.post(
    "/generate-from-spec/flat",
    status_code=status.HTTP_200_OK,
    summary="마크다운으로부터 WBS 생성 (Flat 구조 - 스프링 DB용)",
    description="""
    마크다운 명세서로부터 WBS를 생성하고, **스프링 서버 DB 저장용 Flat 구조**로 변환합니다.
    
    **스프링에서 저장하는 방법**:
    ```java
    // 순서대로 저장하며 task_id -> DB id 매핑
    Map<String, Long> taskIdMap = new HashMap<>();
    
    for (WBSTaskDto dto : tasks) {
        Task task = new Task();
        task.setName(dto.getName());
        
        // 부모 작업이 있으면 매핑된 DB id 설정
        if (dto.getParentTaskId() != null) {
            Long parentDbId = taskIdMap.get(dto.getParentTaskId());
            task.setParentId(parentDbId);
        }
        
        Task saved = taskRepository.save(task);
        taskIdMap.put(dto.getTaskId(), saved.getId());
    }
    ```
    
    **ERD Tasks 테이블 매핑**:
    - task_id → UI 표시용 (1.0, 1.1, 1.2...)
    - parent_task_id → 부모의 task_id (스프링이 DB id로 변환)
    - name → Tasks.name
    - assignee → Tasks.assignee_id (사용자 매핑 필요)
    - start_date/end_date → Tasks.start_date/end_date
    - progress → Tasks.progress (항상 0)
    - status → Tasks.status (항상 "할일")
    """
)
# `/generate-from-spec/flat` removed — flat conversion should be done on the Spring server side.


@router.post(
    "/generate-from-spec/flat",
    status_code=status.HTTP_200_OK,
    summary="마크다운으로부터 WBS 생성 (Flat 구조 - 스프링 DB용)",
    description="""
    마크다운 명세서로부터 WBS를 생성하고, **스프링 서버 DB 저장용 Flat 구조**로 변환합니다.
    """
)
async def generate_wbs_from_spec_flat(request: WBSFromSpecRequest) -> Dict[str, Any]:
    """마크다운 명세서로부터 WBS 생성 (Flat 구조)

    반환 구조:
    {
      "project_name": str,
      "total_tasks": int,
      "total_duration_days": int,
      "tasks": [ { task fields... } ]  # Flat 구조
    }
    """
    try:
        # 1. WBS 계층 구조 생성
        wbs_generator = WBSFromMarkdownGenerator()
        result = await wbs_generator.generate_wbs(request.markdown_spec)

        # 2. Flat 구조로 변환 (스프링에서 저장 순서대로 parent_task_id를 DB id로 매핑)
        flat_tasks = flatten_wbs_for_spring(result.wbs_structure)

        return {
            "project_name": result.project_name,
            "total_tasks": result.total_tasks,
            "total_duration_days": result.total_duration_days,
            "tasks": flat_tasks
        }

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"WBS 생성 중 데이터 검증 오류: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"WBS 생성 중 오류 발생: {str(e)}"
        )


@router.get(
    "/health",
    summary="WBS 서비스 헬스체크",
    description="WBS 생성 서비스의 상태를 확인합니다."
)
async def health_check():
    """WBS 서비스 상태 확인"""
    # Health endpoint was intentionally removed from the public API surface in favor of
    # keeping only the three WBS-related endpoints. If you need a health check, use
    # the application's root `/health` implemented in `app/main.py`.
    raise HTTPException(status_code=status.HTTP_410_GONE, detail="헬스체크 엔드포인트는 더 이상 이 라우터에서 제공되지 않습니다.")
