# FlowPlanServer 전용 GitHub Copilot 지침

> ⚠️ **전체 프로젝트 아키텍처는 루트의 `.github/copilot-instructions.md` 참조**

## 서비스 개요
- **서비스명**: FlowPlanServer (메인 비즈니스 로직)
- **역할**: 사용자 인증/인가, 프로젝트/작업 관리, AI 서비스 통합
- **기술 스택**: Spring Boot 3.5.6, Spring Security, JPA, MySQL
- **Java 버전**: 21
- **배포**: Docker 컨테이너, FlowPlanAI와 함께 운영

## 전체 프로젝트 컨텍스트
- **상위 프로젝트**: FlowPlan (AI 기반 프로젝트 일정 관리 플랫폼)
- **다른 서비스**: FlowPlanAI (FastAPI - AI WBS 생성)
- **통신 방식**: FlowPlanServer → FlowPlanAI (HTTP 요청)
- **책임 범위**: 인증, DB, 비즈니스 로직 / AI 로직은 FlowPlanAI에 위임

---

## 코딩 규칙 및 가이드라인

### 1. 패키지 구조 (DDD 기반)
```
com.hanmo.flowplan/
├── ai/                           # AI 서비스 통합 레이어
│   ├── client/                   # FlowPlanAI 호출 클라이언트
│   ├── dto/                      # AI 서비스 요청/응답 DTO
│   └── config/                   # RestTemplate, WebClient 설정
├── global/                       # 전역 설정
│   ├── config/                   # 전역 설정
│   ├── exception/                # 전역 예외 처리
│   └── security/                 # Spring Security, JWT
├── project/                      # 프로젝트 도메인
│   ├── application/              # 서비스 레이어
│   ├── domain/                   # 엔티티, 리포지토리 인터페이스
│   ├── infrastructure/           # 리포지토리 구현
│   └── presentation/             # 컨트롤러, DTO
├── task/                         # 작업 도메인
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
└── user/                         # 사용자 도메인
    ├── application/
    ├── domain/
    ├── infrastructure/
    └── presentation/
```

### 2. 코드 스타일 (Java 기준)

#### 엔티티 (JPA)
```java
@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Task extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String taskId;  // 계층 구조 ID (1.0, 1.1, 1.1.1)
    
    @Column(length = 50)
    private String parentId;  // 상위 작업 ID
    
    @Column(nullable = false)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column(nullable = false)
    private LocalDate endDate;
    
    @Column(nullable = false)
    private Integer durationDays;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0;  // 0-100
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;
}

public enum TaskStatus {
    TODO, IN_PROGRESS, COMPLETED
}
```

