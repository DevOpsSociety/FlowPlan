# FlowPlanServer - Spring Boot Service

> AI 기반 프로젝트 일정 관리 플랫폼의 메인 비즈니스 로직 서버

## 서비스 개요
- **역할**: 사용자 인증/인가, 프로젝트/작업 관리, AI 서비스 통합
- **기술 스택**: Spring Boot 3.5.6, JPA, MySQL, Spring Security, OAuth2
- **Java 버전**: 21

## 프로젝트 구조

```
src/main/java/com/hanmo/flowplan/
├── FlowPlanApplication.java        # 메인 애플리케이션
├── ai/                             # AI 서비스 통합
│   ├── client/
│   │   └── AiServiceClient.java   # FlowPlanAI 호출 클라이언트
│   ├── dto/
│   │   ├── WbsGenerateRequest.java
│   │   └── WbsGenerateResponse.java
│   └── config/
│       └── RestTemplateConfig.java
├── global/                         # 공통 설정
│   ├── config/
│   ├── exception/
│   └── security/
├── project/                        # 프로젝트 도메인
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
├── task/                           # 작업 도메인
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
└── user/                           # 사용자 도메인
    ├── application/
    ├── domain/
    ├── infrastructure/
    └── presentation/
```

## 빌드 및 실행

### 로컬 개발
```bash
# Gradle 빌드
./gradlew build

# 로컬 실행
./gradlew bootRun

# 테스트
./gradlew test
```

### Docker 실행
```bash
# FlowPlan 루트에서
cd C:\FlowPlan

# docker-compose.yml에서 flowplan-server 주석 해제 후
docker-compose up -d flowplan-server
```

## 환경 변수 설정

### application.yml 필수 설정
- `AI_SERVICE_BASE_URL`: FlowPlanAI 서비스 URL (Docker: http://flowplan-ai:8000)
- `SPRING_DATASOURCE_URL`: 데이터베이스 URL
- `JWT_SECRET`: JWT 시크릿 키

## 주요 기능
- ✅ 사용자 인증/인가 (Spring Security + OAuth2)
- ✅ 프로젝트 CRUD
- ✅ 작업 관리
- ⏳ AI 서비스 통합 (FlowPlanAI 호출)
- ⏳ WBS 자동 생성 및 저장

## 참고
- 전체 아키텍처: `../.github/copilot-instructions.md`
- AI 서비스: `../FlowPlanAI/README.md`
- 설정 가이드: `../SETUP.md`

