# FlowPlanAI 서비스 전용 GitHub Copilot 지침

> ⚠️ **전체 프로젝트 아키텍처는 루트의 `.github/copilot-instructions.md` 참조**

## 서비스 개요
- **서비스명**: FlowPlanAI (AI 마이크로서비스)
- **역할**: Google Gemini AI를 활용한 WBS 자동 생성 전담
- **프레임워크**: FastAPI (Python 3.11+)
- **AI 모델**: Google Gemini 2.0 Flash Experimental
- **아키텍처**: REST API, 비동기 처리, Pydantic 기반 데이터 검증
- **배포**: Docker 컨테이너, FlowPlan 스프링 서버와 함께 운영

## 전체 프로젝트 컨텍스트
- **상위 프로젝트**: FlowPlan (AI 기반 프로젝트 일정 관리 플랫폼)
- **다른 서비스**: FlowPlanServer (Spring Boot - 인증/인가 및 메인 비즈니스 로직)
- **통신 방식**: 스프링 서버 → FlowPlanAI (HTTP 요청)
- **책임 범위**: AI 로직만 담당, 인증/DB는 스프링 서버가 처리

## 핵심 기능
1. **마크다운 명세서 생성**: 프로젝트 정보 → 상세 마크다운 명세서 (`/generate-spec`)
2. **명세서 기반 WBS 생성**: 사용자 편집 마크다운 → 정확한 계층 WBS (`/generate-from-spec`)
3. **Flat 구조 변환**: 명세서로 생성된 WBS → parent_task_id로 연결된 1차원 배열 (`/generate-from-spec/flat`) (스프링 DB 호환)

## 코딩 규칙 및 가이드라인

### 1. 프로젝트 구조
```
app/
├── main.py                 # FastAPI 앱 진입점, CORS 설정
├── core/
│   └── config.py          # 환경 변수 관리 (pydantic-settings)
├── api/routes/
│   └── wbs.py             # WBS 관련 엔드포인트
├── models/
│   ├── request.py         # 요청 모델 (Pydantic)
│   ├── response.py        # 응답 모델 (Pydantic)
│   └── markdown.py        # 마크다운 관련 모델
├── services/
│   ├── gemini_service.py         # Gemini API 호출 로직
│   ├── wbs_generator.py          # WBS 생성 비즈니스 로직
│   ├── markdown_generator.py     # 마크다운 명세서 생성
│   └── wbs_from_markdown.py      # 마크다운 → WBS 변환
└── utils/
    └── wbs_converter.py   # WBS 구조 변환 유틸리티
```

### 2. 코드 스타일
- **타입 힌팅 필수**: 모든 함수 파라미터와 반환값에 타입 명시
- **Pydantic 모델**: 모든 요청/응답은 Pydantic BaseModel 사용
- **비동기 처리**: AI API 호출은 `async`/`await` 사용
- **Docstring**: Google 스타일 독스트링 작성
  ```python
  async def generate_wbs(request: WBSGenerateRequest) -> WBSGenerateResponse:
      """WBS 생성 메인 로직
      
      Args:
          request: WBS 생성 요청
          
      Returns:
          생성된 WBS 응답
      """
  ```

### 3. 데이터 모델 규칙

#### 요청 모델 (request.py)
- **필수 필드 4개**: `project_name`, `project_type`, `team_size`, `expected_duration_days`
- **선택 필드 (총 12개 계약)**: 지금 서비스는 아래 추가 선택 필드를 지원합니다: `start_date`, `end_date`, `budget`, `priority`, `stakeholders`, `deliverables`, `risks`, `detailed_requirements`.
- Field 설명에 `[필수]` 또는 `[선택]` 태그 명시
- 예시:
  ```python
  project_name: str = Field(..., description="[필수] 프로젝트명")
  budget: Optional[str] = Field(None, description="[선택] 예산")
  ```

#### 응답 모델 (response.py)
- **WBSTask**: 스프링 서버 ERD Tasks 테이블 호환 구조
  - `task_id`: 계층 구조 ID (예: "1.0", "1.1", "1.1.1")
  - `parent_id`: 상위 작업 ID (최상위는 None)
  - `status`: Enum으로 "할일", "진행중", "완료"
  - `progress`: 0-100 사이 정수, 기본값 0
  - `subtasks`: 재귀적 하위 작업 리스트 (Optional)

### 4. AI 프롬프트 엔지니어링
- **Gemini 서비스**: `app/services/gemini_service.py`에 집중
- **프롬프트 구조**:
  1. 역할 정의: "당신은 프로젝트 관리 전문가입니다"
  2. 작업 설명: WBS 생성 목적과 요구사항
  3. 형식 지정: JSON 구조, 필드명, 타입
  4. 제약 조건: 계층 구조, 날짜 로직, 담당자 배분
  5. 예시 제공: 실제 출력 예시

