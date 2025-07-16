# 데이터 이관 프로세스 다이어그램

## 📋 문서 정보
- **프로젝트명**: MSSQL to MariaDB 데이터 이관 시스템
- **문서 유형**: 프로세스 다이어그램
- **작성일**: 2024-07-16
- **작성자**: SI 개발팀

## 🎯 1. 전체 시스템 프로세스 플로우

### 1.1 시스템 개요 다이어그램

```mermaid
graph TB
    subgraph "사용자 인터페이스"
        UI[웹 관리 도구<br/>Adminer/phpMyAdmin]
        API[REST API<br/>Postman/curl]
    end
    
    subgraph "애플리케이션 계층"
        CONTROLLER[MigrationController<br/>REST 엔드포인트]
        SERVICE[MigrationService<br/>비즈니스 로직]
        LAUNCHER[JobLauncher<br/>배치 실행기]
    end
    
    subgraph "배치 처리 계층"
        JOB[DataMigrationJob<br/>전체 이관 작업]
        STEP1[UserMigrationStep<br/>사용자 이관]
        STEP2[ProductMigrationStep<br/>상품 이관]
        STEPN[...기타 Step들]
    end
    
    subgraph "데이터 처리 계층"
        READER[DatabaseItemReader<br/>MSSQL 데이터 읽기]
        PROCESSOR[DataTransformProcessor<br/>한글→영어 변환]
        WRITER[DatabaseItemWriter<br/>MariaDB 데이터 쓰기]
    end
    
    subgraph "데이터 저장소"
        MSSQL[(MSSQL Server<br/>소스 데이터베이스<br/>한글 테이블/컬럼)]
        MARIADB[(MariaDB<br/>타겟 데이터베이스<br/>영어 테이블/컬럼)]
        BATCH_META[(Batch Metadata<br/>배치 실행 이력)]
    end
    
    subgraph "모니터링"
        LOGS[로그 파일<br/>batch-migration.log]
        METRICS[메트릭<br/>Actuator/Prometheus]
        ALERTS[알림<br/>Slack/Email]
    end
    
    UI --> CONTROLLER
    API --> CONTROLLER
    CONTROLLER --> SERVICE
    SERVICE --> LAUNCHER
    LAUNCHER --> JOB
    
    JOB --> STEP1
    JOB --> STEP2
    JOB --> STEPN
    
    STEP1 --> READER
    STEP1 --> PROCESSOR
    STEP1 --> WRITER
    
    READER --> MSSQL
    PROCESSOR --> PROCESSOR
    WRITER --> MARIADB
    
    LAUNCHER --> BATCH_META
    
    JOB --> LOGS
    JOB --> METRICS
    METRICS --> ALERTS
    
    style JOB fill:#99ccff
    style PROCESSOR fill:#ffcc99
    style MSSQL fill:#ff9999
    style MARIADB fill:#99ff99
```

## 🔄 2. 배치 실행 프로세스

### 2.1 전체 배치 실행 플로우

```mermaid
sequenceDiagram
    participant User as 사용자
    participant API as REST API
    participant Controller as MigrationController
    participant Launcher as JobLauncher
    participant Job as DataMigrationJob
    participant Step as MigrationStep
    participant Reader as ItemReader
    participant Processor as ItemProcessor
    participant Writer as ItemWriter
    participant MSSQL as MSSQL DB
    participant MariaDB as MariaDB
    
    User->>API: POST /api/migration/start
    API->>Controller: 배치 실행 요청
    Controller->>Launcher: Job 실행 요청
    
    Launcher->>Job: Job 시작
    Note over Job: Job Parameters 설정<br/>startTime, chunkSize 등
    
    Job->>Step: Step 실행 (사용자 테이블)
    
    loop 청크 단위 반복 처리
        Step->>Reader: 데이터 읽기 요청
        Reader->>MSSQL: SELECT * FROM 사용자 LIMIT 1000
        MSSQL-->>Reader: 한글 데이터 반환
        Reader-->>Step: DataRecord List
        
        Step->>Processor: 데이터 변환 요청
        Note over Processor: 테이블명: 사용자→users<br/>컬럼명: 사용자ID→user_id<br/>값: 남성→MALE
        Processor-->>Step: 변환된 DataRecord List
        
        Step->>Writer: 데이터 쓰기 요청
        Writer->>MariaDB: Batch INSERT INTO users
        MariaDB-->>Writer: 쓰기 완료
        Writer-->>Step: 처리 완료
        
        Step->>Step: 트랜잭션 커밋
    end
    
    Step-->>Job: Step 완료 (사용자)
    Job->>Step: 다음 Step 실행 (상품)
    Note over Step: 동일한 처리 과정 반복
    
    Step-->>Job: 모든 Step 완료
    Job-->>Launcher: Job 완료
    Launcher-->>Controller: 실행 결과
    Controller-->>API: JSON 응답
    API-->>User: 배치 실행 결과
```

