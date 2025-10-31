# 멀티 스테이지 빌드 (Gradle + JDK 21)
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Gradle wrapper와 의존성 파일 먼저 복사 (캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src src

# 빌드 (테스트 제외)
RUN gradle build --no-daemon -x test

# 런타임 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 메타데이터
LABEL maintainer="FlowPlan Team"
LABEL description="FlowPlanServer - Main Business Logic Service"
LABEL version="1.0.0"

# 타임존 설정 (선택사항)
ENV TZ=Asia/Seoul

# 빌드 결과물 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

