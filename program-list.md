# 데이터 이관 시스템 프로그램 목록

## 📋 문서 정보
- **프로젝트명**: MSSQL to MariaDB 데이터 이관 시스템
- **시스템명**: BatchMigrationSystem
- **문서 유형**: 프로그램 목록 (Program List)
- **작성일**: 2024-07-16
- **작성자**: SI 개발팀
- **검토자**: 프로젝트 매니저

## 🎯 1. 시스템 개요

### 1.1 시스템 구성
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.3.5, Spring Batch 5.x
- **빌드 도구**: Gradle 8.x
- **패키지 구조**: `com.example.batch`

### 1.2 주요 기능
- MSSQL에서 MariaDB로 대용량 데이터 이관
- 한글 테이블명/컬럼명을 영어로 변환
- REST API를 통한 배치 작업 제어
- 실시간 모니터링 및 오류 복구

### 1.3 네이밍 규칙 (Naming Convention)

#### 1.3.1 Java 클래스 네이밍
- **Controller**: `{기능명}Controller.java` (예: MigrationController.java)
- **Service**: `{기능명}Service.java` (예: MigrationService.java)
- **Job Config**: `{테이블명}MigrationJobConfig.java` (예: UserMigrationJobConfig.java)
- **Step Config**: `{테이블명}MigrationStepConfig.java` (예: UserMigrationStepConfig.java)
- **Reader**: `{타입}ItemReader.java` (예: DatabaseItemReader.java)
- **Processor**: `{기능명}Processor.java` (예: DataTransformProcessor.java)
- **Writer**: `{타입}ItemWriter.java` (예: DatabaseItemWriter.java)
- **Listener**: `{기능명}Listener.java` (예: MigrationStepListener.java)
- **Exception**: `{기능명}Exception.java` (예: MigrationException.java)
- **Util**: `{기능명}Util.java` (예: TableMappingUtil.java)

#### 1.3.2 패키지 네이밍
- **기본 패키지**: `com.example.batch`
- **하위 패키지**: 소문자, 단수형 사용 (예: controller, service, job, step)
- **테스트 패키지**: 동일한 구조로 `src/test/java` 하위에 구성

#### 1.3.3 메서드 네이밍
- **API 메서드**: `{동사}{명사}` (예: startMigration, getMigrationStatus)
- **Service 메서드**: `{동사}{명사}` (예: executeMigrationJob, validateTableData)
- **Batch 메서드**: `{테이블명}MigrationStep` (예: userMigrationStep, productMigrationStep)

#### 1.3.4 변수 네이밍
- **상수**: `UPPER_SNAKE_CASE` (예: DEFAULT_CHUNK_SIZE, MAX_RETRY_COUNT)
- **변수**: `camelCase` (예: chunkSize, skipLimit, tableName)
- **Bean 이름**: `camelCase` (예: migrationService, dataTransformProcessor)

## 📁 2. 패키지 구조 및 프로그램 목록

### 2.1 전체 패키지 구조
```
com.example.batch/
├── BatchMigrationApplication.java          # 메인 애플리케이션
├── config/                                 # 설정 클래스
├── controller/                             # REST API 컨트롤러
├── service/                                # 비즈니스 서비스
├── job/                                    # 배치 Job 설정
├── step/                                   # 배치 Step 설정
├── reader/                                 # ItemReader 구현
├── processor/                              # ItemProcessor 구현
├── writer/                                 # ItemWriter 구현
├── listener/                               # 배치 리스너
├── model/                                  # 데이터 모델
├── exception/                              # 예외 클래스
└── util/                                   # 유틸리티 클래스
```

## 📋 3. 상세 프로그램 목록

### 3.1 메인 애플리케이션

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| MAIN-001 | 메인 애플리케이션 | BatchMigrationApplication.java | Spring Boot 애플리케이션 시작점 | @SpringBootApplication |

