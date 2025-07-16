# 배치 관리 데이터베이스 설계서 (MySQL)

## 📋 문서 정보
- **프로젝트명**: MSSQL to MySQL 대용량 데이터 이관 시스템
- **데이터베이스**: MySQL 8.0
- **용도**: 배치 작업 관리 및 메타데이터 저장
- **작성일**: 2024-07-16
- **작성자**: SI 개발팀

## 🎯 1. 데이터베이스 개요

### 1.1 설계 목적
- **배치 작업 실행 이력 관리**
- **테이블 매핑 정보 저장**
- **데이터 이관 진행 상황 추적**
- **오류 및 성능 모니터링**

### 1.2 데이터베이스 구성
- **데이터베이스명**: `batch_management`
- **문자셋**: `utf8mb4`
- **콜레이션**: `utf8mb4_unicode_ci`
- **엔진**: `InnoDB`

## 📊 2. 테이블 설계

### 2.1 배치 실행 관리 테이블

#### 2.1.1 batch_jobs (배치 작업 정보)
```sql
CREATE TABLE batch_jobs (
    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    job_type ENUM('FULL_MIGRATION', 'DOMAIN_MIGRATION', 'TABLE_MIGRATION') NOT NULL,
    domain_name VARCHAR(50),
    status ENUM('READY', 'RUNNING', 'COMPLETED', 'FAILED', 'STOPPED') NOT NULL DEFAULT 'READY',
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    duration_seconds INT,
    total_tables INT DEFAULT 0,
    completed_tables INT DEFAULT 0,
    total_records BIGINT DEFAULT 0,
    processed_records BIGINT DEFAULT 0,
    error_records BIGINT DEFAULT 0,
    skip_records BIGINT DEFAULT 0,
    chunk_size INT DEFAULT 1000,
    skip_limit INT DEFAULT 100,
    retry_limit INT DEFAULT 3,
    created_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_job_name (job_name),
    INDEX idx_domain_name (domain_name),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

#### 2.1.2 batch_steps (배치 스텝 정보)
```sql
CREATE TABLE batch_steps (
    step_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    domain_name VARCHAR(50),
    source_table VARCHAR(100),
    target_table VARCHAR(100),
    step_order INT NOT NULL,
    status ENUM('READY', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED') NOT NULL DEFAULT 'READY',
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    duration_seconds INT,
    read_count BIGINT DEFAULT 0,
    write_count BIGINT DEFAULT 0,
    skip_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    commit_count INT DEFAULT 0,
    rollback_count INT DEFAULT 0,
    where_clause TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (job_id) REFERENCES batch_jobs(job_id) ON DELETE CASCADE,
    INDEX idx_job_id (job_id),
    INDEX idx_step_name (step_name),
    INDEX idx_domain_name (domain_name),
    INDEX idx_source_table (source_table),
    INDEX idx_status (status)
);
```

### 2.2 도메인 및 테이블 매핑 관리

#### 2.2.1 domain_mapping (도메인 매핑 정보)
```sql
CREATE TABLE domain_mapping (
    domain_id INT AUTO_INCREMENT PRIMARY KEY,
    domain_name VARCHAR(50) NOT NULL UNIQUE,
    domain_description VARCHAR(200),
    source_schema VARCHAR(50) DEFAULT 'dbo',
    target_schema VARCHAR(50) DEFAULT 'target_db',
    table_count INT DEFAULT 0,
    migration_order INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_domain_name (domain_name),
    INDEX idx_migration_order (migration_order)
);
```

#### 2.2.2 table_mapping (테이블 매핑 정보)
```sql
CREATE TABLE table_mapping (
    mapping_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_id INT NOT NULL,
    source_table VARCHAR(100) NOT NULL,
    target_table VARCHAR(100) NOT NULL,
    table_description VARCHAR(200),
    migration_order INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    chunk_size INT DEFAULT 1000,
    where_clause TEXT,
    estimated_rows BIGINT DEFAULT 0,
    last_migrated_at TIMESTAMP NULL,
    migration_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (domain_id) REFERENCES domain_mapping(domain_id) ON DELETE CASCADE,
    UNIQUE KEY uk_source_table (source_table),
    UNIQUE KEY uk_target_table (target_table),
    INDEX idx_domain_id (domain_id),
    INDEX idx_migration_order (migration_order)
);
```

#### 2.2.3 column_mapping (컬럼 매핑 정보)
```sql
CREATE TABLE column_mapping (
    column_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mapping_id BIGINT NOT NULL,
    source_column VARCHAR(100) NOT NULL,
    target_column VARCHAR(100) NOT NULL,
    data_type_source VARCHAR(50),
    data_type_target VARCHAR(50),
    is_nullable BOOLEAN DEFAULT TRUE,
    default_value VARCHAR(200),
    transformation_rule TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (mapping_id) REFERENCES table_mapping(mapping_id) ON DELETE CASCADE,
    INDEX idx_mapping_id (mapping_id),
    INDEX idx_source_column (source_column),
    INDEX idx_target_column (target_column)
);
```

### 2.3 값 변환 및 오류 관리

#### 2.3.1 value_mapping (값 매핑 정보)
```sql
CREATE TABLE value_mapping (
    value_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    source_value VARCHAR(500),
    target_value VARCHAR(500),
    mapping_type ENUM('EXACT', 'PATTERN', 'DEFAULT') DEFAULT 'EXACT',
    is_active BOOLEAN DEFAULT TRUE,
    usage_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_table_column (table_name, column_name),
    INDEX idx_source_value (source_value(100))
);
```

#### 2.3.2 migration_errors (이관 오류 정보)
```sql
CREATE TABLE migration_errors (
    error_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    step_id BIGINT,
    domain_name VARCHAR(50),
    source_table VARCHAR(100),
    error_type ENUM('READ_ERROR', 'TRANSFORM_ERROR', 'WRITE_ERROR', 'VALIDATION_ERROR') NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT NOT NULL,
    source_data JSON,
    stack_trace TEXT,
    retry_count INT DEFAULT 0,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP NULL,
    resolved_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (job_id) REFERENCES batch_jobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (step_id) REFERENCES batch_steps(step_id) ON DELETE SET NULL,
    INDEX idx_job_id (job_id),
    INDEX idx_step_id (step_id),
    INDEX idx_error_type (error_type),
    INDEX idx_source_table (source_table),
    INDEX idx_created_at (created_at)
);
```

### 2.4 성능 모니터링

#### 2.4.1 performance_metrics (성능 메트릭)
```sql
CREATE TABLE performance_metrics (
    metric_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    step_id BIGINT,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,4),
    metric_unit VARCHAR(20),
    measurement_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (job_id) REFERENCES batch_jobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (step_id) REFERENCES batch_steps(step_id) ON DELETE CASCADE,
    INDEX idx_job_id (job_id),
    INDEX idx_step_id (step_id),
    INDEX idx_metric_name (metric_name),
    INDEX idx_measurement_time (measurement_time)
);
```

## 📈 3. 초기 데이터 설정

### 3.1 도메인 매핑 초기 데이터
```sql
INSERT INTO domain_mapping (domain_name, domain_description, migration_order) VALUES
('USER', '사용자 관리 도메인', 1),
('PRODUCT', '상품 관리 도메인', 2),
('ORDER', '주문 관리 도메인', 3),
('PAYMENT', '결제 관리 도메인', 4),
('INVENTORY', '재고 관리 도메인', 5),
('DELIVERY', '배송 관리 도메인', 6),
('CUSTOMER', '고객 서비스 도메인', 7),
('MARKETING', '마케팅 도메인', 8),
('ANALYTICS', '분석 도메인', 9),
('SYSTEM', '시스템 관리 도메인', 10);
```

### 3.2 테이블 매핑 샘플 데이터
```sql
-- USER 도메인 테이블 매핑
INSERT INTO table_mapping (domain_id, source_table, target_table, table_description, migration_order, estimated_rows) VALUES
(1, '사용자', 'users', '사용자 기본 정보', 1, 100000),
(1, '사용자권한', 'user_permissions', '사용자 권한 정보', 2, 50000),
(1, '사용자로그', 'user_logs', '사용자 활동 로그', 3, 1000000);

