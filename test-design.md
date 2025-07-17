# 데이터 이관 시스템 테스트 설계서

## 📋 문서 정보
- **프로젝트명**: MSSQL to MySQL 대규모 데이터 이관 시스템
- **테스트 범위**: 단위테스트, 통합테스트, 성능테스트
- **테스트 프레임워크**: JUnit 5, Mockito, Testcontainers, Spring Boot Test
- **작성일**: 2024-07-16
- **작성자**: SI 개발팀

## 🎯 1. 테스트 전략 개요

### 1.1 테스트 피라미드
```
        /\
       /  \
      / E2E \     ← 5% (End-to-End Tests)
     /______\
    /        \
   /   통합    \   ← 25% (Integration Tests)
  /____________\
 /              \
/     단위       \  ← 70% (Unit Tests)
/________________\
```

### 1.2 테스트 범위 및 비율
| 테스트 유형 | 비율 | 개수 | 목적 |
|-------------|------|------|------|
| **단위 테스트** | 70% | 140개 | 개별 컴포넌트 검증 |
| **통합 테스트** | 25% | 50개 | 컴포넌트 간 연동 검증 |
| **E2E 테스트** | 5% | 10개 | 전체 시나리오 검증 |
| **총계** | 100% | **200개** | 전체 시스템 품질 보장 |

### 1.3 테스트 환경
| 환경 | 용도 | 데이터베이스 | 컨테이너 |
|------|------|-------------|----------|
| **로컬 개발** | 단위 테스트 | H2 In-Memory | 불필요 |
| **통합 테스트** | 통합 테스트 | Testcontainers | Docker |
| **스테이징** | E2E 테스트 | 실제 DB | Kubernetes |

## 🧪 2. 단위 테스트 설계

### 2.1 단위 테스트 대상 및 전략

#### 2.1.1 Controller 단위 테스트
```java
@WebMvcTest(MigrationController.class)
class MigrationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MigrationService migrationService;
    
    @Test
    @DisplayName("전체 이관 시작 API 테스트")
    void startFullMigration_Success() throws Exception {
        // Given
        MigrationResult expectedResult = MigrationResult.builder()
            .jobExecutionId(1L)
            .status("STARTED")
            .build();
        
        when(migrationService.startFullMigration(any())).thenReturn(expectedResult);
        
        // When & Then
        mockMvc.perform(post("/api/migration/start")
                .param("chunkSize", "1000")
                .param("skipLimit", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobExecutionId").value(1L))
                .andExpect(jsonPath("$.status").value("STARTED"));
        
        verify(migrationService).startFullMigration(any());
    }
}
```

#### 2.1.2 Service 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {
    
    @Mock
    private JobLauncher jobLauncher;
    
    @Mock
    private Job dataMigrationJob;
    
    @InjectMocks
    private MigrationService migrationService;
    
    @Test
    @DisplayName("배치 Job 실행 성공 테스트")
    void startFullMigration_Success() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("startTime", System.currentTimeMillis())
            .toJobParameters();
        
        JobExecution jobExecution = new JobExecution(1L);
        jobExecution.setStatus(BatchStatus.STARTED);
        
        when(jobLauncher.run(eq(dataMigrationJob), any(JobParameters.class)))
            .thenReturn(jobExecution);
        
        // When
        MigrationResult result = migrationService.startFullMigration(
            BatchConfiguration.builder()
                .chunkSize(1000)
                .skipLimit(100)
                .build()
        );
        
        // Then
        assertThat(result.getJobExecutionId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("STARTED");
        verify(jobLauncher).run(eq(dataMigrationJob), any(JobParameters.class));
    }
}
```

#### 2.1.3 Processor 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class DataTransformProcessorTest {
    
    @Mock
    private TableMappingUtil tableMappingUtil;
    
    @Mock
    private ValueTransformUtil valueTransformUtil;
    
    @InjectMocks
    private DataTransformProcessor processor;
    
    @Test
    @DisplayName("사용자 데이터 변환 테스트")
    void process_UserData_Success() throws Exception {
        // Given
        DataRecord sourceRecord = DataRecord.builder()
            .tableName("사용자")
            .data(Map.of(
                "사용자ID", 1,
                "이름", "김철수",
                "성별", "남성"
            ))
            .build();
        
        when(tableMappingUtil.getTargetTableName("사용자")).thenReturn("users");
        when(valueTransformUtil.transformValue("성별", "남성")).thenReturn("MALE");
        
        // When
        DataRecord result = processor.process(sourceRecord);
        
        // Then
        assertThat(result.getTableName()).isEqualTo("users");
        assertThat(result.getData().get("user_id")).isEqualTo(1);
        assertThat(result.getData().get("name")).isEqualTo("김철수");
        assertThat(result.getData().get("gender")).isEqualTo("MALE");
        assertThat(result.getData().get("migrated_at")).isNotNull();
    }
    
    @Test
    @DisplayName("잘못된 데이터 처리 시 예외 발생 테스트")
    void process_InvalidData_ThrowsException() {
        // Given
        DataRecord invalidRecord = DataRecord.builder()
            .tableName("존재하지않는테이블")
            .data(Map.of("invalid", "data"))
            .build();
        
        when(tableMappingUtil.getTargetTableName("존재하지않는테이블"))
            .thenThrow(new DataTransformException("Unknown table"));
        
        // When & Then
        assertThatThrownBy(() -> processor.process(invalidRecord))
            .isInstanceOf(DataTransformException.class)
            .hasMessageContaining("Unknown table");
    }
}
```