### 3.2 설정 클래스 (config 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| CONF-001 | 데이터소스 설정 | DataSourceConfig.java | MSSQL, MariaDB 데이터소스 설정 | @Configuration |
| CONF-002 | 배치 설정 | BatchConfig.java | Spring Batch 기본 설정 | @EnableBatchProcessing |
| CONF-003 | 트랜잭션 설정 | TransactionConfig.java | 트랜잭션 매니저 설정 | @Configuration |
| CONF-004 | 스케줄러 설정 | SchedulerConfig.java | 배치 스케줄링 설정 | @EnableScheduling |

### 3.3 REST API 컨트롤러 (controller 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 주요 API 엔드포인트 | 기능 | 비고 |
|-----------|-----------|--------|-------------------|------|------|
| CTRL-001 | 이관 컨트롤러 | MigrationController.java | `/api/migration/start`<br/>`/api/migration/table/{tableName}`<br/>`/api/migration/stop/{jobId}` | 배치 실행 제어 API | @RestController |
| CTRL-002 | 모니터링 컨트롤러 | MonitoringController.java | `/api/monitoring/status/{jobId}`<br/>`/api/monitoring/progress`<br/>`/api/monitoring/metrics` | 배치 상태 조회 API | @RestController |
| CTRL-003 | 설정 컨트롤러 | ConfigController.java | `/api/config/batch`<br/>`/api/config/datasource`<br/>`/api/config/mapping` | 배치 설정 관리 API | @RestController |
| CTRL-004 | 헬스체크 컨트롤러 | HealthController.java | `/api/health/database`<br/>`/api/health/batch`<br/>`/api/health/system` | 시스템 상태 확인 API | @RestController |

### 3.4 비즈니스 서비스 (service 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 주요 메서드 | 기능 | 비고 |
|-----------|-----------|--------|-------------|------|------|
| SERV-001 | 이관 서비스 | MigrationService.java | `startFullMigration()`<br/>`startTableMigration(String tableName)`<br/>`stopMigration(Long jobId)` | 배치 실행 비즈니스 로직 | @Service |
| SERV-002 | 모니터링 서비스 | MonitoringService.java | `getJobStatus(Long jobId)`<br/>`getMigrationProgress()`<br/>`getPerformanceMetrics()` | 배치 상태 모니터링 | @Service |
| SERV-003 | 설정 서비스 | ConfigService.java | `updateBatchConfig(BatchConfig config)`<br/>`getDatasourceConfig()`<br/>`updateMappingRules()` | 동적 설정 관리 | @Service |
| SERV-004 | 검증 서비스 | ValidationService.java | `validateTableData(String tableName)`<br/>`validateMigrationResult()`<br/>`checkDataIntegrity()` | 데이터 검증 로직 | @Service |
| SERV-005 | 테이블 매핑 서비스 | TableMappingService.java | `getTableMapping(String koreanName)`<br/>`getColumnMapping(String tableName)`<br/>`getValueMapping(String column)` | 한글-영어 매핑 관리 | @Service |
| SERV-006 | 알림 서비스 | NotificationService.java | `sendJobStartNotification()`<br/>`sendJobCompleteNotification()`<br/>`sendErrorNotification()` | 배치 실행 알림 | @Service |

### 3.5 배치 Job 설정 (job 패키지)

| 프로그램ID | 프로그램명 | 파일명 | Job Bean 이름 | 기능 | 비고 |
|-----------|-----------|--------|---------------|------|------|
| JOB-001 | 전체 이관 Job | DataMigrationJobConfig.java | `dataMigrationJob` | 전체 테이블 순차 이관 Job | @Configuration |
| JOB-002 | 단일 테이블 Job | SingleTableJobConfig.java | `singleTableMigrationJob` | 개별 테이블 이관 Job | @Configuration |
| JOB-003 | 사용자 이관 Job | UserMigrationJobConfig.java | `userMigrationJob` | 사용자 테이블 전용 Job | @Configuration |
| JOB-004 | 상품 이관 Job | ProductMigrationJobConfig.java | `productMigrationJob` | 상품 테이블 전용 Job | @Configuration |
| JOB-005 | 주문 이관 Job | OrderMigrationJobConfig.java | `orderMigrationJob` | 주문 테이블 전용 Job | @Configuration |
| JOB-006 | Job 팩토리 | JobFactory.java | - | 동적 Job 생성 팩토리 | @Component |
| JOB-007 | Job 파라미터 빌더 | JobParameterBuilder.java | - | Job 파라미터 빌더 | @Component |
| JOB-008 | Job 실행 매니저 | JobExecutionManager.java | - | Job 실행 상태 관리 | @Component |