### 2.2 단일 테이블 이관 프로세스

```mermaid
flowchart TD
    START([API 호출<br/>POST /api/migration/table/사용자])
    
    VALIDATE{파라미터 검증}
    VALIDATE -->|유효| CREATE_JOB[SingleTableJob 생성]
    VALIDATE -->|무효| ERROR_RESPONSE[에러 응답 반환]
    
    CREATE_JOB --> SET_PARAMS[Job Parameters 설정<br/>tableName: 사용자<br/>whereClause: 조건]
    
    SET_PARAMS --> LAUNCH_JOB[Job 실행]
    
    LAUNCH_JOB --> STEP_START[Step 시작<br/>사용자MigrationStep]
    
    STEP_START --> READ_CHUNK[청크 단위 데이터 읽기<br/>MSSQL에서 1000건씩]
    
    READ_CHUNK --> HAS_DATA{데이터 존재?}
    HAS_DATA -->|Yes| PROCESS_DATA[데이터 변환 처리<br/>한글→영어 변환]
    HAS_DATA -->|No| STEP_END[Step 완료]
    
    PROCESS_DATA --> VALIDATE_DATA{데이터 검증}
    VALIDATE_DATA -->|성공| WRITE_DATA[MariaDB에 배치 쓰기]
    VALIDATE_DATA -->|실패| SKIP_RECORD[레코드 스킵<br/>오류 로그 기록]
    
    WRITE_DATA --> COMMIT_TX[트랜잭션 커밋]
    SKIP_RECORD --> CHECK_SKIP_LIMIT{스킵 한도 초과?}
    
    CHECK_SKIP_LIMIT -->|No| READ_CHUNK
    CHECK_SKIP_LIMIT -->|Yes| JOB_FAIL[Job 실패]
    
    COMMIT_TX --> UPDATE_STATS[처리 통계 업데이트]
    UPDATE_STATS --> READ_CHUNK
    
    STEP_END --> JOB_SUCCESS[Job 성공 완료]
    
    JOB_SUCCESS --> SUCCESS_RESPONSE[성공 응답 반환]
    JOB_FAIL --> FAIL_RESPONSE[실패 응답 반환]
    
    style START fill:#e1f5fe
    style PROCESS_DATA fill:#ffcc99
    style WRITE_DATA fill:#c8e6c9
    style JOB_SUCCESS fill:#a5d6a7
    style JOB_FAIL fill:#ffcdd2
```

## 📊 3. 데이터 변환 프로세스

### 3.1 데이터 변환 상세 플로우

