# FlowPlan - AI 기반 프로젝트 일정 관리 플랫폼

> Google Gemini AI를 활용한 WBS(Work Breakdown Structure) 자동 생성 및 프로젝트 관리 시스템

[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)
[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109.0-green.svg)](https://fastapi.tiangolo.com/)

## 📋 프로젝트 개요

FlowPlan은 AI를 활용하여 프로젝트 일정을 자동으로 생성하고 관리하는 플랫폼입니다. 
마이크로서비스 아키텍처로 설계되어 AI 서비스와 메인 비즈니스 로직을 분리하여 확장성과 유지보수성을 확보했습니다.

### 주요 기능
- 🤖 **AI 기반 WBS 자동 생성**: 프로젝트 정보만으로 작업 분해 구조 자동 생성
- 📝 **마크다운 명세서**: AI가 생성한 명세서를 사용자가 편집 후 정확한 WBS 생성
- 👥 **사용자 인증/인가**: JWT 기반 보안 인증 시스템
- 📊 **프로젝트/작업 관리**: CRUD 및 진행률 추적
- 🐳 **Docker 컨테이너화**: 간편한 배포 및 확장

## 🏗️ 아키텍처

```
┌─────────────────┐
│   클라이언트    │ (React/Vue - 예정)
└────────┬────────┘
         │ HTTP/HTTPS
         ▼
┌─────────────────┐
│ FlowPlanServer  │ (Spring Boot)
│   - 인증/인가    │ :8080
│   - 프로젝트 관리│
│   - DB 영속성    │
└────────┬────────┘
         │ HTTP (내부 네트워크)
         ▼
┌─────────────────┐
│  FlowPlanAI     │ (FastAPI)
│   - WBS 생성     │ :8000
│   - Gemini AI    │
└─────────────────┘
         │
         ▼
┌─────────────────┐
│  PostgreSQL DB  │ :5432
└─────────────────┘
```

### 서비스 구성

| 서비스 | 역할 | 기술 스택 | 포트 |
|--------|------|-----------|------|
| **FlowPlanAI** | AI 기반 WBS 생성 | Python, FastAPI, Gemini | 8000 |
| **FlowPlanServer** | 인증/인가, 메인 로직 | Java/Kotlin, Spring Boot | 8080 |
| **PostgreSQL** | 데이터베이스 | PostgreSQL 15 | 5432 |

## 🚀 빠른 시작

### 사전 요구사항
- Docker & Docker Compose 설치
- Gemini API 키 ([발급 받기](https://ai.google.dev/))
- Git

### 1. 프로젝트 클론 및 스프링 서버 설정

```bash
# FlowPlan 메인 프로젝트 (현재 위치)
cd C:\FlowPlan

# 스프링 서버 클론 (Git 주소는 실제 레포지토리로 변경)
git clone <FlowPlanServer_Git_URL> FlowPlanServer
```

### 2. 환경 변수 설정

```bash
# .env.example을 복사하여 .env 생성
copy .env.example .env

# .env 파일 편집
notepad .env
```

**필수 설정:**
```env
GEMINI_API_KEY=your_actual_gemini_api_key
POSTGRES_PASSWORD=secure_production_password
```

### 3. Docker Compose로 전체 서비스 실행

```bash
# AI 서비스만 실행 (현재)
docker-compose up -d flowplan-ai

# 스프링 서버 추가 후 전체 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

### 4. API 문서 확인

- **AI 서비스 (FastAPI)**: http://localhost:8000/docs
- **스프링 서버 (예정)**: http://localhost:8080/swagger-ui.html

## 📂 프로젝트 구조

```
FlowPlan/
├── .github/
│   └── copilot-instructions.md    # GitHub Copilot 프로젝트 지침
├── FlowPlanAI/                    # AI 마이크로서비스
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── .github/
│   │   └── copilot-instructions.md
│   └── app/
│       ├── main.py
│       ├── api/routes/
│       ├── services/              # Gemini 통합, WBS 생성
│       ├── models/                # Pydantic 모델
│       └── utils/
├── FlowPlanServer/                # 스프링 서버 (Git에서 클론)
│   ├── Dockerfile
│   ├── src/main/
│   │   ├── java/
│   │   └── resources/
│   └── .github/
│       └── copilot-instructions.md
├── docker-compose.yml             # 멀티 서비스 오케스트레이션
├── .env.example                   # 환경 변수 템플릿
├── .gitignore
└── README.md
```

## 🔧 개발 가이드

### AI 서비스 (FlowPlanAI) 개발

```bash
cd FlowPlanAI

# 가상 환경 생성
python -m venv venv
venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 로컬 실행
uvicorn app.main:app --reload --port 8000
```

### 스프링 서버 (FlowPlanServer) 개발

```bash
cd FlowPlanServer

# Gradle 빌드
./gradlew build

# 로컬 실행
./gradlew bootRun
```

### Docker로 개별 서비스 재시작

```bash
# AI 서비스만 재빌드 및 재시작
docker-compose up -d --build flowplan-ai

# 스프링 서버만 재시작
docker-compose restart flowplan-server
```

## 📡 API 사용 예시

### WBS 자동 생성 (AI 서비스 직접 호출 - 테스트용)

```bash
curl -X POST http://localhost:8000/api/v1/wbs/generate \
  -H "Content-Type: application/json" \
  -d '{
    "project_name": "신규 쇼핑몰 개발",
    "project_type": "웹 애플리케이션",
    "team_size": 5,
    "expected_duration_days": 90
  }'
```

### WBS 생성 (스프링 서버 경유 - 프로덕션)

```bash
# JWT 토큰 발급 후
curl -X POST http://localhost:8080/api/projects/1/wbs \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "project_name": "신규 쇼핑몰 개발",
    "project_type": "웹 애플리케이션",
    "team_size": 5,
    "expected_duration_days": 90
  }'
```

## 🧪 테스트

### AI 서비스 테스트

```bash
cd FlowPlanAI
pytest tests/
```

### 스프링 서버 테스트

```bash
cd FlowPlanServer
./gradlew test
```

## 📦 배포

### 프로덕션 배포 체크리스트
- [ ] `.env` 파일에 실제 API 키 및 비밀번호 설정
- [ ] `docker-compose.yml`에서 불필요한 포트 매핑 제거
- [ ] CORS 설정을 특정 도메인으로 제한
- [ ] PostgreSQL 볼륨 백업 전략 수립
- [ ] 로그 수집 및 모니터링 설정
- [ ] HTTPS/SSL 인증서 적용

### Docker Hub에 이미지 푸시

```bash
# AI 서비스
docker build -t username/flowplan-ai:1.0 ./FlowPlanAI
docker push username/flowplan-ai:1.0

# 스프링 서버
docker build -t username/flowplan-server:1.0 ./FlowPlanServer
docker push username/flowplan-server:1.0
```

## 🤝 기여 가이드

### Git 브랜치 전략
- `main`: 프로덕션 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 새 기능 개발
- `hotfix/*`: 긴급 수정

### 커밋 메시지 규칙
```
[서비스명] 타입: 제목

상세 설명

예시:
[AI] feat: Gemini 프롬프트 최적화로 WBS 정확도 20% 향상
[Server] fix: JWT 토큰 갱신 로직 버그 수정
```

## 📄 라이선스

MIT License (예정)

## 📞 문의

- **이슈 등록**: [GitHub Issues](이슈 URL)
- **Wiki**: [프로젝트 Wiki](위키 URL)

---

> **개발 팁**: 각 서비스 디렉토리의 `.github/copilot-instructions.md` 파일을 확인하여 GitHub Copilot이 프로젝트 규칙에 맞는 코드를 생성하도록 설정되어 있습니다.