### 3.6 배치 Step 설정 (step 패키지)

| 프로그램ID | 프로그램명 | 파일명 | Step Bean 이름 | 처리 테이블 | 기능 | 비고 |
|-----------|-----------|--------|----------------|-------------|------|------|
| STEP-001 | 사용자 Step | UserMigrationStepConfig.java | `userMigrationStep` | 사용자 → users | 사용자 테이블 이관 Step | @Configuration |
| STEP-002 | 카테고리 Step | CategoryMigrationStepConfig.java | `categoryMigrationStep` | 카테고리 → categories | 카테고리 테이블 이관 Step | @Configuration |
| STEP-003 | 상품 Step | ProductMigrationStepConfig.java | `productMigrationStep` | 상품 → products | 상품 테이블 이관 Step | @Configuration |
| STEP-004 | 주문 Step | OrderMigrationStepConfig.java | `orderMigrationStep` | 주문 → orders | 주문 테이블 이관 Step | @Configuration |
| STEP-005 | 주문상세 Step | OrderDetailMigrationStepConfig.java | `orderDetailMigrationStep` | 주문상세 → order_details | 주문상세 테이블 이관 Step | @Configuration |
| STEP-006 | 리뷰 Step | ReviewMigrationStepConfig.java | `reviewMigrationStep` | 리뷰 → reviews | 리뷰 테이블 이관 Step | @Configuration |
| STEP-007 | 공지사항 Step | NoticeMigrationStepConfig.java | `noticeMigrationStep` | 공지사항 → notices | 공지사항 테이블 이관 Step | @Configuration |
| STEP-008 | 쿠폰 Step | CouponMigrationStepConfig.java | `couponMigrationStep` | 쿠폰 → coupons | 쿠폰 테이블 이관 Step | @Configuration |
| STEP-009 | 배송 Step | DeliveryMigrationStepConfig.java | `deliveryMigrationStep` | 배송 → deliveries | 배송 테이블 이관 Step | @Configuration |
| STEP-010 | 문의 Step | InquiryMigrationStepConfig.java | `inquiryMigrationStep` | 문의 → inquiries | 문의 테이블 이관 Step | @Configuration |
| STEP-011 | 공통 Step | CommonStepConfig.java | - | - | 공통 Step 설정 및 유틸 | @Configuration |
| STEP-012 | Step 팩토리 | StepFactory.java | - | - | 동적 Step 생성 팩토리 | @Component |

### 3.7 ItemReader 구현 (reader 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| READ-001 | DB 아이템 리더 | DatabaseItemReader.java | MSSQL 데이터 읽기 | @Component |
| READ-002 | 커서 리더 팩토리 | CursorReaderFactory.java | 커서 기반 리더 생성 | @Component |
| READ-003 | 페이징 리더 팩토리 | PagingReaderFactory.java | 페이징 기반 리더 생성 | @Component |
| READ-004 | 로우 매퍼 | DataRecordRowMapper.java | ResultSet을 DataRecord로 매핑 | @Component |

### 3.8 ItemProcessor 구현 (processor 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| PROC-001 | 데이터 변환 프로세서 | DataTransformProcessor.java | 한글→영어 데이터 변환 | @Component |
| PROC-002 | 검증 프로세서 | ValidationProcessor.java | 데이터 검증 처리 | @Component |
| PROC-003 | 타입 변환 프로세서 | TypeConversionProcessor.java | 데이터 타입 변환 | @Component |
| PROC-004 | 복합 프로세서 | CompositeProcessor.java | 여러 프로세서 조합 | @Component |

### 3.9 ItemWriter 구현 (writer 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| WRIT-001 | DB 아이템 라이터 | DatabaseItemWriter.java | MariaDB 데이터 쓰기 | @Component |
| WRIT-002 | 배치 라이터 | BatchInsertWriter.java | 배치 INSERT 처리 | @Component |
| WRIT-003 | 복합 라이터 | CompositeWriter.java | 여러 라이터 조합 | @Component |
| WRIT-004 | 오류 처리 라이터 | ErrorHandlingWriter.java | 쓰기 오류 처리 | @Component |

