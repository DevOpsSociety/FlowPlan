# FlowPlanAI

> Google Gemini를 활용한 AI 기반 WBS(Work Breakdown Structure) 자동 생성 시스템

[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109.0-green.svg)](https://fastapi.tiangolo.com/)
[![Google Gemini](https://img.shields.io/badge/Google-Gemini%20AI-orange.svg)](https://ai.google.dev/)

## 개요

FlowPlanAI는 프로젝트 정보를 입력받아 **Google Gemini AI**를 활용하여 작업 분해 구조(WBS)를 자동으로 생성하는 FastAPI 기반 백엔드 서비스입니다.

### 주요 특징

- **AI 기반 자동 생성**: 프로젝트 정보만으로 체계적인 WBS 생성
- **2단계 워크플로우**: 마크다운 명세서 → 사용자 편집 → 정확한 WBS
- **스프링 서버 호환**: Tasks 테이블 ERD에 맞춘 Flat 구조 제공
- **유연한 입력**: 필수 4개 필드만으로 시작 가능, 선택적 상세 정보 추가
- **계층 구조 지원**: 주 작업 → 하위 작업 자동 분해

## 기술 스택

| 분류 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **Backend** | Python | 3.11+ | 메인 언어 |
| | FastAPI | 0.109.0 | 웹 프레임워크 |
| | Uvicorn | 0.27.0 | ASGI 서버 |
| **AI** | Google Gemini | gemini-2.0-flash-exp | WBS 생성 AI 모델 |
| | google-genai | 0.3.0 | Gemini API SDK |
| **데이터 검증** | Pydantic | 2.5.3 | 요청/응답 검증 |
| | pydantic-settings | 2.1.0 | 환경 변수 관리 |
| **환경 설정** | python-dotenv | 1.0.0 | .env 파일 로드 |

## 주요 기능

### 1. 직접 WBS 생성
프로젝트 정보를 기반으로 한 번에 WBS 생성

### 2. 마크다운 명세서 생성
프로젝트 정보를 AI가 상세한 마크다운 명세서로 변환

### 3. 명세서 기반 WBS 생성
사용자가 편집한 마크다운 명세서로부터 정확한 WBS 생성

### 4. Flat 구조 변환 (스프링 DB용)
계층 구조 WBS를 `parent_task_id`로 연결된 1차원 배열로 변환

## 프로젝트 구조

```
FlowPlanAI/
├── .github/
│   └── copilot-instructions.md    # AI 코딩 가이드
├── app/
│   ├── main.py                    # FastAPI 앱 진입점 (CORS, 라우터 등록)
│   ├── api/
│   │   └── routes/
│   │       └── wbs.py             # WBS API 엔드포인트 (5개)
│   ├── services/                  # 비즈니스 로직
│   │   ├── gemini_service.py      # Gemini API 통합 (프롬프트 엔지니어링)
│   │   ├── wbs_generator.py       # 직접 WBS 생성
│   │   ├── markdown_generator.py  # 마크다운 명세서 생성
│   │   └── wbs_from_markdown.py   # 명세서 기반 WBS 생성
│   ├── models/                    # Pydantic 데이터 모델
│   │   ├── request.py             # WBSGenerateRequest (17개 필드)
│   │   ├── response.py            # WBSTask, WBSGenerateResponse
│   │   └── markdown.py            # 마크다운 관련 모델
│   ├── utils/                     # 유틸리티 함수
│   │   └── wbs_converter.py       # 계층 → Flat 구조 변환
│   └── core/
│       └── config.py              # 환경 변수 관리 (GEMINI_API_KEY)
├── .env                           # 환경 변수 (API 키)
├── .env.example                   # 환경 변수 템플릿
├── requirements.txt               # Python 의존성
└── README.md
```

### 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────┐
│  HTTP Request (JSON)                    │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│  API Layer (app/api/routes/)            │
│  - 요청 검증 (Pydantic)                  │
│  - 라우팅 및 에러 처리                   │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│  Service Layer (app/services/)          │
│  - 비즈니스 로직                         │
│  - AI 호출 및 데이터 변환                │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│  External API (Google Gemini)           │
│  - WBS/명세서 생성 (AI)                  │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│  Utility Layer (app/utils/)             │
│  - Flat 구조 변환                        │
│  - 공통 헬퍼 함수                        │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│  HTTP Response (JSON)                   │
└─────────────────────────────────────────┘
```

## 시작하기

### 1. 환경 설정

#### Windows (PowerShell)
```powershell
# 1. 가상환경 생성
python -m venv venv

# 2. 가상환경 활성화
.\venv\Scripts\activate

# 3. 의존성 설치
pip install -r requirements.txt

# 4. 환경 변수 설정
cp .env.example .env
# .env 파일 편집 후 GEMINI_API_KEY 추가
```

#### Linux/macOS
```bash
# 1. 가상환경 생성
python3 -m venv venv

# 2. 가상환경 활성화
source venv/bin/activate

# 3. 의존성 설치
pip install -r requirements.txt

# 4. 환경 변수 설정
cp .env.example .env
# .env 파일 편집 후 GEMINI_API_KEY 추가
```

### 2. API 키 발급

1. [Google AI Studio](https://aistudio.google.com/app/apikey) 접속
2. **Get API Key** 클릭
3. API 키 복사
4. `.env` 파일에 추가:
   ```env
   GEMINI_API_KEY=your_actual_api_key_here
   ```

### 3. 서버 실행

```bash
# 개발 모드 (자동 리로드)
uvicorn app.main:app --reload

# 프로덕션 모드
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### 4. API 문서 확인

서버 실행 후 브라우저에서 확인:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## API 엔드포인트

### 1. 직접 WBS 생성
```http
POST /api/v1/wbs/generate
```
프로젝트 정보를 기반으로 한 번에 WBS 생성

### 2. 마크다운 명세서 생성
```http
POST /api/v1/wbs/generate-spec
```
프로젝트 정보를 AI가 상세한 마크다운 명세서로 변환

### 3. 명세서 기반 WBS 생성 (계층 구조)
```http
POST /api/v1/wbs/generate-from-spec
```
사용자가 편집한 마크다운 명세서로부터 WBS 생성 (subtasks 포함)

### 4. 명세서 기반 WBS 생성 (Flat 구조)
```http
POST /api/v1/wbs/generate-from-spec/flat
```
스프링 서버 DB 저장용 Flat 구조로 변환 (parent_task_id로 계층 표현)

### 5. 헬스체크
```http
GET /api/v1/wbs/health
```

## API 사용 예시

### 예시 1: 최소 입력으로 WBS 생성

**요청** (필수 4개 필드만):
```bash
curl -X POST "http://localhost:8000/api/v1/wbs/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "project_name": "FlowPlan 앱 개발",
    "project_type": "모바일 앱",
    "team_size": 5,
    "expected_duration_days": 60
  }'
```

**응답**:
```json
{
  "project_name": "FlowPlan 앱 개발",
  "wbs_structure": [
    {
      "task_id": "1.0",
      "parent_id": null,
      "name": "기획",
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
          "assignee": "BA",
          "start_date": "2024-01-01",
          "end_date": "2024-01-05",
          "duration_days": 5,
          "progress": 0,
          "status": "할일",
          "subtasks": []
        }
      ]
    }
  ],
  "total_tasks": 15,
  "total_duration_days": 60
}
```

### 예시 2: 상세 입력으로 정확한 WBS 생성

**요청** (모든 필드 포함):
```json
{
  "project_name": "FlowPlan 모바일 앱 개발",
  "project_type": "모바일 앱 (iOS/Android)",
  "team_size": 7,
  "expected_duration_days": 90,
  "project_duration": {
    "start_date": "2024-01-01",
    "end_date": "2024-03-31"
  },
  "budget": "1억원",
  "priority": "높음",
  "stakeholders": ["CEO", "CTO", "마케팅 이사"],
  "deliverables": ["iOS 앱", "Android 앱", "API 문서"],
  "risks": ["일정 지연 가능성", "디자인 리소스 부족"],
  "project_purpose": "프로젝트 일정 관리 플랫폼 개발",
  "key_features": ["간트차트", "WBS 자동 생성", "칸반보드"],
  "detailed_requirements": "반응형 디자인, 다크모드 지원, 오프라인 모드",
  "constraints": "애자일 방법론, 2주 스프린트, iOS 14+ 지원"
}
```

### 예시 3: 마크다운 워크플로우 (2단계, 권장)

#### Step 1: 명세서 생성
```bash
POST /api/v1/wbs/generate-spec
```
```json
{
  "project_name": "신규 앱 개발",
  "project_type": "모바일 앱",
  "team_size": 5,
  "expected_duration_days": 30
}
```

**응답**:
```json
{
  "project_name": "신규 앱 개발",
  "markdown_spec": "# 프로젝트 명세서: 신규 앱 개발\n\n## 📋 프로젝트 개요\n..."
}
```

#### Step 2: 사용자가 명세서 편집 후 WBS 생성
```bash
POST /api/v1/wbs/generate-from-spec
```
```json
{
  "markdown_spec": "# 수정된 명세서\n\n## 핵심 기능\n### 1. 로그인\n- 소셜 로그인\n..."
}
```

### 예시 4: 스프링 서버 DB 저장용 (Flat 구조)

```bash
POST /api/v1/wbs/generate-from-spec/flat
```

**응답** (1차원 배열):
```json
{
  "project_name": "신규 앱 개발",
  "total_tasks": 12,
  "total_duration_days": 30,
  "tasks": [
    {
      "task_id": "1.0",
      "parent_task_id": null,
      "name": "기획",
      "assignee": "PM",
      "start_date": "2024-01-01",
      "end_date": "2024-01-10",
      "duration_days": 10,
      "progress": 0,
      "status": "할일"
    },
    {
      "task_id": "1.1",
      "parent_task_id": "1.0",
      "name": "요구사항 분석",
      "assignee": "BA",
      "start_date": "2024-01-01",
      "end_date": "2024-01-05",
      "duration_days": 5,
      "progress": 0,
      "status": "할일"
    },
    {
      "task_id": "1.2",
      "parent_task_id": "1.0",
      "name": "기획서 작성",
      "assignee": "PM",
      "start_date": "2024-01-06",
      "end_date": "2024-01-10",
      "duration_days": 5,
      "progress": 0,
      "status": "할일"
    }
  ]
}
```

## 입력 필드 설명

### WBSGenerateRequest (17개 필드)

| 필드 | 필수 | 타입 | 설명 | 예시 |
|------|------|------|------|------|
| **기본 정보 (필수)** |||||
| `project_name` | O | string | 프로젝트명 | "FlowPlan 앱 개발" |
| `project_type` | O | string | 프로젝트 주제/종류 | "모바일 앱", "웹 서비스" |
| `team_size` | O | int | 참여 인원 수 | 5, 10 |
| `expected_duration_days` | O | int | 예상 기간(일) | 60, 90 |
| **일정 정보 (선택)** |||||
| `project_duration` | X | object | 시작일/마감일 | `{"start_date": "2024-01-01", "end_date": "2024-03-31"}` |
| **프로젝트 정보 (선택)** |||||
| `budget` | X | string | 예산 | "1억원", "$100,000" |
| `priority` | X | string | 우선순위 | "높음", "중간", "낮음" |
| `stakeholders` | X | array | 이해관계자 목록 | ["CEO", "CTO", "마케팅 이사"] |
| `deliverables` | X | array | 주요 산출물 목록 | ["iOS 앱", "Android 앱", "API 문서"] |
| `risks` | X | array | 예상 리스크 목록 | ["일정 지연 가능성", "리소스 부족"] |
| **상세 요구사항 (선택)** |||||
| `project_purpose` | X | string | 프로젝트 목적 | "일정 관리 플랫폼 개발" |
| `key_features` | X | array | 주요 기능 목록 | ["간트차트", "WBS", "칸반보드"] |
| `detailed_requirements` | X | string | 구체적 요구사항 | "반응형 디자인, 다크모드 지원" |
| `constraints` | X | string | 제약사항 | "애자일 방법론, 2주 스프린트" |

**Tip**: 선택 필드를 많이 입력할수록 AI가 더 정확하고 상세한 WBS를 생성합니다!

## 스프링 서버 통합 가이드

### Tasks 테이블 ERD 매핑

FlowPlanAI의 출력은 스프링 서버의 Tasks 테이블 구조에 맞춰 설계되었습니다:

| FlowPlanAI 필드 | Tasks 테이블 컬럼 | 설명 |
|-----------------|-------------------|------|
| `task_id` | - | UI 표시용 (1.0, 1.1, 1.2...) |
| `parent_task_id` | `parent_id` (FK) | 부모 작업의 task_id → DB id 매핑 필요 |
| `name` | `name` | 작업명 |
| `assignee` | `assignee_id` (FK) | 담당자 (사용자 ID 매핑 필요) |
| `start_date` | `start_date` | 시작일 (YYYY-MM-DD) |
| `end_date` | `end_date` | 마감일 (YYYY-MM-DD) |
| `progress` | `progress` | 진행률 (항상 0) |
| `status` | `status` | 상태 (항상 "할일") |

### 스프링 서버 저장 예시 (Java)

```java
@Service
public class WBSService {
    
    @Transactional
    public void saveWBSTasks(List<WBSTaskDto> tasks, Long projectId) {
        // task_id → DB id 매핑 (순서대로 저장)
        Map<String, Long> taskIdToDbId = new HashMap<>();
        
        for (WBSTaskDto dto : tasks) {
            Task task = new Task();
            task.setProjectId(projectId);
            task.setName(dto.getName());
            task.setStartDate(dto.getStartDate());
            task.setEndDate(dto.getEndDate());
            task.setProgress(dto.getProgress());
            task.setStatus(dto.getStatus());
            
            // assignee 매핑 (문자열 → 사용자 ID)
            User user = userRepository.findByName(dto.getAssignee())
                .orElseThrow(() -> new UserNotFoundException());
            task.setAssigneeId(user.getId());
            
            // 부모 작업 ID 설정
            if (dto.getParentTaskId() != null) {
                Long parentDbId = taskIdToDbId.get(dto.getParentTaskId());
                task.setParentId(parentDbId);
            }
            
            // 저장 → auto_increment로 DB id 생성
            Task saved = taskRepository.save(task);
            
            // task_id → DB id 매핑 저장
            taskIdToDbId.put(dto.getTaskId(), saved.getId());
        }
    }
}
```

### 핵심 포인트

1. **순서 보장**: AI가 반환하는 배열은 **부모 → 자식 순서**가 보장됨
2. **task_id는 논리적 계층**: 실제 DB id는 auto_increment로 생성
3. **parent_task_id 매핑**: 순서대로 저장하며 `Map`으로 task_id → DB id 변환

## 워크플로우 비교

### 방법 1: 직접 생성 (빠름)
```
프로젝트 정보 입력
    ↓
POST /api/v1/wbs/generate
    ↓
WBS 생성 완료
```
**장점**: 빠름  
**단점**: AI가 추측한 내용 포함 가능

### 방법 2: 마크다운 워크플로우 (권장)
```
프로젝트 정보 입력
    ↓
POST /api/v1/wbs/generate-spec
    ↓
마크다운 명세서 생성
    ↓
사용자가 상세히 편집
    ↓
POST /api/v1/wbs/generate-from-spec/flat
    ↓
정확한 WBS 생성
```
**장점**: 사용자가 검토/수정 가능, 더 정확한 결과  
**단점**: 2단계 필요
