# FlowPlan 프로젝트 전용 GitHub Copilot 지침

## 프로젝트 개요
- **프로젝트명**: FlowPlan
- **목적**: AI 기반 프로젝트 일정 관리 및 WBS 자동 생성 플랫폼
- **아키텍처**: 마이크로서비스 아키텍처 (Docker Compose 기반)
- **배포 방식**: Docker 컨테이너화, 멀티 서비스 오케스트레이션

## 전체 서비스 구성

### 1. FlowPlanAI (Python FastAPI)
- **역할**: AI 기반 WBS 자동 생성 백엔드
- **기술 스택**: Python 3.11+, FastAPI, Google Gemini AI
- **포트**: 8000 (기본)
- **책임**:
  - 프로젝트 정보 기반 WBS 자동 생성
  - 마크다운 명세서 생성
  - Gemini AI 통합
  - RESTful API 제공

### 2. FlowPlan 스프링 서버 (Spring Boot)
- **역할**: 인증/인가 및 메인 비즈니스 로직
- **기술 스택**: Java/Kotlin, Spring Boot, Spring Security
- **포트**: 8080 (기본)
- **책임**:
  - 사용자 인증/인가 (JWT, OAuth 등)
  - 프로젝트/작업 CRUD
  - DB 영속성 관리 (JPA/Hibernate)
  - FlowPlanAI API 호출 및 통합
  - Tasks 테이블 관리 (ERD 기준)

## 프로젝트 디렉토리 구조

```
FlowPlan/
├── docker-compose.yml           # 멀티 서비스 오케스트레이션
├── .env                         # 전역 환경 변수
├── .github/
│   └── copilot-instructions.md  # 이 파일
├── FlowPlanAI/                  # AI 서비스 (FastAPI)
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── .env
│   ├── app/
│   │   ├── main.py
│   │   ├── api/routes/
│   │   ├── services/
│   │   ├── models/
│   │   └── utils/
│   └── .github/
│       └── copilot-instructions.md  # AI 서비스 전용 지침
└── FlowPlanServer/              # 스프링 서버 (Git에서 클론)
    ├── Dockerfile
    ├── build.gradle / pom.xml
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   └── resources/
    │   └── test/
    └── .github/
        └── copilot-instructions.md  # 스프링 서버 전용 지침
```

## Docker 멀티 서비스 아키텍처 규칙

### 1. Docker Compose 설계
- **네트워크**: 서비스 간 통신을 위한 bridge 네트워크 정의
- **볼륨**: 데이터베이스 영속성, 로그 저장
- **의존성**: `depends_on`으로 서비스 시작 순서 제어
- **환경 변수**: `.env` 파일로 중앙 관리

```yaml
# docker-compose.yml 예시
version: '3.8'
services:
  flowplan-ai:
    build: ./FlowPlanAI
    ports:
      - "8000:8000"
    environment:
      - GEMINI_API_KEY=${GEMINI_API_KEY}
    networks:
      - flowplan-network
  
  flowplan-server:
    build: ./FlowPlanServer
    ports:
      - "8080:8080"
    depends_on:
      - flowplan-ai
      - db
    environment:
      - AI_SERVICE_URL=http://flowplan-ai:8000
    networks:
      - flowplan-network
  
  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=flowplan
    volumes:
      - db-data:/var/lib/postgresql/data
    networks:
      - flowplan-network

networks:
  flowplan-network:
    driver: bridge

volumes:
  db-data:
```

### 2. 서비스 간 통신
- **AI → 스프링**: 직접 통신 없음 (스프링이 클라이언트)
- **스프링 → AI**: HTTP 요청으로 WBS 생성 API 호출
  - URL: `http://flowplan-ai:8000/api/v1/wbs/generate`
  - RestTemplate, WebClient, Feign 등 사용
- **인증**: 스프링 서버에서만 처리, AI 서비스는 토큰 검증 불필요 (내부 통신)

### 3. Dockerfile 작성 규칙

#### FlowPlanAI Dockerfile
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY ./app ./app
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

#### FlowPlanServer Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4. 환경 변수 관리
- **루트 `.env`**: 공통 환경 변수 (DB 정보, API 키 등)
- **서비스별 `.env`**: 각 서비스 전용 설정
- **Docker Compose**: `env_file` 또는 `environment`로 주입

```env
# 루트 .env
POSTGRES_DB=flowplan
POSTGRES_USER=admin
POSTGRES_PASSWORD=secure_password
GEMINI_API_KEY=your_gemini_key

# FlowPlanAI/.env
APP_NAME=FlowPlanAI
APP_VERSION=1.0.0
API_V1_PREFIX=/api/v1

# FlowPlanServer/.env (예정)
SPRING_PROFILES_ACTIVE=prod
AI_SERVICE_BASE_URL=http://flowplan-ai:8000
```

## 서비스별 책임 분리

### FlowPlanAI 담당
✅ WBS 자동 생성 (Gemini AI)
✅ 마크다운 명세서 생성
✅ Flat 구조 변환 (parent_task_id)
✅ AI 프롬프트 엔지니어링
❌ 사용자 인증/인가
❌ DB 영속성 (직접 DB 접근 안 함)
❌ 프로젝트/작업 CRUD