### 3.10 배치 리스너 (listener 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| LIST-001 | Job 실행 리스너 | JobExecutionListener.java | Job 실행 이벤트 처리 | @Component |
| LIST-002 | Step 실행 리스너 | MigrationStepListener.java | Step 실행 이벤트 처리 | @Component |
| LIST-003 | 청크 리스너 | ChunkListener.java | 청크 처리 이벤트 | @Component |
| LIST-004 | 스킵 리스너 | SkipListener.java | 스킵 이벤트 처리 | @Component |
| LIST-005 | 재시도 리스너 | RetryListener.java | 재시도 이벤트 처리 | @Component |

### 3.11 데이터 모델 (model 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| MODL-001 | 데이터 레코드 | DataRecord.java | 이관 데이터 모델 | POJO |
| MODL-002 | 배치 설정 | BatchConfiguration.java | 배치 설정 모델 | @ConfigurationProperties |
| MODL-003 | 이관 결과 | MigrationResult.java | 이관 결과 모델 | POJO |
| MODL-004 | 테이블 메타데이터 | TableMetadata.java | 테이블 정보 모델 | POJO |
| MODL-005 | 처리 통계 | ProcessingStats.java | 처리 통계 모델 | POJO |

### 3.12 예외 클래스 (exception 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| EXCP-001 | 이관 예외 | MigrationException.java | 이관 관련 예외 | extends RuntimeException |
| EXCP-002 | 데이터 검증 예외 | DataValidationException.java | 데이터 검증 예외 | extends MigrationException |
| EXCP-003 | 변환 예외 | DataTransformException.java | 데이터 변환 예외 | extends MigrationException |
| EXCP-004 | 설정 예외 | ConfigurationException.java | 설정 관련 예외 | extends MigrationException |
| EXCP-005 | 전역 예외 핸들러 | GlobalExceptionHandler.java | 전역 예외 처리 | @ControllerAdvice |

### 3.13 유틸리티 클래스 (util 패키지)

| 프로그램ID | 프로그램명 | 파일명 | 기능 | 비고 |
|-----------|-----------|--------|------|------|
| UTIL-001 | 테이블 매핑 유틸 | TableMappingUtil.java | 테이블명 매핑 유틸리티 | static methods |
| UTIL-002 | 컬럼 매핑 유틸 | ColumnMappingUtil.java | 컬럼명 매핑 유틸리티 | static methods |
| UTIL-003 | 값 변환 유틸 | ValueTransformUtil.java | 값 변환 유틸리티 | static methods |
| UTIL-004 | SQL 빌더 | SqlBuilder.java | 동적 SQL 생성 | @Component |
| UTIL-005 | 날짜 유틸 | DateTimeUtil.java | 날짜/시간 처리 유틸 | static methods |
| UTIL-006 | 문자열 유틸 | StringUtil.java | 문자열 처리 유틸 | static methods |

## 🌐 4. API 엔드포인트 상세 목록

### 4.1 MigrationController API

| HTTP Method | 엔드포인트 | 메서드명 | 기능 | 파라미터 |
|-------------|-----------|----------|------|----------|
| POST | `/api/migration/start` | `startFullMigration()` | 전체 테이블 이관 시작 | chunkSize, skipLimit |
| POST | `/api/migration/table/{tableName}` | `startTableMigration()` | 특정 테이블 이관 시작 | tableName, whereClause, chunkSize |
| POST | `/api/migration/stop/{jobId}` | `stopMigration()` | 실행 중인 배치 중지 | jobId |
| POST | `/api/migration/restart/{jobId}` | `restartMigration()` | 실패한 배치 재시작 | jobId, fromStep |
| GET | `/api/migration/jobs` | `getAllJobs()` | 모든 Job 목록 조회 | page, size |

### 4.2 MonitoringController API

