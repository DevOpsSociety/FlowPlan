import json
import re
from typing import Dict, Any
from app.models.response import WBSGenerateResponse
from app.services.gemini_service import GeminiService


class WBSFromMarkdownGenerator:
    """마크다운 명세서로부터 WBS 생성 서비스"""
    
    def __init__(self):
        self.gemini_service = GeminiService()
    
    async def generate_wbs(self, markdown_spec: str) -> WBSGenerateResponse:
        """
        마크다운 명세서로부터 WBS 생성
        
        Args:
            markdown_spec: 마크다운 형식의 프로젝트 명세서
            
        Returns:
            생성된 WBS 응답
        """
        # 1. Gemini API를 통해 WBS 구조 생성
        wbs_json_str = await self.gemini_service.generate_wbs_from_markdown(markdown_spec)
        
        # 2. JSON 파싱 및 검증
        wbs_data = self._parse_and_validate_wbs(wbs_json_str)
        
        # 3. Pydantic 모델로 변환
        response = WBSGenerateResponse(**wbs_data)
        
        return response
    
    def _parse_and_validate_wbs(self, json_str: str) -> Dict[str, Any]:
        """
        Groq 응답을 파싱하고 검증
        
        Args:
            json_str: Groq가 생성한 JSON 문자열
            
        Returns:
            파싱된 딕셔너리
        """
        try:
            # 마크다운 코드 블록 제거
            json_str = re.sub(r'^```(?:json)?\s*\n', '', json_str, flags=re.MULTILINE)
            json_str = re.sub(r'\n```\s*$', '', json_str, flags=re.MULTILINE)
            json_str = json_str.strip()
            
            # JSON 파싱
            wbs_data = json.loads(json_str)
            
            # 필수 필드 확인
            required_fields = ['project_name', 'total_tasks', 'total_duration_days', 'wbs_structure']
            missing_fields = [field for field in required_fields if field not in wbs_data]
            
            if missing_fields:
                raise ValueError(
                    f"필수 필드 누락: {', '.join(missing_fields)}\n"
                    f"AI 응답 키: {list(wbs_data.keys())}\n"
                    f"AI가 한글 키를 사용했을 수 있습니다. 프롬프트를 수정해야 합니다."
                )
            
            return wbs_data
            
        except json.JSONDecodeError as e:
            raise ValueError(f"WBS JSON 파싱 실패: {str(e)}\n응답 (처음 500자): {json_str[:500]}")
        except ValueError:
            raise
        except Exception as e:
            raise ValueError(f"WBS 데이터 검증 실패: {str(e)}\n응답: {json_str[:500]}")