```mermaid
graph TD
    INPUT[원본 데이터<br/>MSSQL ResultSet]
    
    subgraph "DataTransformProcessor"
        EXTRACT[데이터 추출<br/>ResultSet → Map]
        
        subgraph "변환 단계"
            TABLE_TRANSFORM[테이블명 변환<br/>사용자 → users]
            COLUMN_TRANSFORM[컬럼명 변환<br/>사용자ID → user_id<br/>이름 → name]
            VALUE_TRANSFORM[값 변환<br/>남성 → MALE<br/>배송완료 → DELIVERED]
        end
        
        VALIDATE[데이터 검증<br/>필수값, 타입, 길이]
        TYPE_CONVERT[타입 변환<br/>String, Number, Date]
        ADD_METADATA[메타데이터 추가<br/>migrated_at 컬럼]
        
        ERROR_HANDLE[오류 처리<br/>Skip/Retry 결정]
    end
    
    OUTPUT[변환된 데이터<br/>DataRecord 객체]
    SKIP[스킵된 데이터<br/>오류 로그]
    
    INPUT --> EXTRACT
    EXTRACT --> TABLE_TRANSFORM
    TABLE_TRANSFORM --> COLUMN_TRANSFORM
    COLUMN_TRANSFORM --> VALUE_TRANSFORM
    
    VALUE_TRANSFORM --> VALIDATE
    VALIDATE -->|성공| TYPE_CONVERT
    VALIDATE -->|실패| ERROR_HANDLE
    
    TYPE_CONVERT --> ADD_METADATA
    ADD_METADATA --> OUTPUT
    
    ERROR_HANDLE -->|Skip| SKIP
    ERROR_HANDLE -->|Retry| VALIDATE
    
    style TABLE_TRANSFORM fill:#e3f2fd
    style COLUMN_TRANSFORM fill:#e8f5e8
    style VALUE_TRANSFORM fill:#fff3e0
    style VALIDATE fill:#fce4ec
    style ERROR_HANDLE fill:#ffebee
```

### 3.2 테이블별 변환 매핑

```mermaid
graph LR
    subgraph "소스 테이블 (MSSQL - 한글)"
        T1[사용자<br/>- 사용자ID<br/>- 이름<br/>- 성별: 남성/여성]
        T2[상품<br/>- 상품ID<br/>- 상품명<br/>- 판매상태: 판매중/품절]
        T3[주문<br/>- 주문ID<br/>- 주문상태: 배송완료/배송중]
    end
    
    subgraph "변환 규칙"
        RULE1[테이블명 변환<br/>한글 → 영어]
        RULE2[컬럼명 변환<br/>한글 → snake_case]
        RULE3[값 변환<br/>한글 → 영어 상수]
    end
    
    subgraph "타겟 테이블 (MariaDB - 영어)"
        T4[users<br/>- user_id<br/>- name<br/>- gender: MALE/FEMALE]
        T5[products<br/>- product_id<br/>- product_name<br/>- sales_status: ON_SALE/OUT_OF_STOCK]
        T6[orders<br/>- order_id<br/>- order_status: DELIVERED/SHIPPING]
    end
    
    T1 --> RULE1 --> T4
    T2 --> RULE2 --> T5
    T3 --> RULE3 --> T6
    
    style T1 fill:#ffcdd2
    style T2 fill:#ffcdd2
    style T3 fill:#ffcdd2
    style T4 fill:#c8e6c9
    style T5 fill:#c8e6c9
    style T6 fill:#c8e6c9
    style RULE1 fill:#fff3e0
    style RULE2 fill:#fff3e0
    style RULE3 fill:#fff3e0
```

## 🔧 4. 오류 처리 프로세스

### 4.1 오류 처리 플로우

```mermaid
flowchart TD
    PROCESS_START[데이터 처리 시작]
    
    READ_DATA[데이터 읽기]
    READ_ERROR{읽기 오류?}
    
    TRANSFORM_DATA[데이터 변환]
    TRANSFORM_ERROR{변환 오류?}
    
    WRITE_DATA[데이터 쓰기]
    WRITE_ERROR{쓰기 오류?}
    
    SUCCESS[처리 성공]
    
    subgraph "오류 처리"
        LOG_ERROR[오류 로그 기록]
        CHECK_RETRY{재시도 가능?}
        RETRY_COUNT{재시도 횟수<br/>< 3회?}
        SKIP_ITEM[아이템 스킵]
        CHECK_SKIP_LIMIT{스킵 한도<br/>< 100개?}
        JOB_FAIL[Job 실패]
    end
    
    PROCESS_START --> READ_DATA
    READ_DATA --> READ_ERROR
    READ_ERROR -->|No| TRANSFORM_DATA
    READ_ERROR -->|Yes| LOG_ERROR
    
    TRANSFORM_DATA --> TRANSFORM_ERROR
    TRANSFORM_ERROR -->|No| WRITE_DATA
    TRANSFORM_ERROR -->|Yes| LOG_ERROR
    
    WRITE_DATA --> WRITE_ERROR
    WRITE_ERROR -->|No| SUCCESS
    WRITE_ERROR -->|Yes| LOG_ERROR
    
    LOG_ERROR --> CHECK_RETRY
    CHECK_RETRY -->|Yes| RETRY_COUNT
    CHECK_RETRY -->|No| SKIP_ITEM
    
    RETRY_COUNT -->|Yes| READ_DATA
    RETRY_COUNT -->|No| SKIP_ITEM
    
    SKIP_ITEM --> CHECK_SKIP_LIMIT
    CHECK_SKIP_LIMIT -->|Yes| PROCESS_START
    CHECK_SKIP_LIMIT -->|No| JOB_FAIL
    
    style SUCCESS fill:#c8e6c9
    style JOB_FAIL fill:#ffcdd2
    style LOG_ERROR fill:#fff3e0
    style SKIP_ITEM fill:#ffecb3
```