| HTTP Method | 엔드포인트 | 메서드명 | 기능 | 파라미터 |
|-------------|-----------|----------|------|----------|
| GET | `/api/monitoring/status/{jobId}` | `getJobStatus()` | 특정 Job 상태 조회 | jobId |
| GET | `/api/monitoring/progress` | `getMigrationProgress()` | 전체 이관 진행률 조회 | - |
| GET | `/api/monitoring/metrics` | `getPerformanceMetrics()` | 성능 메트릭 조회 | startTime, endTime |
| GET | `/api/monitoring/logs/{jobId}` | `getJobLogs()` | Job 실행 로그 조회 | jobId, level |
| GET | `/api/monitoring/errors` | `getErrorSummary()` | 오류 요약 정보 조회 | date, severity |

### 4.3 ConfigController API

| HTTP Method | 엔드포인트 | 메서드명 | 기능 | 파라미터 |
|-------------|-----------|----------|------|----------|
| GET | `/api/config/batch` | `getBatchConfig()` | 배치 설정 조회 | - |
| PUT | `/api/config/batch` | `updateBatchConfig()` | 배치 설정 업데이트 | BatchConfig JSON |
| GET | `/api/config/datasource` | `getDatasourceConfig()` | 데이터소스 설정 조회 | type (source/target) |
| GET | `/api/config/mapping` | `getMappingRules()` | 매핑 규칙 조회 | type (table/column/value) |
| PUT | `/api/config/mapping` | `updateMappingRules()` | 매핑 규칙 업데이트 | MappingRule JSON |

### 4.4 HealthController API

| HTTP Method | 엔드포인트 | 메서드명 | 기능 | 파라미터 |
|-------------|-----------|----------|------|----------|
| GET | `/api/health/database` | `checkDatabaseHealth()` | 데이터베이스 연결 상태 확인 | type (source/target) |
| GET | `/api/health/batch` | `checkBatchHealth()` | 배치 시스템 상태 확인 | - |
| GET | `/api/health/system` | `checkSystemHealth()` | 전체 시스템 상태 확인 | - |
| GET | `/api/health/memory` | `getMemoryStatus()` | 메모리 사용량 조회 | - |

## 📊 5. 테스트 프로그램 목록

### 4.1 단위 테스트 (src/test/java)

| 프로그램ID | 프로그램명 | 파일명 | 테스트 대상 | 비고 |
|-----------|-----------|--------|-------------|------|
| TEST-001 | 변환 프로세서 테스트 | DataTransformProcessorTest.java | DataTransformProcessor | @ExtendWith(MockitoExtension.class) |
| TEST-002 | DB 리더 테스트 | DatabaseItemReaderTest.java | DatabaseItemReader | @ExtendWith(MockitoExtension.class) |
| TEST-003 | DB 라이터 테스트 | DatabaseItemWriterTest.java | DatabaseItemWriter | @ExtendWith(MockitoExtension.class) |
| TEST-004 | 컨트롤러 테스트 | MigrationControllerTest.java | MigrationController | @WebMvcTest |
| TEST-005 | 서비스 테스트 | MigrationServiceTest.java | MigrationService | @ExtendWith(MockitoExtension.class) |

### 4.2 통합 테스트

| 프로그램ID | 프로그램명 | 파일명 | 테스트 대상 | 비고 |
|-----------|-----------|--------|-------------|------|
| ITEST-001 | 배치 통합 테스트 | BatchIntegrationTest.java | 전체 배치 플로우 | @SpringBatchTest |
| ITEST-002 | DB 통합 테스트 | DatabaseIntegrationTest.java | 데이터베이스 연동 | @Testcontainers |
| ITEST-003 | API 통합 테스트 | ApiIntegrationTest.java | REST API 전체 | @SpringBootTest |

## 🔄 5. 배치 Bean 및 메서드 상세 목록

### 5.1 Job Configuration Bean

