from typing import List, Dict, Any
from app.models.response import WBSTask


def flatten_wbs_for_spring(wbs_structure: List[WBSTask]) -> List[Dict[str, Any]]:
    """
    계층 구조의 WBS를 flat 구조로 변환 (스프링 서버 DB 저장용)
    
    Args:
        wbs_structure: 계층 구조의 WBS 작업 리스트
        
    Returns:
        flat 구조의 작업 리스트
        
    Note:
        - task_id는 계층 구조 표시용 (UI에서 사용)
        - parent_task_id는 부모 작업의 task_id (스프링에서 매핑 필요)
        - 스프링에서 저장 순서대로 저장하면 parent_id를 올바르게 설정 가능
    """
    flat_tasks = []
    
    def flatten_recursive(tasks: List[WBSTask], parent_task_id: str = None):
        """재귀적으로 작업을 flat하게 만듦"""
        for task in tasks:
            # 현재 작업 추가
            flat_task = {
                "task_id": task.task_id,  # UI 표시용 (1.0, 1.1, 1.2...)
                "parent_task_id": parent_task_id,  # 부모의 task_id (1.1의 부모는 1.0)
                "name": task.name,
                "assignee": task.assignee,
                "start_date": task.start_date.isoformat(),
                "end_date": task.end_date.isoformat(),
                "duration_days": task.duration_days,
                "progress": task.progress,  # 항상 0
                "status": task.status.value  # 항상 "할일"
            }
            flat_tasks.append(flat_task)
            
            # 하위 작업 재귀 처리
            if task.subtasks:
                flatten_recursive(task.subtasks, task.task_id)
    
    flatten_recursive(wbs_structure)
    return flat_tasks


# 사용 예시
"""
# 계층 구조
wbs_structure = [
    {
        "task_id": "1.0",
        "name": "기획",
        "subtasks": [
            {"task_id": "1.1", "name": "요구사항 분석", "subtasks": []},
            {"task_id": "1.2", "name": "디자인", "subtasks": []}
        ]
    }
]

# Flat 구조로 변환
flat_tasks = flatten_wbs_for_spring(wbs_structure)

# 결과:
# [
#   {"task_id": "1.0", "parent_id": null, "name": "기획", ...},
#   {"task_id": "1.1", "parent_id": "1.0", "name": "요구사항 분석", ...},
#   {"task_id": "1.2", "parent_id": "1.0", "name": "디자인", ...}
# ]
"""
