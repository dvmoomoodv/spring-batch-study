# MSSQL to MariaDB 데이터 이관 배치 프로그램

이 프로그램은 Spring Batch를 사용하여 MSSQL 데이터베이스에서 MariaDB로 데이터를 이관하는 배치 애플리케이션입니다.

## 주요 특징

- **청크 기반 처리**: 메모리 효율적인 대용량 데이터 처리
- **환경별 설정**: 개발(1:1), 운영(Master-Slave) 환경 지원
- **동적 설정**: 런타임에 청크 사이즈, 스킵 제한 등 조절 가능
- **상세한 로깅**: 진행 상황, 성능 지표, 오류 정보 등 포괄적 로깅
- **오류 처리**: 재시도, 스킵, 롤백 등 강력한 오류 복구 메커니즘
- **REST API**: 웹 인터페이스를 통한 배치 실행 및 모니터링

## 시스템 요구사항

- Java 17 이상
- Spring Boot 3.2.0
- MSSQL Server (소스 DB)
- MariaDB (타겟 DB)

## 설정

### 1. 데이터베이스 설정

`src/main/resources/application.yml` 파일에서 데이터베이스 연결 정보를 수정하세요:

```yaml
# 개발 환경
spring:
  profiles:
    active: dev
  datasource:
    source:  # MSSQL
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=sourceDB
      username: sa
      password: yourPassword
    target:  # MariaDB
      jdbc-url: jdbc:mariadb://localhost:3306/targetDB
      username: root
      password: yourPassword

# 운영 환경
spring:
  profiles:
    active: prod
  datasource:
    source:  # MSSQL
      jdbc-url: jdbc:sqlserver://prod-mssql-server:1433;databaseName=sourceDB
    target:  # MariaDB Master
      jdbc-url: jdbc:mariadb://prod-mariadb-master:3306/targetDB
    target-slave:  # MariaDB Slave
      jdbc-url: jdbc:mariadb://prod-mariadb-slave:3306/targetDB
```

### 2. 배치 설정

```yaml
batch:
  chunk-size: 1000  # 청크 사이즈 (조절 가능)
  skip-limit: 100   # 오류 허용 개수
  retry-limit: 3    # 재시도 횟수
```

## 빌드 및 실행

### 1. 빌드

```bash
./gradlew build
```

### 2. 실행

#### 개발 환경에서 실행
```bash
java -jar build/libs/batch-migration-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

#### 운영 환경에서 실행
```bash
java -jar build/libs/batch-migration-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

#### 청크 사이즈 조절하여 실행
```bash
java -jar build/libs/batch-migration-1.0-SNAPSHOT.jar --batch.chunk-size=500
```

## REST API 사용법

애플리케이션이 실행되면 다음 REST API를 사용할 수 있습니다:

### 1. 전체 데이터 이관 시작
```bash
curl -X POST "http://localhost:8080/api/migration/start"
```

### 2. 청크 사이즈를 조절하여 이관 시작
```bash
curl -X POST "http://localhost:8080/api/migration/start?chunkSize=500&skipLimit=50"
```

### 3. 특정 테이블만 이관
```bash
curl -X POST "http://localhost:8080/api/migration/table/users"
```

### 4. 조건부 테이블 이관
```bash
curl -X POST "http://localhost:8080/api/migration/table/orders?whereClause=created_date>='2024-01-01'"
```

### 5. 현재 설정 조회
```bash
curl -X GET "http://localhost:8080/api/migration/config"
```

### 6. 설정 동적 변경
```bash
curl -X PUT "http://localhost:8080/api/migration/config?chunkSize=2000&skipLimit=200"
```

## 로그 모니터링

### 로그 파일 위치
- **일반 로그**: `logs/batch-migration.log`
- **배치 처리 로그**: `logs/batch-processing.log`
- **에러 로그**: `logs/batch-errors.log`
- **성능 로그**: `logs/batch-performance.log`

### 실시간 로그 모니터링
```bash
# 전체 로그 모니터링
tail -f logs/batch-migration.log

# 배치 처리 로그만 모니터링
tail -f logs/batch-processing.log

# 에러 로그만 모니터링
tail -f logs/batch-errors.log
```

## 성능 튜닝 가이드

### 1. 청크 사이즈 조절
- **소량 데이터**: 100-500
- **중간 데이터**: 1000-2000
- **대용량 데이터**: 2000-5000

### 2. 커넥션 풀 설정
```yaml
spring:
  datasource:
    source:
      hikari:
        maximum-pool-size: 20
        minimum-idle: 10
```

### 3. JVM 옵션
```bash
java -Xms2g -Xmx4g -XX:+UseG1GC -jar batch-migration.jar
```

## 문제 해결

### 1. 메모리 부족
- 청크 사이즈를 줄이세요
- JVM 힙 메모리를 늘리세요

### 2. 처리 속도 저하
- 청크 사이즈를 늘리세요
- 데이터베이스 인덱스를 확인하세요
- 커넥션 풀 크기를 늘리세요

### 3. 데이터 오류
- 스킵 제한을 늘리세요
- 소스 데이터 품질을 확인하세요
- 타겟 DB 제약조건을 확인하세요

## 커스터마이징

### 1. 새로운 테이블 추가
`DataMigrationJobConfig.java`에서 새로운 Step을 추가하세요:

```java
@Bean
public Job dataMigrationJob() {
    return new JobBuilder("dataMigrationJob", jobRepository)
            .start(migrationStep("users", null))
            .next(migrationStep("orders", null))
            .next(migrationStep("your_new_table", null))  // 새 테이블 추가
            .build();
}
```

### 2. 데이터 변환 로직 수정
`DataTransformProcessor.java`에서 변환 로직을 수정하세요.

### 3. 커스텀 검증 로직 추가
`DataTransformProcessor.java`의 `validateData` 메서드를 수정하세요.