### 2.2 단위 테스트 목록

#### 2.2.1 Controller 테스트 (12개)
| 테스트 클래스 | 테스트 메서드 수 | 주요 테스트 시나리오 |
|---------------|------------------|---------------------|
| MigrationControllerTest | 8개 | API 요청/응답, 파라미터 검증, 예외 처리 |
| MonitoringControllerTest | 6개 | 상태 조회, 진행률 조회, 메트릭 조회 |
| ConfigControllerTest | 4개 | 설정 조회/변경, 검증 |
| HealthControllerTest | 3개 | 헬스체크, 시스템 상태 |

#### 2.2.2 Service 테스트 (18개)
| 테스트 클래스 | 테스트 메서드 수 | 주요 테스트 시나리오 |
|---------------|------------------|---------------------|
| MigrationServiceTest | 10개 | Job 실행, 중지, 재시작, 상태 관리 |
| MonitoringServiceTest | 6개 | 진행률 계산, 성능 메트릭 수집 |
| ConfigServiceTest | 4개 | 동적 설정 변경, 검증 |
| ValidationServiceTest | 8개 | 데이터 검증, 무결성 체크 |
| TableMappingServiceTest | 5개 | 테이블 매핑, 컬럼 매핑 |
| NotificationServiceTest | 3개 | 알림 발송, 템플릿 처리 |

#### 2.2.3 Batch 컴포넌트 테스트 (30개)
| 테스트 클래스 | 테스트 메서드 수 | 주요 테스트 시나리오 |
|---------------|------------------|---------------------|
| DataTransformProcessorTest | 12개 | 데이터 변환, 매핑, 검증, 예외 처리 |
| DatabaseItemReaderTest | 8개 | 데이터 읽기, 커서 처리, 페이징 |
| DatabaseItemWriterTest | 6개 | 배치 쓰기, 트랜잭션, 오류 처리 |
| MigrationStepListenerTest | 4개 | Step 이벤트 처리, 통계 수집 |

#### 2.2.4 Utility 테스트 (20개)
| 테스트 클래스 | 테스트 메서드 수 | 주요 테스트 시나리오 |
|---------------|------------------|---------------------|
| TableMappingUtilTest | 8개 | 테이블명 변환, 매핑 규칙 |
| ColumnMappingUtilTest | 6개 | 컬럼명 변환, 타입 매핑 |
| ValueTransformUtilTest | 10개 | 값 변환, 패턴 매칭 |
| SqlBuilderTest | 6개 | 동적 SQL 생성, 조건절 처리 |
| DateTimeUtilTest | 4개 | 날짜 변환, 포맷팅 |
| StringUtilTest | 6개 | 문자열 처리, 인코딩 |

## 🔗 3. 통합 테스트 설계

### 3.1 통합 테스트 전략

