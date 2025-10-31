import json
import re
from datetime import datetime, timedelta
from typing import Dict, Any
from app.models.request import WBSGenerateRequest
from app.models.response import WBSGenerateResponse, WBSTask
from app.services.gemini_service import GeminiService


class WBSGenerator:
    """WBS 생성 서비스"""
    
    def __init__(self):
        self.gemini_service = GeminiService()
    
    async def generate_wbs(self, request: WBSGenerateRequest) -> WBSGenerateResponse:
        """
        WBS 생성 메인 로직
        
        Args:
            request: WBS 생성 요청
            
        Returns:
            생성된 WBS 응답
        """
        # 1. 요청 데이터를 Gemini용 형식으로 변환
        project_data = self._prepare_project_data(request)
        
        # 2. Gemini API를 통해 WBS 구조 생성
        wbs_json_str = await self.gemini_service.generate_wbs_structure(project_data)
        
        # 3. JSON 파싱 및 검증
        wbs_data = self._parse_and_validate_wbs(wbs_json_str)
        
        # 4. Pydantic 모델로 변환
        response = WBSGenerateResponse(**wbs_data)
        
        return response
    
    def _prepare_project_data(self, request: WBSGenerateRequest) -> Dict[str, Any]:
        """요청 데이터를 Gemini API용 형식으로 변환"""
        
        # 기간 계산: 구체적인 날짜가 있으면 사용, 없으면 예상 기간 사용
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
    
    def _parse_and_validate_wbs(self, json_str: str) -> Dict[str, Any]:
        """
        Gemini 응답을 파싱하고 검증
        
        Args:
            json_str: Gemini가 생성한 JSON 문자열
            
        Returns:
            파싱된 딕셔너리
        """
        try:
            # 마크다운 코드 블록 제거 (```json ... ``` 또는 ``` ... ```)
            json_str = re.sub(r'^```(?:json)?\s*\n', '', json_str, flags=re.MULTILINE)
            json_str = re.sub(r'\n```\s*$', '', json_str, flags=re.MULTILINE)
            json_str = json_str.strip()
            
            # JSON 파싱
            wbs_data = json.loads(json_str)
            
            return wbs_data
            
        except json.JSONDecodeError as e:
            raise ValueError(f"WBS JSON 파싱 실패: {str(e)}\n응답: {json_str[:500]}")
        except Exception as e:
            raise ValueError(f"WBS 데이터 검증 실패: {str(e)}")
