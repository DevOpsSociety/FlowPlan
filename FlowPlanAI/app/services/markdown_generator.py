from app.models.request import WBSGenerateRequest
from app.services.gemini_service import GeminiService
from typing import Dict, Any


class MarkdownSpecGenerator:
    """마크다운 프로젝트 명세서 생성 서비스"""
    
    def __init__(self):
        self.gemini_service = GeminiService()
    
    async def generate_spec(self, request: WBSGenerateRequest) -> str:
        """
        프로젝트 정보를 마크다운 명세서로 변환
        
        Args:
            request: WBS 생성 요청
            
        Returns:
            마크다운 형식의 프로젝트 명세서
        """
        # 1. 프로젝트 데이터 준비
        project_data = self._prepare_project_data(request)
        
        # 2. Gemini로 마크다운 생성
        markdown_spec = await self.gemini_service.generate_markdown_spec(project_data)
        
        return markdown_spec
    
    def _prepare_project_data(self, request: WBSGenerateRequest) -> Dict[str, Any]:
        """요청 데이터를 딕셔너리로 변환"""
        
        # 기간 계산
        if request.project_duration:
            total_days = (request.project_duration.end_date - request.project_duration.start_date).days + 1
            start_date = request.project_duration.start_date.isoformat()
            end_date = request.project_duration.end_date.isoformat()
        else:
            total_days = request.expected_duration_days
            start_date = None
            end_date = None
        
        return {
            # 기본 정보
            "project_name": request.project_name,
            "project_type": request.project_type,
            "team_size": request.team_size,
            "total_days": total_days,
            
            # 날짜 정보
            "start_date": start_date,
            "end_date": end_date,
            
            # 추가 정보
            "budget": request.budget,
            "priority": request.priority.value if request.priority else None,
            "stakeholders": request.stakeholders,
            "deliverables": request.deliverables,
            "risks": request.risks,
            
            # 상세 요구사항
            "project_purpose": request.project_purpose,
            "key_features": request.key_features,
            "detailed_requirements": request.detailed_requirements,
            "constraints": request.constraints
        }