#### 3.1.1 데이터베이스 통합 테스트
```java
@SpringBootTest
@Testcontainers
class DatabaseIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .withPassword("Test123!")
            .withEnv("ACCEPT_EULA", "Y");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("전체 데이터 이관 통합 테스트")
    void fullMigrationIntegrationTest() {
        // Given: 소스 데이터 준비
        insertTestDataToMSSql();
        
        // When: 이관 실행
        ResponseEntity<MigrationResult> response = restTemplate.postForEntity(
            "/api/migration/start?chunkSize=100", 
            null, 
            MigrationResult.class
        );
        
        // Then: 결과 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo("COMPLETED");
        
        // 데이터 정합성 검증
        verifyDataIntegrity();
    }
    
    private void verifyDataIntegrity() {
        // 소스와 타겟 레코드 수 비교
        int sourceCount = getSourceRecordCount();
        int targetCount = getTargetRecordCount();
        assertThat(targetCount).isEqualTo(sourceCount);
        
        // 데이터 변환 검증
        verifyDataTransformation();
    }
}
```

#### 3.1.2 배치 Job 통합 테스트
```java
@SpringBatchTest
@SpringBootTest
class BatchJobIntegrationTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @Test
    @DisplayName("사용자 이관 Job 통합 테스트")
    void userMigrationJobTest() throws Exception {
        // Given
        jobRepositoryTestUtils.removeJobExecutions();
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("tableName", "사용자")
            .addLong("time", System.currentTimeMillis())
            .toJobParameters();
        
        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions()).hasSize(1);
        
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isGreaterThan(0);
        assertThat(stepExecution.getWriteCount()).isEqualTo(stepExecution.getReadCount());
        assertThat(stepExecution.getSkipCount()).isEqualTo(0);
    }
}
```

### 3.2 통합 테스트 목록

#### 3.2.1 API 통합 테스트 (15개)
| 테스트 클래스 | 테스트 메서드 수 | 테스트 시나리오 |
|---------------|------------------|-----------------|
| MigrationApiIntegrationTest | 8개 | 전체 API 플로우, 인증, 권한 |
| MonitoringApiIntegrationTest | 4개 | 실시간 모니터링, WebSocket |
| ConfigApiIntegrationTest | 3개 | 설정 변경 후 동작 확인 |

#### 3.2.2 배치 통합 테스트 (20개)
| 테스트 클래스 | 테스트 메서드 수 | 테스트 시나리오 |
|---------------|------------------|-----------------|
| FullMigrationJobIntegrationTest | 5개 | 전체 이관 Job 실행 |
| DomainMigrationJobIntegrationTest | 10개 | 도메인별 Job 실행 (10개 도메인) |
| ErrorHandlingIntegrationTest | 3개 | 오류 발생 시 복구 테스트 |
| PerformanceIntegrationTest | 2개 | 대용량 데이터 처리 테스트 |

#### 3.2.3 데이터베이스 통합 테스트 (15개)
| 테스트 클래스 | 테스트 메서드 수 | 테스트 시나리오 |
|---------------|------------------|-----------------|
| DatabaseConnectionIntegrationTest | 4개 | 다중 DB 연결, 트랜잭션 |
| DataIntegrityIntegrationTest | 6개 | 데이터 정합성, 외래키 검증 |
| PerformanceIntegrationTest | 3개 | 쿼리 성능, 인덱스 효과 |
| ConcurrencyIntegrationTest | 2개 | 동시 실행, 락 처리 |

## 🚀 4. 성능 테스트 설계

### 4.1 성능 테스트 시나리오