-- PRODUCT 도메인 테이블 매핑
INSERT INTO table_mapping (domain_id, source_table, target_table, table_description, migration_order, estimated_rows) VALUES
(2, '상품', 'products', '상품 기본 정보', 1, 500000),
(2, '카테고리', 'categories', '상품 카테고리', 2, 1000),
(2, '상품옵션', 'product_options', '상품 옵션 정보', 3, 200000);
```

## 🔧 4. 인덱스 및 성능 최적화

### 4.1 복합 인덱스
```sql
-- 배치 작업 조회 최적화
CREATE INDEX idx_job_status_domain ON batch_jobs(status, domain_name, created_at);

-- 스텝 진행 상황 조회 최적화
CREATE INDEX idx_step_job_order ON batch_steps(job_id, step_order, status);

-- 오류 분석 최적화
CREATE INDEX idx_error_table_type ON migration_errors(source_table, error_type, created_at);

-- 성능 메트릭 조회 최적화
CREATE INDEX idx_metric_job_time ON performance_metrics(job_id, metric_name, measurement_time);
```

### 4.2 파티셔닝 (대용량 데이터 대비)
```sql
-- 성능 메트릭 테이블 월별 파티셔닝
ALTER TABLE performance_metrics 
PARTITION BY RANGE (YEAR(measurement_time) * 100 + MONTH(measurement_time)) (
    PARTITION p202407 VALUES LESS THAN (202408),
    PARTITION p202408 VALUES LESS THAN (202409),
    PARTITION p202409 VALUES LESS THAN (202410),
    PARTITION p202410 VALUES LESS THAN (202411),
    PARTITION p202411 VALUES LESS THAN (202412),
    PARTITION p202412 VALUES LESS THAN (202501),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 📊 5. 뷰 및 프로시저

### 5.1 배치 현황 조회 뷰
```sql
CREATE VIEW v_batch_summary AS
SELECT 
    j.job_id,
    j.job_name,
    j.domain_name,
    j.status,
    j.start_time,
    j.end_time,
    j.duration_seconds,
    j.total_tables,
    j.completed_tables,
    ROUND((j.completed_tables / j.total_tables) * 100, 2) as progress_percentage,
    j.processed_records,
    j.error_records,
    ROUND((j.processed_records / (j.processed_records + j.error_records)) * 100, 2) as success_rate
FROM batch_jobs j
WHERE j.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
```

### 5.2 도메인별 진행 상황 뷰
```sql
CREATE VIEW v_domain_progress AS
SELECT 
    dm.domain_name,
    dm.domain_description,
    COUNT(tm.mapping_id) as total_tables,
    COUNT(CASE WHEN tm.last_migrated_at IS NOT NULL THEN 1 END) as migrated_tables,
    ROUND((COUNT(CASE WHEN tm.last_migrated_at IS NOT NULL THEN 1 END) / COUNT(tm.mapping_id)) * 100, 2) as completion_rate,
    SUM(tm.estimated_rows) as total_estimated_rows,
    MAX(tm.last_migrated_at) as last_migration_time
FROM domain_mapping dm
LEFT JOIN table_mapping tm ON dm.domain_id = tm.domain_id
WHERE dm.is_active = TRUE AND tm.is_active = TRUE
GROUP BY dm.domain_id, dm.domain_name, dm.domain_description
ORDER BY dm.migration_order;
```

## 🚨 6. 모니터링 및 알림

### 6.1 배치 상태 모니터링 쿼리
```sql
-- 실행 중인 배치 작업 조회
SELECT job_id, job_name, domain_name, status, start_time, 
       TIMESTAMPDIFF(MINUTE, start_time, NOW()) as running_minutes
FROM batch_jobs 
WHERE status = 'RUNNING';

-- 최근 실패한 배치 작업 조회
SELECT job_id, job_name, domain_name, start_time, end_time, 
       (SELECT COUNT(*) FROM migration_errors WHERE job_id = bj.job_id) as error_count
FROM batch_jobs bj
WHERE status = 'FAILED' AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);
```

### 6.2 성능 임계값 모니터링
```sql
-- 처리 속도가 느린 스텝 조회
SELECT bs.step_name, bs.source_table, bs.read_count, bs.duration_seconds,
       ROUND(bs.read_count / bs.duration_seconds, 2) as records_per_second
FROM batch_steps bs
WHERE bs.status = 'COMPLETED' 
  AND bs.duration_seconds > 0
  AND (bs.read_count / bs.duration_seconds) < 100  -- 초당 100건 미만
ORDER BY records_per_second ASC;
```

이 데이터베이스 설계서는 600개 테이블을 200개로 이관하는 대규모 프로젝트에 최적화되어 있으며, 10개 도메인 기반의 체계적인 관리를 지원합니다.