#### AI 서비스 호출 클라이언트
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {
    
    @Value("${ai-service.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * AI 서비스에 WBS 생성 요청
     * 
     * @param request WBS 생성 요청 데이터
     * @return AI가 생성한 WBS 응답
     * @throws AiServiceException AI 서비스 통신 실패 시
     */
    public WbsGenerateResponse generateWbs(WbsGenerateRequest request) {
        String url = baseUrl + "/api/v1/wbs/generate";
        
        try {
            log.info("Calling AI service: {}", url);
            ResponseEntity<WbsGenerateResponse> response = restTemplate.postForEntity(
                url, 
                request, 
                WbsGenerateResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            
            throw new AiServiceException("AI 서비스 응답이 비어있습니다");
            
        } catch (RestClientException e) {
            log.error("AI 서비스 호출 실패", e);
            throw new AiServiceException("AI 서비스 호출 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * AI 서비스 헬스체크
     */
    public boolean checkHealth() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/health", 
                String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("AI 서비스 헬스체크 실패", e);
            return false;
        }
    }
}
```

#### WBS 통합 서비스
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WbsService {
    
    private final AiServiceClient aiServiceClient;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    
    /**
     * WBS 자동 생성 및 저장
     * 
     * 1. 프로젝트 정보 조회
     * 2. AI 서비스에 WBS 생성 요청
     * 3. 응답받은 WBS를 Tasks 테이블에 저장
     * 4. 저장된 WBS 반환
     */
    public WbsResponse generateAndSaveWbs(Long projectId, WbsGenerateRequest request, Long userId) {
        // 1. 프로젝트 권한 확인
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));
        
        validateProjectAccess(project, userId);
        
        // 2. AI 서비스 호출
        WbsGenerateResponse aiResponse = aiServiceClient.generateWbs(request);
        
        // 3. WBS를 Tasks 테이블에 저장
        List<Task> savedTasks = saveWbsFromAi(project, aiResponse.getWbsStructure(), userId);
        
        // 4. 응답 생성
        return WbsResponse.builder()
            .projectId(projectId)
            .projectName(aiResponse.getProjectName())
            .totalTasks(savedTasks.size())
            .tasks(savedTasks.stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList()))
            .build();
    }
    
    private List<Task> saveWbsFromAi(Project project, List<WbsTask> wbsStructure, Long userId) {
        List<Task> tasks = new ArrayList<>();
        
        for (WbsTask wbsTask : wbsStructure) {
            // assignee 문자열을 User 엔티티로 변환 (매핑 로직 필요)
            User assignee = findOrCreateUser(wbsTask.getAssignee());
            
            Task task = Task.builder()
                .taskId(wbsTask.getTaskId())
                .parentId(wbsTask.getParentId())
                .name(wbsTask.getName())
                .assignee(assignee)
                .project(project)
                .startDate(wbsTask.getStartDate())
                .endDate(wbsTask.getEndDate())
                .durationDays(wbsTask.getDurationDays())
                .progress(wbsTask.getProgress())
                .status(TaskStatus.valueOf(wbsTask.getStatus().name()))
                .build();
            
            tasks.add(taskRepository.save(task));
            
            // 재귀적으로 하위 작업 저장
            if (wbsTask.getSubtasks() != null && !wbsTask.getSubtasks().isEmpty()) {
                tasks.addAll(saveWbsFromAi(project, wbsTask.getSubtasks(), userId));
            }
        }
        
        return tasks;
    }
}
```

### 3. application.properties 설정

```properties
spring.application.name=FlowPlan

# Server
server.port=8080

# Database (MySQL)
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/flowplan}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# AI Service (중요!)
ai-service.base-url=${AI_SERVICE_BASE_URL:http://flowplan-ai:8000}
ai-service.connect-timeout=5000
ai-service.read-timeout=30000

# JWT
jwt.secret=${JWT_SECRET:your-secret-key-change-in-production}
jwt.expiration=${JWT_EXPIRATION:86400000}

# Actuator
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

### 4. RestTemplate 설정

```java
@Configuration
public class RestTemplateConfig {
    
    @Value("${ai-service.connect-timeout:5000}")
    private int connectTimeout;
    
    @Value("${ai-service.read-timeout:30000}")
    private int readTimeout;
    
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new CustomRestTemplateErrorHandler());
        
        return restTemplate;
    }
}
```

### 5. 전역 예외 처리

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiResponse<?>> handleAiServiceException(AiServiceException e) {
        log.error("AI 서비스 오류", e);
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error("AI 서비스 오류: " + e.getMessage()));
    }
    
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleProjectNotFound(ProjectNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("접근 권한이 없습니다"));
    }
}
```

---

## 주요 규칙

### 1. AI 서비스 통합
- ✅ **AiServiceClient**를 통해서만 FlowPlanAI 호출
- ✅ Docker 내부 네트워크 URL 사용: `http://flowplan-ai:8000`
- ✅ 타임아웃 설정: 연결 5초, 읽기 30초
- ✅ 에러 핸들링: `AiServiceException`으로 래핑
- ❌ 컨트롤러에서 직접 HTTP 호출 금지

### 2. Tasks 엔티티 매핑
- ✅ `assignee` (String from AI) → `assignee_id` (FK to User)
- ✅ `project_name` → `project_id` (FK to Project)
- ✅ `task_id`, `parent_id` 계층 구조 유지
- ✅ `status` Enum: TODO, IN_PROGRESS, COMPLETED
- ✅ `progress`: 0-100 정수

### 3. API 응답 형식 (표준화)
```java
@Getter
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .data(null)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

### 4. 로깅
```java
@Slf4j
public class SomeService {
    // INFO: 정상 흐름
    log.info("사용자 {}가 프로젝트 {} WBS 생성 요청", userId, projectId);
    
    // WARN: 경고 (복구 가능)
    log.warn("AI 서비스 응답 지연: {}ms", duration);
    
    // ERROR: 에러 (복구 불가)
    log.error("AI 서비스 호출 실패", e);
}
```

---

## Docker 및 배포

### Dockerfile
```dockerfile
# 멀티 스테이지 빌드
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon -x test

# 런타임 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 금지 사항
- ❌ **AI 로직 구현** (Gemini API 직접 호출 등)
- ❌ **컨트롤러에서 직접 HTTP 호출**
- ❌ **FlowPlanAI 응답 구조 임의 변경**
- ❌ **AI 서비스 외부 URL 사용** (Docker 내부만)
- ❌ **하드코딩된 비밀번호, API 키**

---

## 참고 정보
- **전체 아키텍처**: 루트 `../.github/copilot-instructions.md` 참조
- **AI 서비스 응답 구조**: FlowPlanAI의 `response.py` 참조
- **Docker 네트워크**: 서비스명으로 통신 (`http://flowplan-ai:8000`)

---

> **Copilot 사용 팁**: 
> - AI 로직은 절대 구현하지 않고 FlowPlanAI에 HTTP 요청
> - Tasks 엔티티는 FlowPlanAI 응답 구조와 호환 유지
> - Docker 환경 변수 (`AI_SERVICE_BASE_URL`) 항상 고려