### 5. 에러 핸들링
- **Gemini API 오류**: `gemini_service.py`에서 try-except로 처리
- **JSON 파싱 오류**: 마크다운 코드블록 제거 후 파싱
  ```python
  json_str = re.sub(r'^```(?:json)?\s*\n', '', json_str, flags=re.MULTILINE)
  json_str = re.sub(r'\n```\s*$', '', json_str, flags=re.MULTILINE)
  ```
- **Pydantic 검증 오류**: FastAPI가 자동으로 422 응답 반환
- **HTTPException**: 명확한 status_code와 detail 메시지

### 6. 환경 변수
- `.env` 파일에 민감 정보 저장
- `app/core/config.py`에서 pydantic-settings로 관리
- 필수 환경 변수:
  - `GEMINI_API_KEY`: Google Gemini API 키
  - `APP_NAME`: 앱 이름
  - `APP_VERSION`: 버전
  - `API_V1_PREFIX`: API 경로 프리픽스 (예: /api/v1)

### 7. API 엔드포인트 설계
- **라우터**: `/api/v1/wbs` 프리픽스
- **태그**: "WBS"로 그룹화
- **문서화**: `summary`, `description`, `examples` 충실히 작성
- **상태 코드**:
  - 200: 성공
  - 422: 요청 데이터 검증 실패
  - 500: 서버 내부 오류 (AI API 오류 등)

### 8. 테스트 고려사항
- FastAPI의 `/docs` (Swagger UI)로 수동 테스트
- 요청 예시: `examples` 딕셔너리로 minimal/full 버전 제공
- AI 응답 변동성: JSON 파싱 로직 견고하게 작성

### 9. 성능 최적화
- **비동기 I/O**: Gemini API 호출은 async로 non-blocking
- **응답 캐싱**: 동일 요청 반복 시 고려 (선택사항)
- **타임아웃 설정**: AI API 호출에 적절한 timeout 설정

### 10. 보안
- **API 키 보호**: .env 파일, 환경 변수로만 관리
- **CORS**: 프로덕션에서는 특정 도메인만 허용
- **입력 검증**: Pydantic으로 자동 검증, 추가 비즈니스 로직 검증

## 새 기능 추가 시 체크리스트
- [ ] `models/request.py`에 요청 모델 추가/수정
- [ ] `models/response.py`에 응답 모델 추가/수정
- [ ] `services/`에 비즈니스 로직 구현
- [ ] `api/routes/wbs.py`에 엔드포인트 추가
- [ ] Docstring 및 타입 힌팅 작성
- [ ] `/docs`에서 API 문서 확인
- [ ] 에러 케이스 처리 (try-except)
- [ ] README.md 업데이트 (필요 시)

## Docker 및 배포

### Dockerfile 구조
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY ./app ./app
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 환경 변수 (Docker Compose 주입)
- `GEMINI_API_KEY`: Gemini API 키 (필수)
- `APP_NAME`: 앱 이름
- `APP_VERSION`: 버전
- `API_V1_PREFIX`: API 경로 프리픽스

### 서비스 간 통신
- **FlowPlanAI → 스프링 서버**: 없음 (FlowPlanAI는 수동적 역할)
- **스프링 서버 → FlowPlanAI**: HTTP 요청으로 WBS 생성 API 호출
  - Docker 내부 URL: `http://flowplan-ai:8000/api/v1/wbs/generate`
  - 인증 불필요 (내부 네트워크 통신)
- **외부 노출**: Docker Compose에서 포트 매핑 여부 결정 (기본적으로 내부 전용)

### 헬스 체크
- `/health` 엔드포인트 필수 구현
- Docker Compose healthcheck에서 사용
- Gemini API 연결 상태 확인 포함 권장

## 금지 사항
- ❌ 타입 힌팅 없는 함수
- ❌ API 키를 코드에 하드코딩
- ❌ 동기 방식 AI API 호출 (async 필수)
- ❌ Pydantic 검증 우회
- ❌ 독스트링 없는 public 함수
- ❌ ERD 스키마와 호환되지 않는 응답 구조
- ❌ **사용자 인증/인가 구현** (스프링 서버 책임)
- ❌ **직접 DB 접근** (스프링 서버를 통해서만)
- ❌ **JWT 토큰 검증** (내부 서비스이므로 불필요)

## 참고 정보
- **AI 모델**: gemini-2.0-flash-exp (빠른 응답, 실험적 기능)
- **스프링 서버 연동**: `flatten_wbs_for_spring()` 함수로 flat 구조 변환
- **WBS 계층**: task_id 형식 "1.0" → "1.1" → "1.1.1" (최대 3단계 권장)
- **날짜 계산**: `start_date + duration_days = end_date` 로직 일관성 유지
- **전체 아키텍처**: 루트 `.github/copilot-instructions.md` 참조

## 스프링 서버와의 데이터 계약

### WBS 응답 구조 (Tasks 테이블 매핑)
```python
# FlowPlanAI 응답
{
  "project_name": "프로젝트명",
  "total_tasks": 10,
  "total_duration_days": 30,
  "wbs_structure": [
    {
      "task_id": "1.0",           # 계층 구조 ID
      "parent_id": null,          # 최상위는 null
      "name": "작업명",
      "assignee": "PM",           # 담당자 (스프링에서 User ID로 매핑)
      "start_date": "2025-01-01",
      "end_date": "2025-01-10",
      "duration_days": 10,
      "progress": 0,              # 기본값 0
      "status": "할일",           # "할일", "진행중", "완료"
      "subtasks": [...]           # 재귀적 하위 작업
    }
  ]
}

# 스프링 서버에서 Tasks 테이블에 저장 시
- assignee (str) → assignee_id (Long, User FK)
- project_name → project_id (Long, Project FK)
- task_id/parent_id → 계층 구조 유지하여 저장
```

### Flat 구조 변환 (선택사항)
- `flatten_wbs_for_spring()` 함수로 1차원 배열 변환
- parent_task_id로 계층 관계 유지
- 스프링 서버에서 직접 처리 가능하므로 선택적 사용

---

> **Copilot 사용 팁**: 
> - 이 서비스는 AI 로직에만 집중, 인증/DB는 절대 구현하지 않음
> - 스프링 서버와의 데이터 계약 (응답 구조) 변경 시 양쪽 모두 업데이트
> - Gemini API 프롬프트 수정 시 `gemini_service.py`의 기존 스타일 유지
> - Docker 환경에서 실행됨을 항상 고려 (환경 변수, 포트, 네트워크)

