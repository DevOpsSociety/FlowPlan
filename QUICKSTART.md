# 🚀 FlowPlan 빠른 시작 가이드

## 현재 상황 요약
✅ **FlowPlanAI** (AI 서비스) - 완료, 독립 실행 가능  
⚠️ **FlowPlanServer** (스프링 서버) - Git 클론 필요

---

## 방법 1: AI 서비스만 실행 (바로 시작 가능) ⚡

### 1단계: Gemini API 키 설정
```bash
# .env 파일 열기
notepad C:\FlowPlan\.env

# 다음 라인 수정:
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### 2단계: Docker로 실행
```bash
cd C:\FlowPlan
docker-compose up -d flowplan-ai
```

### 3단계: 테스트
```bash
# 헬스체크
curl http://localhost:8000/health

# API 문서 (브라우저에서)
start http://localhost:8000/docs

# WBS 생성 테스트
curl -X POST http://localhost:8000/api/v1/wbs/generate ^
  -H "Content-Type: application/json" ^
  -d "{\"project_name\":\"테스트 프로젝트\",\"project_type\":\"웹 앱\",\"team_size\":5,\"expected_duration_days\":30}"
```

✅ **완료!** AI 서비스가 동작합니다.

---

## 방법 2: 전체 시스템 실행 (스프링 서버 포함) 🔧

### 사전 요구사항
- Git 설치 확인: `git --version`
- 스프링 서버 Git Repository URL
- Docker Desktop 실행 중

### 1단계: 스프링 서버 클론

#### 옵션 A: 빠른 클론 (기존 폴더 제거)
```bash
cd C:\FlowPlan
rmdir /s /q FlowPlanServer
git clone <스프링서버_Git_URL> FlowPlanServer
```

#### 옵션 B: 백업 후 클론
```bash
cd C:\FlowPlan
move FlowPlanServer FlowPlanServer_backup
git clone <스프링서버_Git_URL> FlowPlanServer
```

### 2단계: Copilot 지침 추가
```bash
cd C:\FlowPlan

# .github 폴더 생성
mkdir FlowPlanServer\.github

# Copilot 지침 복사
copy SPRING_SERVER_COPILOT_INSTRUCTIONS_TEMPLATE.md FlowPlanServer\.github\copilot-instructions.md
```

### 3단계: 스프링 서버 설정 확인

#### Dockerfile 확인 (없으면 생성)
```bash
cd FlowPlanServer
dir Dockerfile
```

없으면 다음 내용으로 생성:
```dockerfile
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon -x test

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### application.yml 확인
`src/main/resources/application.yml`에 다음 설정 추가:
```yaml
ai-service:
  base-url: ${AI_SERVICE_BASE_URL:http://flowplan-ai:8000}

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://db:5432/flowplan}
    username: ${SPRING_DATASOURCE_USERNAME:flowplan_admin}
    password: ${SPRING_DATASOURCE_PASSWORD:password}
```

### 4단계: docker-compose.yml 수정
```bash
cd C:\FlowPlan
notepad docker-compose.yml
```

**다음 섹션의 주석(#) 제거:**
- `flowplan-server` 전체 섹션
- `db` 전체 섹션
- `volumes: postgres-data` 섹션

### 5단계: 환경 변수 추가
```bash
notepad C:\FlowPlan\.env
```

**추가할 내용:**
```env
# JWT 설정
JWT_SECRET=your_jwt_secret_key_change_this
JWT_EXPIRATION=86400000
```

### 6단계: 전체 서비스 실행
```bash
cd C:\FlowPlan

# 모든 서비스 빌드 및 실행 (첫 실행은 5-10분 소요)
docker-compose up -d --build

# 로그 확인 (Ctrl+C로 종료)
docker-compose logs -f
```

### 7단계: 테스트
```bash
# DB 헬스체크
docker-compose exec db pg_isready -U flowplan_admin

# AI 서비스
curl http://localhost:8000/health

# 스프링 서버
curl http://localhost:8080/actuator/health
```

✅ **완료!** 전체 시스템이 동작합니다.

---

## 📊 서비스 포트

| 서비스 | 포트 | URL |
|--------|------|-----|
| AI 서비스 (FastAPI) | 8000 | http://localhost:8000 |
| AI 서비스 문서 | 8000 | http://localhost:8000/docs |
| 스프링 서버 | 8080 | http://localhost:8080 |
| PostgreSQL | 5432 | localhost:5432 |

---

## 🛑 서비스 중지 및 재시작

### 전체 중지
```bash
cd C:\FlowPlan
docker-compose down
```

### 전체 재시작
```bash
docker-compose restart
```

### 특정 서비스만 재시작
```bash
docker-compose restart flowplan-ai
docker-compose restart flowplan-server
```

### 완전 정리 (DB 데이터 삭제)
```bash
docker-compose down -v
```

---

## ⚠️ 문제 해결

### "Gemini API 키가 유효하지 않습니다"
→ `.env` 파일의 `GEMINI_API_KEY` 확인

### "포트가 이미 사용 중입니다"
→ 8000, 8080, 5432 포트 사용 중인 프로그램 종료

### "Docker 빌드가 너무 느립니다"
→ 정상입니다. 첫 빌드는 5-10분 소요

### "Git 클론 시 인증 실패"
```bash
# HTTPS 클론 시 Personal Access Token 사용
git clone https://<TOKEN>@github.com/your-org/FlowPlanServer.git FlowPlanServer
```

---

## 📚 더 알아보기

| 문서 | 내용 |
|------|------|
| `README.md` | 프로젝트 전체 개요 및 아키텍처 |
| `.github/copilot-instructions.md` | 전체 개발 규칙 및 가이드 |
| `FlowPlanAI/.github/copilot-instructions.md` | AI 서비스 개발 가이드 |
| `FlowPlanServer/.github/copilot-instructions.md` | 스프링 서버 개발 가이드 |

---

> **개발 팁**: GitHub Copilot이 `.github/copilot-instructions.md` 파일을 자동으로 읽어 프로젝트 규칙에 맞는 코드를 제안합니다.

