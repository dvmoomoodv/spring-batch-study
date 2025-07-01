# MSSQL to MariaDB 데이터 이관 시스템 아키텍처

## 📋 목차
1. [시스템 개요](#시스템-개요)
2. [전체 아키텍처](#전체-아키텍처)
3. [환경별 구성](#환경별-구성)
4. [컴포넌트 상세](#컴포넌트-상세)
5. [데이터 플로우](#데이터-플로우)
6. [기술 스택](#기술-스택)
7. [보안 고려사항](#보안-고려사항)

## 🎯 시스템 개요

본 시스템은 **Spring Batch**를 기반으로 한 **MSSQL에서 MariaDB로의 데이터 이관 솔루션**입니다. 한글 테이블명과 컬럼명을 영어로 변환하면서 대용량 데이터를 안전하고 효율적으로 이관하는 것을 목표로 합니다.

### 주요 특징
- ✅ **청크 기반 처리**: 메모리 효율적인 대용량 데이터 처리
- ✅ **한글-영어 변환**: 테이블명, 컬럼명, 값의 자동 변환
- ✅ **환경별 설정**: 개발(1:1), 운영(Master-Slave) 환경 지원
- ✅ **실시간 모니터링**: REST API 기반 진행 상황 추적
- ✅ **오류 복구**: 재시도, 스킵, 롤백 메커니즘
- ✅ **Docker 기반**: 컨테이너화된 개발/테스트 환경

## 🏗️ 전체 아키텍처

```mermaid
graph TB
    subgraph "Source System"
        MSSQL[(MSSQL Server<br/>한글 테이블/컬럼)]
    end
    
    subgraph "Batch Application"
        API[REST API Controller]
        JOB[Spring Batch Job]
        READER[Database Reader]
        PROCESSOR[Data Transform Processor<br/>한글→영어 변환]
        WRITER[Database Writer]
        
        API --> JOB
        JOB --> READER
        READER --> PROCESSOR
        PROCESSOR --> WRITER
    end
    
    subgraph "Target System - Development"
        MARIADB_DEV[(MariaDB<br/>영어 테이블/컬럼)]
    end
    
    subgraph "Target System - Production"
        MARIADB_MASTER[(MariaDB Master<br/>Write)]
        MARIADB_SLAVE[(MariaDB Slave<br/>Read)]
        
        MARIADB_MASTER -.->|Replication| MARIADB_SLAVE
    end
    
    subgraph "Monitoring & Management"
        LOGS[Application Logs]
        METRICS[Performance Metrics]
        WEB_UI[Web Management UI]
    end
    
    MSSQL --> READER
    WRITER --> MARIADB_DEV
    WRITER --> MARIADB_MASTER
    
    JOB --> LOGS
    JOB --> METRICS
    API --> WEB_UI
    
    style MSSQL fill:#ff9999
    style MARIADB_DEV fill:#99ccff
    style MARIADB_MASTER fill:#99ccff
    style MARIADB_SLAVE fill:#ccccff
    style PROCESSOR fill:#ffcc99
```

## 🌍 환경별 구성

### 개발 환경 (Development)

```mermaid
graph LR
    subgraph "Development Environment"
        subgraph "Docker Containers"
            MSSQL_DEV[MSSQL Dev<br/>Port: 1433]
            MARIADB_DEV[MariaDB Dev<br/>Port: 3306]
            ADMINER[Adminer<br/>Port: 8082]
        end
        
        subgraph "Application"
            BATCH_APP[Batch Application<br/>Port: 8080]
        end
    end
    
    MSSQL_DEV --> BATCH_APP
    BATCH_APP --> MARIADB_DEV
    ADMINER --> MSSQL_DEV
    ADMINER --> MARIADB_DEV
    
    style MSSQL_DEV fill:#ff9999
    style MARIADB_DEV fill:#99ccff
    style BATCH_APP fill:#99ff99
```

| 컴포넌트 | 포트 | 용도 | 접속 정보 |
|---------|------|------|-----------|
| MSSQL Dev | 1433 | 소스 데이터베이스 | sa / DevPassword123! |
| MariaDB Dev | 3306 | 타겟 데이터베이스 | root / DevPassword123! |
| Adminer | 8082 | DB 관리 도구 | Web UI |
| Batch App | 8080 | 배치 애플리케이션 | REST API |

### 운영 환경 (Production)

```mermaid
graph TB
    subgraph "Production Environment"
        subgraph "Source"
            MSSQL_PROD[MSSQL Production<br/>Port: 1434]
        end
        
        subgraph "Target - Master/Slave"
            MARIADB_MASTER[MariaDB Master<br/>Port: 3307<br/>Read/Write]
            MARIADB_SLAVE[MariaDB Slave<br/>Port: 3308<br/>Read Only]
            
            MARIADB_MASTER -.->|Binary Log<br/>Replication| MARIADB_SLAVE
        end
        
        subgraph "Application Cluster"
            BATCH_APP1[Batch App 1]
            BATCH_APP2[Batch App 2]
            LB[Load Balancer]
            
            LB --> BATCH_APP1
            LB --> BATCH_APP2
        end
        
        subgraph "Management"
            PMA_MASTER[phpMyAdmin Master<br/>Port: 8083]
            PMA_SLAVE[phpMyAdmin Slave<br/>Port: 8084]
            ADMINER_PROD[Adminer<br/>Port: 8085]
        end
    end
    
    MSSQL_PROD --> BATCH_APP1
    MSSQL_PROD --> BATCH_APP2
    BATCH_APP1 --> MARIADB_MASTER
    BATCH_APP2 --> MARIADB_MASTER
    
    PMA_MASTER --> MARIADB_MASTER
    PMA_SLAVE --> MARIADB_SLAVE
    ADMINER_PROD --> MSSQL_PROD
    
    style MSSQL_PROD fill:#ff6666
    style MARIADB_MASTER fill:#6699ff
    style MARIADB_SLAVE fill:#99ccff
    style BATCH_APP1 fill:#66ff66
    style BATCH_APP2 fill:#66ff66
```

| 환경 | 컴포넌트 | 포트 | 역할 | 고가용성 |
|------|---------|------|------|----------|
| 운영 | MSSQL Prod | 1434 | 소스 DB | Cluster |
| 운영 | MariaDB Master | 3307 | 타겟 DB (Write) | Master-Slave |
| 운영 | MariaDB Slave | 3308 | 타겟 DB (Read) | Replication |
| 운영 | Batch App | 8080 | 배치 처리 | Load Balanced |

## 🔧 컴포넌트 상세

### 1. Spring Batch 아키텍처

```mermaid
graph TD
    subgraph "Spring Batch Core"
        JOB_LAUNCHER[JobLauncher]
        JOB_REPOSITORY[JobRepository]
        
        subgraph "Job Configuration"
            JOB[DataMigrationJob]
            STEP1[사용자 Migration Step]
            STEP2[상품 Migration Step]
            STEP3[주문 Migration Step]
            STEP_N[... 기타 테이블 Steps]
            
            JOB --> STEP1
            STEP1 --> STEP2
            STEP2 --> STEP3
            STEP3 --> STEP_N
        end
        
        subgraph "Step Components"
            READER[DatabaseItemReader<br/>MSSQL Cursor]
            PROCESSOR[DataTransformProcessor<br/>한글→영어 변환]
            WRITER[DatabaseItemWriter<br/>MariaDB Batch Insert]
            
            READER --> PROCESSOR
            PROCESSOR --> WRITER
        end
        
        subgraph "Monitoring"
            LISTENER[MigrationStepListener]
            METRICS[Performance Metrics]
            
            STEP1 --> LISTENER
            LISTENER --> METRICS
        end
    end
    
    JOB_LAUNCHER --> JOB
    JOB --> JOB_REPOSITORY
    
    style PROCESSOR fill:#ffcc99
    style LISTENER fill:#ccffcc
```

### 2. 데이터 변환 프로세서 상세

| 변환 유형 | 소스 (한글) | 타겟 (영어) | 예시 |
|-----------|-------------|-------------|------|
| **테이블명** | 사용자 | users | 사용자 → users |
| **테이블명** | 상품 | products | 상품 → products |
| **테이블명** | 주문 | orders | 주문 → orders |
| **컬럼명** | 사용자ID | user_id | 사용자ID → user_id |
| **컬럼명** | 이름 | name | 이름 → name |
| **컬럼명** | 주문상태 | order_status | 주문상태 → order_status |
| **값 변환** | 남성 | MALE | 성별: 남성 → MALE |
| **값 변환** | 배송완료 | DELIVERED | 주문상태: 배송완료 → DELIVERED |
| **값 변환** | 판매중 | ON_SALE | 판매상태: 판매중 → ON_SALE |

### 3. 배치 처리 설정

| 설정 항목 | 개발환경 | 운영환경 | 설명 |
|-----------|----------|----------|------|
| **Chunk Size** | 100-500 | 1000-5000 | 한 번에 처리할 레코드 수 |
| **Skip Limit** | 50 | 100-500 | 허용 가능한 오류 개수 |
| **Retry Limit** | 3 | 3-5 | 재시도 횟수 |
| **Connection Pool** | 5-10 | 20-50 | DB 연결 풀 크기 |
| **Thread Pool** | 2-4 | 8-16 | 병렬 처리 스레드 수 |

## 🔄 데이터 플로우

### 1. 전체 데이터 플로우

```mermaid
sequenceDiagram
    participant Client as REST Client
    participant API as Migration Controller
    participant Job as Batch Job
    participant Reader as Item Reader
    participant Processor as Transform Processor
    participant Writer as Item Writer
    participant Source as MSSQL
    participant Target as MariaDB
    
    Client->>API: POST /api/migration/start
    API->>Job: Launch Migration Job
    
    loop For Each Table
        Job->>Reader: Read Chunk (1000 records)
        Reader->>Source: SELECT * FROM 한글테이블
        Source-->>Reader: Return Records
        
        Reader->>Processor: Process Records
        Note over Processor: 한글→영어 변환<br/>데이터 검증<br/>타입 변환
        
        Processor->>Writer: Write Transformed Records
        Writer->>Target: Batch INSERT INTO english_table
        Target-->>Writer: Confirm Insert
        
        Writer-->>Job: Report Progress
    end
    
    Job-->>API: Job Completion Status
    API-->>Client: Migration Result
```

### 2. 오류 처리 플로우

```mermaid
graph TD
    START[레코드 처리 시작]
    PROCESS[데이터 변환 처리]
    VALIDATE[데이터 검증]
    WRITE[데이터베이스 쓰기]
    
    ERROR_SKIP[Skip 처리]
    ERROR_RETRY[Retry 처리]
    ERROR_FAIL[Job 실패]
    
    SUCCESS[처리 완료]
    
    START --> PROCESS
    PROCESS --> VALIDATE
    VALIDATE --> WRITE
    WRITE --> SUCCESS
    
    PROCESS -->|변환 오류| ERROR_SKIP
    VALIDATE -->|검증 실패| ERROR_SKIP
    WRITE -->|DB 오류| ERROR_RETRY
    
    ERROR_RETRY -->|재시도 한도 초과| ERROR_SKIP
    ERROR_RETRY -->|재시도| WRITE
    ERROR_SKIP -->|Skip 한도 초과| ERROR_FAIL
    ERROR_SKIP -->|계속 처리| START
    
    style ERROR_SKIP fill:#ffcccc
    style ERROR_RETRY fill:#ffffcc
    style ERROR_FAIL fill:#ff9999
    style SUCCESS fill:#ccffcc
```

## 💻 기술 스택

### Backend 기술 스택

| 계층 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **Language** | Java | 17 | 메인 개발 언어 |
| **Framework** | Spring Boot | 3.3.5 | 애플리케이션 프레임워크 |
| **Batch** | Spring Batch | 5.x | 배치 처리 프레임워크 |
| **Build** | Gradle | 8.x | 빌드 도구 |
| **Database** | MSSQL Server | 2022 | 소스 데이터베이스 |
| **Database** | MariaDB | 11.2 | 타겟 데이터베이스 |
| **Connection Pool** | HikariCP | 5.1.0 | 데이터베이스 연결 풀 |
| **Testing** | JUnit 5 | 5.10.0 | 단위 테스트 |
| **Testing** | Mockito | 5.x | 모킹 프레임워크 |
| **Testing** | Testcontainers | 1.19.x | 통합 테스트 |

### Infrastructure 기술 스택

| 계층 | 기술 | 용도 |
|------|------|------|
| **Containerization** | Docker | 컨테이너화 |
| **Orchestration** | Docker Compose | 로컬 개발 환경 |
| **Database Management** | Adminer | DB 관리 도구 |
| **Database Management** | phpMyAdmin | MariaDB 전용 관리 |
| **Monitoring** | Spring Actuator | 애플리케이션 모니터링 |
| **Logging** | Logback | 로깅 프레임워크 |

### 개발 도구

| 도구 | 용도 |
|------|------|
| **IntelliJ IDEA** | 통합 개발 환경 |
| **Git** | 버전 관리 |
| **Postman** | API 테스트 |
| **DBeaver** | 데이터베이스 클라이언트 |

## 🔒 보안 고려사항

### 1. 데이터베이스 보안

| 보안 요소 | 구현 방법 | 설명 |
|-----------|-----------|------|
| **연결 암호화** | SSL/TLS | 데이터베이스 연결 암호화 |
| **인증** | 사용자/비밀번호 | 강력한 비밀번호 정책 |
| **권한 관리** | 최소 권한 원칙 | 필요한 권한만 부여 |
| **네트워크 격리** | Docker Network | 컨테이너 간 네트워크 분리 |

### 2. 애플리케이션 보안

```mermaid
graph LR
    subgraph "Security Layers"
        AUTH[Authentication]
        AUTHZ[Authorization]
        ENCRYPT[Data Encryption]
        AUDIT[Audit Logging]
        
        AUTH --> AUTHZ
        AUTHZ --> ENCRYPT
        ENCRYPT --> AUDIT
    end
    
    subgraph "Implementation"
        JWT[JWT Tokens]
        RBAC[Role-Based Access]
        AES[AES Encryption]
        LOG[Security Logs]
        
        AUTH --> JWT
        AUTHZ --> RBAC
        ENCRYPT --> AES
        AUDIT --> LOG
    end
```

### 3. 운영 보안 체크리스트

- [ ] **비밀번호 관리**: 환경변수 또는 Secret 관리 도구 사용
- [ ] **네트워크 보안**: 방화벽 설정 및 VPN 접근
- [ ] **로그 모니터링**: 보안 이벤트 실시간 모니터링
- [ ] **백업 암호화**: 데이터 백업 시 암호화 적용
- [ ] **접근 제어**: IP 화이트리스트 및 시간 기반 접근 제어
- [ ] **정기 보안 점검**: 취약점 스캔 및 보안 업데이트

## 📊 성능 최적화

### 1. 배치 성능 튜닝 가이드

| 항목 | 소량 데이터 | 중량 데이터 | 대량 데이터 |
|------|-------------|-------------|-------------|
| **Chunk Size** | 100-500 | 1000-2000 | 2000-5000 |
| **Thread Pool** | 2-4 | 4-8 | 8-16 |
| **Connection Pool** | 5-10 | 10-20 | 20-50 |
| **JVM Heap** | 1-2GB | 2-4GB | 4-8GB |
| **Fetch Size** | 100-500 | 500-1000 | 1000-2000 |

### 2. 모니터링 지표

```mermaid
graph TD
    subgraph "Performance Metrics"
        TPS[Records/Second]
        MEM[Memory Usage]
        CPU[CPU Usage]
        DB_CONN[DB Connections]
        ERROR_RATE[Error Rate]
        
        subgraph "Thresholds"
            TPS_WARN[< 100 rps]
            MEM_WARN[> 80%]
            CPU_WARN[> 70%]
            ERROR_WARN[> 1%]
        end
    end
    
    TPS --> TPS_WARN
    MEM --> MEM_WARN
    CPU --> CPU_WARN
    ERROR_RATE --> ERROR_WARN
    
    style TPS_WARN fill:#ffcccc
    style MEM_WARN fill:#ffcccc
    style CPU_WARN fill:#ffcccc
    style ERROR_WARN fill:#ffcccc
```

이 아키텍처 문서는 시스템의 전체적인 구조와 각 컴포넌트의 역할, 그리고 운영 시 고려해야 할 사항들을 포괄적으로 다루고 있습니다. 개발팀과 운영팀이 시스템을 이해하고 효과적으로 관리할 수 있도록 도움을 제공합니다.