### FlowPlanServer 담당
✅ 사용자 회원가입/로그인
✅ JWT 토큰 발급/검증
✅ 프로젝트/작업 CRUD (JPA)
✅ Tasks 테이블 저장/조회
✅ FlowPlanAI API 호출 및 응답 저장
✅ 권한 관리 (프로젝트 멤버십)
❌ AI 로직 (FlowPlanAI에 위임)

## 통합 워크플로우 예시

### WBS 생성 전체 프로세스
1. **클라이언트** → 스프링 서버: `POST /api/projects/{id}/wbs` (JWT 포함)
2. **스프링 서버**: JWT 검증 및 권한 확인
3. **스프링 서버** → FlowPlanAI: `POST /api/v1/wbs/generate` (프로젝트 정보)
4. **FlowPlanAI**: Gemini AI로 WBS 생성 후 응답
5. **스프링 서버**: 응답받은 WBS를 Tasks 테이블에 저장
6. **스프링 서버** → 클라이언트: 저장된 WBS 반환

## 코딩 규칙 (공통)

### 1. API 설계
- **RESTful 원칙** 준수
- **일관된 응답 형식**:
  ```json
  {
    "success": true,
    "data": { ... },
    "message": "성공",
    "timestamp": "2025-10-30T12:00:00Z"
  }
  ```
- **에러 응답 표준화**:
  ```json
  {
    "success": false,
    "error": {
      "code": "INVALID_INPUT",
      "message": "프로젝트 이름은 필수입니다"
    },
    "timestamp": "2025-10-30T12:00:00Z"
  }
  ```

### 2. 보안
- **민감 정보**: 절대 Git 커밋 금지 (`.env`, API 키)
- **서비스 간 통신**: Docker 내부 네트워크만 허용
- **외부 노출**: 스프링 서버 포트만 외부 공개, AI 서비스는 내부 전용

### 3. 로깅
- **구조화된 로그**: JSON 형식 권장
- **레벨**: DEBUG, INFO, WARN, ERROR
- **컨텍스트**: 요청 ID, 사용자 ID, 타임스탬프 포함

### 4. 에러 핸들링
- **서비스 장애 대응**: Retry 로직, Circuit Breaker
- **타임아웃 설정**: AI API 호출 30초, DB 쿼리 10초
- **Fallback**: AI 서비스 장애 시 기본 WBS 템플릿 제공

## Git 워크플로우

### 브랜치 전략
- `main`: 프로덕션 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `hotfix/*`: 긴급 수정 브랜치

### 커밋 메시지 규칙
```
[서비스명] 타입: 제목

- 상세 설명 1
- 상세 설명 2

예시:
[AI] feat: Gemini 프롬프트 최적화로 WBS 정확도 향상
[Server] fix: JWT 만료 시간 검증 로직 수정
[Docker] chore: PostgreSQL 볼륨 마운트 경로 변경
```

### 타입
- `feat`: 새 기능
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `docs`: 문서 수정
- `test`: 테스트 추가
- `chore`: 빌드/설정 변경

## 배포 및 운영

### 개발 환경
```bash
# 전체 서비스 실행
docker-compose up --build

# 특정 서비스만 재시작
docker-compose restart flowplan-ai
```

### 프로덕션 배포
- **CI/CD**: GitHub Actions (예정)
- **컨테이너 레지스트리**: Docker Hub / AWS ECR
- **오케스트레이션**: Docker Compose → Kubernetes (확장 시)

### 모니터링
- **헬스 체크**: `/health` 엔드포인트 필수
- **메트릭**: Prometheus + Grafana (선택사항)
- **로그 수집**: ELK Stack / CloudWatch

## 새 서비스 추가 시 체크리스트
- [ ] `docker-compose.yml`에 서비스 정의 추가
- [ ] Dockerfile 작성
- [ ] 서비스별 `.github/copilot-instructions.md` 작성
- [ ] 환경 변수 `.env` 설정
- [ ] 네트워크 및 의존성 설정
- [ ] 헬스 체크 엔드포인트 구현
- [ ] README.md 업데이트

## 금지 사항
- ❌ 서비스 간 직접 DB 접근 (각자의 DB만 접근)
- ❌ API 키를 Dockerfile이나 docker-compose.yml에 하드코딩
- ❌ 외부에 AI 서비스 포트 직접 노출
- ❌ 서비스별 책임 경계 위반 (AI 서비스에서 인증 처리 등)
- ❌ 민감 정보를 Git 커밋

## 참고 자료
- **각 서비스별 상세 지침**: 하위 디렉토리의 `.github/copilot-instructions.md` 참조
- **Docker 네트워크**: 서비스명으로 내부 통신 (`http://서비스명:포트`)
- **Tasks ERD**: 스프링 서버의 도메인 모델 참조

---

> **Copilot 사용 팁**: 
> - 각 서비스 코드 작업 시 해당 서비스 디렉토리의 `copilot-instructions.md` 참조
> - Docker 관련 작업 시 이 루트 레벨 지침 참조
> - 서비스 간 통신 구현 시 Docker 네트워크 규칙 확인