### 4.2 예외 유형별 처리 전략

```mermaid
graph TD
    EXCEPTION[예외 발생]
    
    subgraph "예외 분류"
        DB_ERROR[데이터베이스 오류<br/>Connection, Timeout]
        DATA_ERROR[데이터 오류<br/>Validation, Conversion]
        SYSTEM_ERROR[시스템 오류<br/>Memory, Network]
    end
    
    subgraph "처리 전략"
        RETRY[재시도<br/>최대 3회]
        SKIP[스킵<br/>로그 기록 후 계속]
        FAIL[실패<br/>Job 중단]
    end
    
    subgraph "복구 방법"
        AUTO_RECOVERY[자동 복구<br/>재시도 성공]
        MANUAL_FIX[수동 수정<br/>데이터 정정 후 재실행]
        SYSTEM_RESTART[시스템 재시작<br/>리소스 정리 후 재실행]
    end
    
    EXCEPTION --> DB_ERROR
    EXCEPTION --> DATA_ERROR
    EXCEPTION --> SYSTEM_ERROR
    
    DB_ERROR --> RETRY
    DATA_ERROR --> SKIP
    SYSTEM_ERROR --> FAIL
    
    RETRY --> AUTO_RECOVERY
    SKIP --> MANUAL_FIX
    FAIL --> SYSTEM_RESTART
    
    style DB_ERROR fill:#e3f2fd
    style DATA_ERROR fill:#fff3e0
    style SYSTEM_ERROR fill:#ffebee
    style RETRY fill:#e8f5e8
    style SKIP fill:#fff9c4
    style FAIL fill:#ffcdd2
```

## 📈 5. 모니터링 프로세스

### 5.1 실시간 모니터링 플로우

```mermaid
graph TB
    subgraph "데이터 수집"
        BATCH_EVENTS[배치 이벤트<br/>Step 시작/완료]
        METRICS[성능 메트릭<br/>처리속도, 메모리]
        LOGS[로그 데이터<br/>오류, 경고]
    end
    
    subgraph "데이터 처리"
        COLLECTOR[메트릭 수집기<br/>Micrometer]
        AGGREGATOR[데이터 집계<br/>통계 계산]
        FORMATTER[데이터 포맷팅<br/>JSON, Prometheus]
    end
    
    subgraph "모니터링 도구"
        ACTUATOR[Spring Actuator<br/>/actuator/metrics]
        PROMETHEUS[Prometheus<br/>메트릭 저장소]
        GRAFANA[Grafana<br/>대시보드]
    end
    
    subgraph "알림 시스템"
        ALERT_RULES[알림 규칙<br/>임계값 설정]
        NOTIFICATION[알림 발송<br/>Slack, Email]
    end
    
    BATCH_EVENTS --> COLLECTOR
    METRICS --> COLLECTOR
    LOGS --> COLLECTOR
    
    COLLECTOR --> AGGREGATOR
    AGGREGATOR --> FORMATTER
    
    FORMATTER --> ACTUATOR
    FORMATTER --> PROMETHEUS
    PROMETHEUS --> GRAFANA
    
    GRAFANA --> ALERT_RULES
    ALERT_RULES --> NOTIFICATION
    
    style COLLECTOR fill:#e3f2fd
    style GRAFANA fill:#e8f5e8
    style NOTIFICATION fill:#fff3e0
```

이 프로세스 다이어그램은 SI 프로젝트에서 시스템의 전체적인 흐름과 각 단계별 처리 과정을 명확하게 보여줍니다.