| Job Config 클래스 | Bean 메서드 | Bean 이름 | 실행 순서 | 의존 Step |
|-------------------|-------------|-----------|-----------|-----------|
| DataMigrationJobConfig | `dataMigrationJob()` | dataMigrationJob | 1 | userMigrationStep → categoryMigrationStep → ... |
| UserMigrationJobConfig | `userMigrationJob()` | userMigrationJob | - | userMigrationStep |
| ProductMigrationJobConfig | `productMigrationJob()` | productMigrationJob | - | productMigrationStep |
| OrderMigrationJobConfig | `orderMigrationJob()` | orderMigrationJob | - | orderMigrationStep |
| SingleTableJobConfig | `createSingleTableJob()` | singleTableMigrationJob_{tableName} | - | 동적 생성 Step |

### 5.2 Step Configuration Bean

| Step Config 클래스 | Bean 메서드 | Bean 이름 | Reader Bean | Processor Bean | Writer Bean |
|-------------------|-------------|-----------|-------------|----------------|-------------|
| UserMigrationStepConfig | `userMigrationStep()` | userMigrationStep | userItemReader | dataTransformProcessor | databaseItemWriter |
| CategoryMigrationStepConfig | `categoryMigrationStep()` | categoryMigrationStep | categoryItemReader | dataTransformProcessor | databaseItemWriter |
| ProductMigrationStepConfig | `productMigrationStep()` | productMigrationStep | productItemReader | dataTransformProcessor | databaseItemWriter |
| OrderMigrationStepConfig | `orderMigrationStep()` | orderMigrationStep | orderItemReader | dataTransformProcessor | databaseItemWriter |
| OrderDetailMigrationStepConfig | `orderDetailMigrationStep()` | orderDetailMigrationStep | orderDetailItemReader | dataTransformProcessor | databaseItemWriter |

### 5.3 ItemReader Bean

| Reader 클래스 | Bean 메서드 | Bean 이름 | 대상 테이블 | SQL 쿼리 |
|---------------|-------------|-----------|-------------|----------|
| DatabaseItemReader | `createUserItemReader()` | userItemReader | 사용자 | `SELECT * FROM 사용자 ORDER BY 사용자ID` |
| DatabaseItemReader | `createCategoryItemReader()` | categoryItemReader | 카테고리 | `SELECT * FROM 카테고리 ORDER BY 카테고리ID` |
| DatabaseItemReader | `createProductItemReader()` | productItemReader | 상품 | `SELECT * FROM 상품 ORDER BY 상품ID` |
| DatabaseItemReader | `createOrderItemReader()` | orderItemReader | 주문 | `SELECT * FROM 주문 ORDER BY 주문ID` |
| DatabaseItemReader | `createOrderDetailItemReader()` | orderDetailItemReader | 주문상세 | `SELECT * FROM 주문상세 ORDER BY 주문상세ID` |

### 5.4 ItemProcessor Bean

| Processor 클래스 | Bean 메서드 | Bean 이름 | 변환 기능 | 처리 대상 |
|------------------|-------------|-----------|-----------|-----------|
| DataTransformProcessor | `dataTransformProcessor()` | dataTransformProcessor | 한글→영어 변환, 데이터 검증 | 모든 테이블 |
| ValidationProcessor | `validationProcessor()` | validationProcessor | 데이터 유효성 검증 | 모든 테이블 |
| TypeConversionProcessor | `typeConversionProcessor()` | typeConversionProcessor | 데이터 타입 변환 | 모든 테이블 |
| CompositeProcessor | `compositeProcessor()` | compositeProcessor | 여러 프로세서 조합 | 복잡한 변환 |

### 5.5 ItemWriter Bean

| Writer 클래스 | Bean 메서드 | Bean 이름 | 대상 DB | 쓰기 방식 |
|---------------|-------------|-----------|---------|-----------|
| DatabaseItemWriter | `databaseItemWriter()` | databaseItemWriter | MariaDB | Batch INSERT |
| BatchInsertWriter | `batchInsertWriter()` | batchInsertWriter | MariaDB | 대용량 Batch INSERT |
| ErrorHandlingWriter | `errorHandlingWriter()` | errorHandlingWriter | MariaDB | 오류 처리 포함 쓰기 |
| CompositeWriter | `compositeWriter()` | compositeWriter | MariaDB | 여러 Writer 조합 |

### 5.6 Listener Bean