#### 4.1.1 대용량 데이터 처리 테스트
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class PerformanceTest {
    
    @Test
    @Order(1)
    @DisplayName("100만 건 데이터 이관 성능 테스트")
    void performanceTest_1Million_Records() {
        // Given: 100만 건 테스트 데이터 생성
        generateTestData(1_000_000);
        
        // When: 이관 실행 및 시간 측정
        long startTime = System.currentTimeMillis();
        MigrationResult result = migrationService.startFullMigration(
            BatchConfiguration.builder()
                .chunkSize(5000)
                .skipLimit(1000)
                .build()
        );
        long endTime = System.currentTimeMillis();
        
        // Then: 성능 기준 검증
        long duration = endTime - startTime;
        long recordsPerSecond = 1_000_000 / (duration / 1000);
        
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(recordsPerSecond).isGreaterThan(1000); // 초당 1000건 이상
        assertThat(duration).isLessThan(900_000); // 15분 이내
    }
    
    @Test
    @Order(2)
    @DisplayName("동시 실행 성능 테스트")
    void concurrentMigrationTest() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        List<Future<MigrationResult>> futures = new ArrayList<>();
        
        // 5개 도메인 동시 실행
        String[] domains = {"USER", "PRODUCT", "ORDER", "PAYMENT", "INVENTORY"};
        
        for (String domain : domains) {
            Future<MigrationResult> future = executor.submit(() -> {
                try {
                    return migrationService.startDomainMigration(domain);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }
        
        // 모든 작업 완료 대기 (최대 30분)
        boolean completed = latch.await(30, TimeUnit.MINUTES);
        assertThat(completed).isTrue();
        
        // 모든 작업 성공 확인
        for (Future<MigrationResult> future : futures) {
            MigrationResult result = future.get();
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
        }
    }
}
```

### 4.2 성능 기준

| 테스트 유형 | 데이터량 | 목표 성능 | 허용 시간 |
|-------------|----------|-----------|-----------|
| **소량 데이터** | 1만 건 | 초당 5,000건 | 2초 이내 |
| **중량 데이터** | 10만 건 | 초당 2,000건 | 50초 이내 |
| **대량 데이터** | 100만 건 | 초당 1,000건 | 15분 이내 |
| **초대량 데이터** | 1,000만 건 | 초당 800건 | 3시간 이내 |

## 📊 5. 테스트 자동화 및 CI/CD

### 5.1 테스트 실행 전략
```yaml
# GitHub Actions 워크플로우
name: Test Pipeline

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Unit Tests
        run: ./gradlew test --tests "*Test"
      
  integration-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Integration Tests
        run: ./gradlew test --tests "*IntegrationTest"
        
  performance-tests:
    runs-on: ubuntu-latest
    needs: integration-tests
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - name: Run Performance Tests
        run: ./gradlew test --tests "*PerformanceTest"
```

### 5.2 테스트 커버리지 목표
| 구분 | 목표 커버리지 | 측정 도구 |
|------|---------------|-----------|
| **라인 커버리지** | 85% 이상 | JaCoCo |
| **브랜치 커버리지** | 80% 이상 | JaCoCo |
| **메서드 커버리지** | 90% 이상 | JaCoCo |

## 🔧 6. 테스트 도구 및 설정

### 6.1 Gradle 테스트 설정
```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.batch:spring-batch-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mysql'
    testImplementation 'org.testcontainers:mssqlserver'
    testImplementation 'com.h2database:h2'
}

test {
    useJUnitPlatform()
    testLogging {
        events "passed", "skipped", "failed"
    }
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

### 6.2 테스트 프로파일 설정
```yaml
# application-test.yml
spring:
  datasource:
    source:
      url: jdbc:h2:mem:sourcedb
      driver-class-name: org.h2.Driver
    target:
      url: jdbc:h2:mem:targetdb
      driver-class-name: org.h2.Driver
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: always

logging:
  level:
    org.springframework.batch: DEBUG
    com.example.batch: DEBUG
```

## 🎯 7. 테스트 실행 가이드

### 7.1 로컬 개발 환경 테스트
```bash
# 1. 단위 테스트만 실행
./gradlew test --tests "*Test" --exclude-task integrationTest

# 2. 특정 클래스 테스트
./gradlew test --tests "MigrationControllerTest"

# 3. 특정 메서드 테스트
./gradlew test --tests "MigrationControllerTest.startFullMigration_Success"

# 4. 패키지별 테스트
./gradlew test --tests "com.example.batch.controller.*"

# 5. 테스트 커버리지 리포트 생성
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### 7.2 통합 테스트 실행
```bash
# 1. Docker 환경 준비
docker-compose -f docker-compose-test.yml up -d

# 2. 통합 테스트 실행
./gradlew integrationTest

# 3. 특정 통합 테스트 실행
./gradlew test --tests "*IntegrationTest"

# 4. 성능 테스트 실행 (별도)
./gradlew test --tests "*PerformanceTest" -Dspring.profiles.active=performance

# 5. 테스트 환경 정리
docker-compose -f docker-compose-test.yml down -v
```

### 7.3 CI/CD 파이프라인 테스트
```bash
# 1. 전체 테스트 파이프라인 실행
./gradlew clean build

# 2. 단계별 실행
./gradlew clean
./gradlew compileJava
./gradlew test
./gradlew integrationTest
./gradlew build

# 3. 테스트 결과 확인
ls -la build/reports/tests/
ls -la build/reports/jacoco/
```

## 📋 8. 테스트 체크리스트

### 8.1 개발자 체크리스트
#### 코드 작성 시
- [ ] 새로운 기능에 대한 단위 테스트 작성
- [ ] 기존 테스트가 모두 통과하는지 확인
- [ ] 테스트 커버리지 80% 이상 유지
- [ ] 테스트 메서드명이 명확하고 이해하기 쉬운지 확인
- [ ] Given-When-Then 패턴으로 테스트 구조화

#### Pull Request 전
- [ ] 모든 단위 테스트 통과
- [ ] 관련 통합 테스트 통과
- [ ] 코드 커버리지 기준 충족
- [ ] 테스트 실행 시간이 합리적인 범위 내
- [ ] 테스트 코드 리뷰 완료

### 8.2 QA 체크리스트
#### 통합 테스트 검증
- [ ] 모든 API 엔드포인트 정상 동작
- [ ] 데이터베이스 연동 정상 동작
- [ ] 배치 Job 실행 및 완료 확인
- [ ] 오류 처리 및 복구 메커니즘 동작
- [ ] 성능 기준 충족 확인

#### E2E 테스트 검증
- [ ] 전체 이관 프로세스 정상 완료
- [ ] 10개 도메인 모두 정상 이관
- [ ] 데이터 정합성 100% 일치
- [ ] 모니터링 및 알림 정상 동작
- [ ] 장애 상황 복구 테스트 통과

### 8.3 운영 배포 전 체크리스트
#### 성능 테스트
- [ ] 대용량 데이터 처리 성능 기준 충족
- [ ] 동시 실행 시나리오 정상 동작
- [ ] 메모리 사용량 임계치 내 유지
- [ ] 데이터베이스 연결 풀 최적화 확인
- [ ] 장시간 실행 시 안정성 확인

#### 보안 테스트
- [ ] API 인증 및 권한 검증
- [ ] SQL Injection 방어 확인
- [ ] 민감 데이터 암호화 확인
- [ ] 로그에 민감 정보 노출 없음
- [ ] 네트워크 보안 설정 확인

## 📊 9. 테스트 메트릭 및 리포팅

### 9.1 테스트 실행 결과 예시
```
===============================================
Test Results Summary
===============================================
Total Tests: 200
Passed: 195
Failed: 3
Skipped: 2
Success Rate: 97.5%

Coverage Summary:
- Line Coverage: 87.3%
- Branch Coverage: 82.1%
- Method Coverage: 91.5%

Performance Test Results:
- 100만 건 처리 시간: 12분 30초
- 처리 속도: 1,333 records/sec
- 메모리 사용량: 최대 6.2GB
===============================================
```

### 9.2 테스트 품질 지표
| 지표 | 목표값 | 현재값 | 상태 |
|------|--------|--------|------|
| **테스트 통과율** | 95% 이상 | 97.5% | ✅ 양호 |
| **코드 커버리지** | 85% 이상 | 87.3% | ✅ 양호 |
| **테스트 실행 시간** | 30분 이내 | 25분 | ✅ 양호 |
| **성능 테스트 통과** | 100% | 100% | ✅ 양호 |

## 🚨 10. 테스트 실패 시 대응 방안

### 10.1 단위 테스트 실패
1. **즉시 대응**: 개발자가 즉시 수정
2. **원인 분석**: 로그 분석 및 디버깅
3. **수정 및 재테스트**: 코드 수정 후 테스트 재실행
4. **회귀 테스트**: 관련 테스트 모두 재실행

### 10.2 통합 테스트 실패
1. **환경 점검**: 테스트 환경 상태 확인
2. **의존성 확인**: 외부 시스템 연동 상태 점검
3. **데이터 상태 확인**: 테스트 데이터 정합성 검증
4. **단계별 디버깅**: 각 컴포넌트별 개별 테스트

### 10.3 성능 테스트 실패
1. **리소스 모니터링**: CPU, 메모리, 디스크 사용량 확인
2. **쿼리 최적화**: 느린 쿼리 식별 및 최적화
3. **설정 튜닝**: 배치 크기, 스레드 풀 등 조정
4. **인프라 스케일링**: 필요시 리소스 증설

이 테스트 설계서는 대규모 데이터 이관 프로젝트의 품질을 보장하기 위한 포괄적인 테스트 전략과 실행 가이드를 제시합니다.