| Listener 클래스 | Bean 메서드 | Bean 이름 | 이벤트 타입 | 처리 기능 |
|-----------------|-------------|-----------|-------------|-----------|
| JobExecutionListener | `jobExecutionListener()` | jobExecutionListener | Job 시작/완료 | Job 실행 로깅, 알림 |
| MigrationStepListener | `migrationStepListener()` | migrationStepListener | Step 시작/완료 | Step 실행 통계, 진행률 |
| ChunkListener | `chunkListener()` | chunkListener | Chunk 처리 | 청크 단위 모니터링 |
| SkipListener | `skipListener()` | skipListener | Skip 이벤트 | 스킵된 데이터 로깅 |
| RetryListener | `retryListener()` | retryListener | Retry 이벤트 | 재시도 로깅 |

## 📁 6. 설정 파일 목록

### 5.1 애플리케이션 설정

| 파일명 | 위치 | 용도 | 비고 |
|--------|------|------|------|
| application.yml | src/main/resources | 메인 설정 파일 | 프로파일별 설정 |
| application-dev.yml | src/main/resources | 개발환경 설정 | 개발용 DB 설정 |
| application-prod.yml | src/main/resources | 운영환경 설정 | 운영용 DB 설정 |
| logback-spring.xml | src/main/resources | 로깅 설정 | 로그 레벨, 파일 설정 |

### 5.2 빌드 설정

| 파일명 | 위치 | 용도 | 비고 |
|--------|------|------|------|
| build.gradle | 프로젝트 루트 | Gradle 빌드 설정 | 의존성, 플러그인 |
| gradle.properties | 프로젝트 루트 | Gradle 속성 | 버전, 인코딩 설정 |
| settings.gradle | 프로젝트 루트 | 프로젝트 설정 | 프로젝트명 |

### 5.3 Docker 설정

| 파일명 | 위치 | 용도 | 비고 |
|--------|------|------|------|
| Dockerfile | 프로젝트 루트 | Docker 이미지 빌드 | 애플리케이션 컨테이너화 |
| docker-compose.yml | docker/dev | 개발환경 Docker | 개발용 DB 컨테이너 |
| docker-compose-arm64.yml | docker/dev | ARM64 환경 Docker | Apple Silicon 호환 |

## 📈 6. 프로그램 복잡도 및 규모

### 7.1 프로그램 통계 (업데이트)

| 구분 | 개수 | 예상 라인 수 | 주요 클래스 | 비고 |
|------|------|-------------|-------------|------|
| **메인 클래스** | 1 | 50 | BatchMigrationApplication | 애플리케이션 시작점 |
| **설정 클래스** | 4 | 800 | DataSourceConfig, BatchConfig 등 | Spring 설정 |
| **컨트롤러** | 4 | 800 | MigrationController, MonitoringController 등 | REST API (32개 엔드포인트) |
| **서비스** | 6 | 1,800 | MigrationService, MonitoringService 등 | 비즈니스 로직 |
| **Job 설정** | 8 | 2,000 | DataMigrationJobConfig, UserMigrationJobConfig 등 | 배치 Job (5개 주요 Job) |
| **Step 설정** | 12 | 3,000 | UserMigrationStepConfig, ProductMigrationStepConfig 등 | 배치 Step (10개 테이블 Step) |
| **Reader** | 4 | 800 | DatabaseItemReader, CursorReaderFactory 등 | 데이터 읽기 |
| **Processor** | 4 | 1,000 | DataTransformProcessor, ValidationProcessor 등 | 데이터 변환 |
| **Writer** | 4 | 800 | DatabaseItemWriter, BatchInsertWriter 등 | 데이터 쓰기 |
| **Listener** | 5 | 750 | JobExecutionListener, MigrationStepListener 등 | 이벤트 처리 |
| **Model** | 5 | 500 | DataRecord, BatchConfiguration 등 | 데이터 모델 |
| **Exception** | 5 | 300 | MigrationException, DataValidationException 등 | 예외 처리 |
| **Utility** | 6 | 900 | TableMappingUtil, ColumnMappingUtil 등 | 유틸리티 |
| **Test** | 12 | 3,000 | 단위 테스트 8개 + 통합 테스트 4개 | 테스트 코드 |
| **총계** | **80** | **16,500** | 전체 시스템 | 대규모 배치 시스템 |

### 7.2 개발 공수 추정 (업데이트)

| 구분 | 상세 내역 | 개발 공수 (M/D) | 테스트 공수 (M/D) | 총 공수 (M/D) |
|------|-----------|----------------|------------------|---------------|
| **설정 및 인프라** | DataSource, Batch, Transaction 설정 | 4 | 1 | 5 |
| **API 개발** | 4개 Controller, 32개 엔드포인트 | 8 | 3 | 11 |
| **서비스 개발** | 6개 Service 클래스, 비즈니스 로직 | 6 | 2 | 8 |
| **배치 Job 개발** | 8개 Job Config, 5개 주요 Job | 10 | 3 | 13 |
| **배치 Step 개발** | 12개 Step Config, 10개 테이블 Step | 15 | 5 | 20 |
| **데이터 처리** | Reader, Processor, Writer 구현 | 12 | 4 | 16 |
| **리스너 및 모니터링** | 5개 Listener, 메트릭 수집 | 5 | 2 | 7 |
| **예외 처리** | 5개 Exception, 전역 핸들러 | 3 | 1 | 4 |
| **유틸리티** | 6개 Util 클래스, 매핑 로직 | 4 | 1 | 5 |
| **통합 테스트** | 배치 통합, DB 통합, API 통합 | 3 | 5 | 8 |
| **성능 튜닝** | 청크 크기, DB 최적화, JVM 튜닝 | 3 | 2 | 5 |
| **문서화** | API 문서, 배치 설계서, 운영 가이드 | 3 | 0 | 3 |
| **총계** | **80개 클래스, 16,500 라인** | **76** | **29** | **105** |

### 7.3 개발 일정 추정

| 단계 | 기간 (주) | 주요 활동 | 산출물 |
|------|-----------|-----------|--------|
| **1단계: 설계 및 설정** | 1주 | 아키텍처 설계, 환경 설정 | 설계서, 개발환경 |
| **2단계: 기반 개발** | 2주 | API, Service, 기본 배치 구조 | Controller, Service 클래스 |
| **3단계: 배치 개발** | 3주 | Job, Step, Reader/Writer 구현 | 배치 처리 컴포넌트 |
| **4단계: 데이터 처리** | 2주 | Processor, 매핑 로직, 검증 | 데이터 변환 로직 |
| **5단계: 모니터링** | 1주 | Listener, 메트릭, 로깅 | 모니터링 시스템 |
| **6단계: 테스트** | 2주 | 단위 테스트, 통합 테스트 | 테스트 코드 |
| **7단계: 성능 튜닝** | 1주 | 성능 최적화, 부하 테스트 | 성능 보고서 |
| **8단계: 문서화** | 1주 | 사용자 가이드, 운영 매뉴얼 | 문서 세트 |
| **총 개발 기간** | **13주** | **약 3개월** | **완전한 배치 시스템** |

## 🔧 7. 개발 환경 및 도구

### 7.1 개발 도구

| 구분 | 도구명 | 버전 | 용도 |
|------|--------|------|------|
| **IDE** | IntelliJ IDEA | 2024.1 | 통합 개발 환경 |
| **JDK** | OpenJDK | 17 | Java 개발 키트 |
| **빌드** | Gradle | 8.x | 빌드 자동화 |
| **VCS** | Git | 2.x | 버전 관리 |
| **DB 도구** | DBeaver | 23.x | 데이터베이스 클라이언트 |
| **API 테스트** | Postman | 10.x | API 테스트 도구 |

### 7.2 런타임 환경

| 구분 | 도구명 | 버전 | 용도 |
|------|--------|------|------|
| **컨테이너** | Docker | 24.x | 컨테이너화 |
| **오케스트레이션** | Docker Compose | 2.x | 로컬 환경 구성 |
| **모니터링** | Spring Actuator | 3.x | 애플리케이션 모니터링 |
| **로깅** | Logback | 1.4.x | 로깅 프레임워크 |

이 프로그램 목록은 SI 프로젝트에서 요구되는 상세한 프로그램 정보와 개발 계획을 포함하고 있습니다.
