# Docker 기반 데이터 이관 테스트 완전 가이드

이 가이드는 MSSQL에서 MariaDB로의 데이터 이관 배치 프로그램을 Docker 환경에서 처음부터 끝까지 실행하는 방법을 단계별로 상세히 설명합니다.

## 📋 사전 요구사항

- **Docker Desktop** 설치 (최신 버전)
- **Docker Compose** 설치
- **Java 17** 설치
- **최소 8GB RAM** (운영환경 테스트 시 16GB 권장)
- **Apple Silicon (M1/M2) Mac** 사용자는 ARM64 호환 이미지 사용

## 🎯 전체 실행 플로우

```
1. Docker 환경 구성 → 2. 데이터베이스 초기화 → 3. 샘플 데이터 확인 →
4. 애플리케이션 빌드 → 5. 배치 실행 → 6. 데이터 이관 확인 → 7. 결과 검증
```

## 🚀 Step 1: 개발 환경 구성 및 실행

### 1.1 프로젝트 디렉토리 확인
```bash
# 프로젝트 루트에서 디렉토리 구조 확인
ls -la docker/dev/
# 다음 파일들이 있어야 합니다:
# - docker-compose.yml (기본 환경용)
# - docker-compose-arm64.yml (ARM64/Apple Silicon용)
# - start-arm64.sh (ARM64 자동 실행 스크립트)
# - init-scripts/
# - init-mssql.sh
```

### 1.2 환경별 컨테이너 시작 방법

#### 🍎 ARM64 환경 (Apple Silicon M1/M2 Mac) - 권장
```bash
# 개발환경 디렉토리로 이동
cd docker/dev

# ARM64 전용 자동 실행 스크립트 사용 (가장 간단)
./start-arm64.sh

# 또는 수동으로 ARM64 compose 파일 사용
docker-compose -f docker-compose-arm64.yml down -v  # 기존 정리
docker-compose -f docker-compose-arm64.yml up -d    # 시작

# 실행 상태 확인
docker-compose -f docker-compose-arm64.yml ps
```

**ARM64 환경 예상 출력:**
```
NAME                COMMAND                  SERVICE             STATUS              PORTS
adminer-dev         "entrypoint.sh docke…"   adminer             running             0.0.0.0:8082->8080/tcp
mariadb-dev         "docker-entrypoint.s…"   mariadb-dev         running (healthy)   0.0.0.0:3306->3306/tcp
mssql-dev           "/opt/mssql/bin/perm…"   mssql-dev           running (healthy)   0.0.0.0:1433->1433/tcp
mysql-client-dev    "docker-entrypoint.s…"   mysql-client        running
```

#### 💻 Intel/AMD64 환경
```bash
# 개발환경 디렉토리로 이동
cd docker/dev

# 기존 컨테이너가 있다면 정리
docker-compose down -v

# 기본 compose 파일로 컨테이너 시작
docker-compose up -d

# 실행 상태 확인
docker-compose ps
```

**Intel 환경 예상 출력:**
```
NAME                COMMAND                  SERVICE             STATUS              PORTS
adminer-dev         "entrypoint.sh docke…"   adminer             running             0.0.0.0:8082->8080/tcp
mariadb-dev         "docker-entrypoint.s…"   mariadb-dev         running (healthy)   0.0.0.0:3306->3306/tcp
mssql-dev           "/opt/mssql/bin/perm…"   mssql-dev           running (healthy)   0.0.0.0:1433->1433/tcp
phpmyadmin-dev      "/docker-entrypoint.…"   phpmyadmin          running             0.0.0.0:8081->80/tcp
```

**예상 출력:**
```
NAME                COMMAND                  SERVICE             STATUS              PORTS
adminer-dev         "entrypoint.sh docke…"   adminer             running             0.0.0.0:8082->8080/tcp
mariadb-dev         "docker-entrypoint.s…"   mariadb-dev         running (healthy)   0.0.0.0:3306->3306/tcp
mssql-dev           "/opt/mssql/bin/perm…"   mssql-dev           running (healthy)   0.0.0.0:1433->1433/tcp
phpmyadmin-dev      "/docker-entrypoint.…"   phpmyadmin          running             0.0.0.0:8081->80/tcp
```

### 1.3 컨테이너 로그 확인

#### ARM64 환경
```bash
# 모든 컨테이너 로그 확인
docker-compose -f docker-compose-arm64.yml logs

# 특정 컨테이너 로그 실시간 확인
docker-compose -f docker-compose-arm64.yml logs -f mssql-dev
docker-compose -f docker-compose-arm64.yml logs -f mariadb-dev
```

#### Intel 환경
```bash
# 모든 컨테이너 로그 확인
docker-compose logs

# 특정 컨테이너 로그 실시간 확인
docker-compose logs -f mssql-dev
docker-compose logs -f mariadb-dev
```

### 1.4 데이터베이스 연결 확인
```bash
# MSSQL 연결 테스트 (Azure SQL Edge - ARM64 호환)
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -Q "SELECT @@VERSION"

# MariaDB 연결 테스트
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -e "SELECT VERSION();"

# 네트워크 연결 확인
docker exec -it mariadb-dev ping mssql-dev
```

**성공 시 예상 출력:**
- **MSSQL**: `Microsoft SQL Azure Edge (RTM) - 15.0.2000.1573 (ARM64)`
- **MariaDB**: `mariadb Ver 15.1 Distrib 11.2.x-MariaDB`
- **네트워크**: `PING mssql-dev (172.x.x.x): 56 data bytes`

## 🗄️ Step 2: 데이터베이스 초기화 및 샘플 데이터 생성

### 2.1 MSSQL 데이터베이스 초기화

#### 🍎 ARM64 환경 (Azure SQL Edge) - sqlcmd 문제 해결

**문제**: Azure SQL Edge에는 sqlcmd가 기본 설치되지 않음

**해결방법 1: 간단한 Python 방식 (권장)**
```bash
# Python을 사용한 간단 초기화 (sqlcmd 없이)
./init-mssql-simple.sh
```

**해결방법 2: sqlcmd 설치 후 사용**
```bash
# 1단계: sqlcmd 설치
./install-sqlcmd.sh

# 2단계: 설치 성공 시 초기화
docker exec -it mssql-dev /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P DevPassword123! -C -i /docker-entrypoint-initdb.d/01-create-database.sql
docker exec -it mssql-dev /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P DevPassword123! -C -i /docker-entrypoint-initdb.d/02-insert-sample-data.sql
```

**해결방법 3: 수동 Python 실행**
```bash
# 컨테이너 내부에서 Python으로 직접 실행
docker exec -it mssql-dev bash
# 컨테이너 내부에서:
apt-get update && apt-get install -y python3 python3-pip
pip3 install pyodbc
python3 << 'EOF'
import pyodbc
conn = pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=master;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes')
cursor = conn.cursor()
cursor.execute("CREATE DATABASE sourceDB")
conn.commit()
print("데이터베이스 생성 완료")
conn.close()
EOF
```

#### 💻 Intel 환경 (MSSQL Server)
```bash
# Intel 환경에서는 기본 sqlcmd 사용
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/01-create-database.sql
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/02-insert-sample-data.sql
```

### 2.2 MSSQL 샘플 데이터 상세 확인

#### 2.2.1 데이터베이스 및 테이블 구조 확인
```bash
# 데이터베이스 목록 확인
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -Q "SELECT name FROM sys.databases"

# sourceDB 사용 및 테이블 목록 확인
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -d sourceDB -Q "
SELECT
    TABLE_NAME as '테이블명',
    TABLE_TYPE as '타입'
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME"
```

#### 2.2.2 각 테이블의 레코드 수 확인

##### 🍎 ARM64 환경 (Python 사용)
```bash
# Python을 사용한 레코드 수 확인
docker exec -it mssql-dev python3 << 'EOF'
import pyodbc
try:
    conn = pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=sourceDB;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes')
    cursor = conn.cursor()

    tables = ['사용자', '상품', '주문', '주문상세', '카테고리', '리뷰', '공지사항', '쿠폰', '배송', '문의']
    print("테이블명    레코드수")
    print("-" * 20)

    for table in tables:
        try:
            cursor.execute(f"SELECT COUNT(*) FROM [{table}]")
            count = cursor.fetchone()[0]
            print(f"{table:<10} {count}")
        except Exception as e:
            print(f"{table:<10} 오류: {e}")

    conn.close()
except Exception as e:
    print(f"연결 오류: {e}")
EOF
```

##### 💻 Intel 환경 (sqlcmd 사용)
```bash
# sqlcmd를 사용한 레코드 수 확인
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -d sourceDB -Q "
SELECT
    '사용자' as 테이블명, COUNT(*) as 레코드수 FROM 사용자
UNION ALL SELECT '상품', COUNT(*) FROM 상품
UNION ALL SELECT '주문', COUNT(*) FROM 주문
UNION ALL SELECT '주문상세', COUNT(*) FROM 주문상세
UNION ALL SELECT '카테고리', COUNT(*) FROM 카테고리
UNION ALL SELECT '리뷰', COUNT(*) FROM 리뷰
UNION ALL SELECT '공지사항', COUNT(*) FROM 공지사항
UNION ALL SELECT '쿠폰', COUNT(*) FROM 쿠폰
UNION ALL SELECT '배송', COUNT(*) FROM 배송
UNION ALL SELECT '문의', COUNT(*) FROM 문의
ORDER BY 테이블명"
```

**예상 출력:**
```
테이블명    레코드수
공지사항    3
문의        3
배송        3
사용자      8
상품        10
쿠폰        3
카테고리    9
리뷰        4
주문        5
주문상세    8
```

#### 2.2.3 실제 데이터 샘플 확인 (이관 전 상태)

##### 🍎 ARM64 환경 (Python 사용)
```bash
# 사용자 테이블 샘플 데이터 확인
docker exec -it mssql-dev python3 << 'EOF'
import pyodbc
try:
    conn = pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=sourceDB;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes')
    cursor = conn.cursor()

    print("=== 사용자 테이블 샘플 (한글 컬럼명과 값) ===")
    cursor.execute("SELECT TOP 3 사용자ID, 이름, 이메일, 성별, 직업, 활성여부 FROM 사용자 ORDER BY 사용자ID")
    for row in cursor.fetchall():
        print(f"ID: {row[0]}, 이름: {row[1]}, 이메일: {row[2]}, 성별: {row[3]}, 직업: {row[4]}, 활성: {row[5]}")

    print("\n=== 상품 테이블 샘플 ===")
    cursor.execute("SELECT TOP 3 상품ID, 상품명, 카테고리, 가격, 판매상태 FROM 상품 ORDER BY 상품ID")
    for row in cursor.fetchall():
        print(f"ID: {row[0]}, 상품명: {row[1]}, 카테고리: {row[2]}, 가격: {row[3]}, 상태: {row[4]}")

    print("\n=== 주문 테이블 샘플 (상태값 확인) ===")
    cursor.execute("SELECT TOP 3 주문ID, 주문번호, 주문상태, 결제방법, 총금액 FROM 주문 ORDER BY 주문ID")
    for row in cursor.fetchall():
        print(f"ID: {row[0]}, 번호: {row[1]}, 상태: {row[2]}, 결제: {row[3]}, 금액: {row[4]}")

    conn.close()
except Exception as e:
    print(f"오류: {e}")
EOF
```

##### 💻 Intel 환경 (sqlcmd 사용)
```bash
# 사용자 테이블 샘플 데이터 (한글 컬럼명과 값)
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -d sourceDB -Q "
SELECT TOP 3
    사용자ID,
    이름,
    이메일,
    성별,
    직업,
    활성여부
FROM 사용자
ORDER BY 사용자ID"

# 상품 테이블 샘플 데이터
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -d sourceDB -Q "
SELECT TOP 3
    상품ID,
    상품명,
    카테고리,
    가격,
    판매상태
FROM 상품
ORDER BY 상품ID"

# 주문 테이블 샘플 데이터 (상태값 확인)
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -d sourceDB -Q "
SELECT TOP 3
    주문ID,
    주문번호,
    주문상태,
    결제방법,
    총금액
FROM 주문
ORDER BY 주문ID"
```

**예상 출력 (사용자 테이블):**
```
사용자ID  이름    이메일                  성별  직업    활성여부
1        김철수   kim.cs@example.com     남성  개발자   1
2        이영희   lee.yh@example.com     여성  디자이너  1
3        박민수   park.ms@example.com    남성  마케터   1
```

**예상 출력 (주문 테이블):**
```
주문ID  주문번호        주문상태  결제방법  총금액
1      ORD-2024-001   배송완료  신용카드  934000.00
2      ORD-2024-002   배송중    계좌이체  174000.00
3      ORD-2024-003   주문접수  신용카드  2499000.00
```

### 2.3 MariaDB 타겟 테이블 상세 확인 (이관 전 빈 상태)

#### 2.3.1 타겟 데이터베이스 구조 확인
```bash
# MariaDB 데이터베이스 목록 확인
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -e "SHOW DATABASES;"

# targetDB의 테이블 목록 확인 (영어 테이블명)
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    TABLE_NAME as '테이블명(영어)',
    TABLE_ROWS as '현재레코드수',
    TABLE_COMMENT as '설명'
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'targetDB'
AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;"
```

**예상 출력:**
```
테이블명(영어)    현재레코드수  설명
categories       0
coupons          0
deliveries       0
inquiries        0
notices          0
order_details    0
orders           0
products         0
reviews          0
users            0
```

#### 2.3.2 주요 테이블 구조 확인 (영어 컬럼명)
```bash
# 사용자 테이블 구조 (한글 → 영어 매핑 확인)
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
DESCRIBE users;"

# 상품 테이블 구조
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
DESCRIBE products;"

# 주문 테이블 구조
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
DESCRIBE orders;"
```

**예상 출력 (users 테이블):**
```
Field           Type         Null  Key  Default             Extra
user_id         int(11)      NO    PRI  NULL                auto_increment
name            varchar(50)  NO         NULL
email           varchar(100) NO    UNI  NULL
phone_number    varchar(20)  YES        NULL
address         varchar(200) YES        NULL
birth_date      date         YES        NULL
gender          varchar(10)  YES        NULL
occupation      varchar(50)  YES        NULL
created_at      timestamp    NO         CURRENT_TIMESTAMP
updated_at      timestamp    NO         CURRENT_TIMESTAMP   on update CURRENT_TIMESTAMP
is_active       tinyint(1)   YES        1
migrated_at     timestamp    YES        NULL
```

#### 2.3.3 빈 테이블 상태 확인
```bash
# 모든 테이블이 비어있는지 확인
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT 'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_details', COUNT(*) FROM order_details
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'reviews', COUNT(*) FROM reviews
UNION ALL SELECT 'notices', COUNT(*) FROM notices
UNION ALL SELECT 'coupons', COUNT(*) FROM coupons
UNION ALL SELECT 'deliveries', COUNT(*) FROM deliveries
UNION ALL SELECT 'inquiries', COUNT(*) FROM inquiries
ORDER BY table_name;"
```

**예상 출력 (이관 전 - 모든 테이블이 비어있음):**
```
table_name      record_count
categories      0
coupons         0
deliveries      0
inquiries       0
notices         0
order_details   0
orders          0
products        0
reviews         0
users           0
```

### 2.4 웹 관리 도구 접속 및 확인
- **phpMyAdmin (MariaDB)**: http://localhost:8081
  - 사용자: root
  - 비밀번호: DevPassword123!
  - 데이터베이스: targetDB 선택

- **Adminer (MSSQL)**: http://localhost:8082
  - 시스템: MS SQL (Server) 선택
  - 서버: mssql-dev
  - 사용자: sa
  - 비밀번호: DevPassword123!
  - 데이터베이스: sourceDB 선택

**웹 UI에서 확인할 내용:**
1. MSSQL에서 한글 테이블명과 데이터 확인
2. MariaDB에서 영어 테이블명과 빈 테이블 확인

## 🏗️ Step 3: 애플리케이션 빌드 및 설정

### 3.1 프로젝트 빌드
```bash
# 프로젝트 루트 디렉토리로 이동
cd /Users/dvmoomoodv/IdeaProjects/test

# Gradle 빌드 실행
./gradlew clean build

# 빌드 성공 확인
ls -la build/libs/
# batch-migration-1.0-SNAPSHOT.jar 파일이 생성되어야 함
```

**빌드 성공 시 예상 출력:**
```
BUILD SUCCESSFUL in 45s
7 actionable tasks: 7 executed
```

### 3.2 애플리케이션 설정 확인
```bash
# application.yml 설정 확인
cat src/main/resources/application.yml | grep -A 20 "spring:"
```

### 3.3 테스트 실행
```bash
# 단위 테스트 실행
./gradlew test

# 테스트 결과 확인
open build/reports/tests/test/index.html
```

## 🚀 Step 4: 배치 애플리케이션 실행

### 4.1 개발환경에서 애플리케이션 시작
```bash
# 개발 프로파일로 애플리케이션 시작
java -jar build/libs/batch-migration-1.0-SNAPSHOT.jar --spring.profiles.active=dev &

# 또는 스크립트 사용
./scripts/run-migration.sh --profile dev --chunk-size 100
```

### 4.2 애플리케이션 시작 확인
```bash
# 애플리케이션 로그 확인
tail -f logs/batch-migration.log

# 애플리케이션 상태 확인 (다른 터미널에서)
curl -X GET "http://localhost:8080/actuator/health"
```

**성공 시 예상 출력:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

### 4.3 배치 설정 확인
```bash
# 현재 배치 설정 조회
curl -X GET "http://localhost:8080/api/migration/config"
```

**예상 출력:**
```json
{
  "chunkSize": 1000,
  "skipLimit": 100,
  "retryLimit": 3
}
```

## 📊 Step 5: 데이터 이관 실행

### 5.1 전체 데이터 이관 시작
```bash
# 기본 설정으로 전체 이관 시작
curl -X POST "http://localhost:8080/api/migration/start"

# 또는 청크 사이즈를 조절하여 실행
curl -X POST "http://localhost:8080/api/migration/start?chunkSize=100&skipLimit=50"
```

**성공 시 예상 출력:**
```json
{
  "success": true,
  "jobExecutionId": 1,
  "jobInstanceId": 1,
  "status": "STARTED",
  "startTime": "2024-07-01T10:30:00",
  "message": "Migration job started successfully"
}
```

### 5.2 개별 테이블 이관 테스트
```bash
# 사용자 테이블만 이관
curl -X POST "http://localhost:8080/api/migration/table/사용자"

# 상품 테이블만 이관
curl -X POST "http://localhost:8080/api/migration/table/상품"

# 조건부 이관 (예: 특정 날짜 이후 주문)
curl -X POST "http://localhost:8080/api/migration/table/주문?whereClause=주문일시>='2024-01-01'"
```

### 5.3 실시간 진행 상황 모니터링
```bash
# 별도 터미널에서 로그 실시간 확인
tail -f logs/batch-migration.log

# 배치 처리 상세 로그
tail -f logs/batch-processing.log

# 에러 로그만 확인
tail -f logs/batch-errors.log
```

**로그에서 확인할 수 있는 정보:**
```
2024-07-01 10:30:15.123 [main] INFO  [c.e.b.j.DataMigrationJobConfig] - Creating migration step for table: 사용자 with chunk size: 100
2024-07-01 10:30:15.456 [main] INFO  [c.e.b.r.DatabaseItemReader] - Creating ItemReader for table: 사용자 with SQL: SELECT * FROM 사용자
2024-07-01 10:30:16.789 [main] INFO  [c.e.b.p.DataTransformProcessor] - Processed 100 records, errors: 0 for table: 사용자
2024-07-01 10:30:17.012 [main] INFO  [c.e.b.w.DatabaseItemWriter] - Successfully wrote 100 records to table: users, Total written: 100
```

## 🔍 Step 6: 데이터 이관 결과 상세 확인

### 6.1 이관 후 테이블 및 레코드 수 확인

#### 6.1.1 전체 테이블 레코드 수 비교
```bash
# 이관 후 MariaDB 테이블 레코드 수 확인
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    TABLE_NAME as '테이블명(영어)',
    TABLE_ROWS as '레코드수',
    CREATE_TIME as '생성시간'
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'targetDB'
AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;"

# 정확한 레코드 수 확인 (TABLE_ROWS는 근사치일 수 있음)
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT 'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_details', COUNT(*) FROM order_details
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'reviews', COUNT(*) FROM reviews
UNION ALL SELECT 'notices', COUNT(*) FROM notices
UNION ALL SELECT 'coupons', COUNT(*) FROM coupons
UNION ALL SELECT 'deliveries', COUNT(*) FROM deliveries
UNION ALL SELECT 'inquiries', COUNT(*) FROM inquiries
ORDER BY table_name;"
```

**예상 출력 (이관 성공 시):**
```
table_name      record_count
categories      9
coupons         3
deliveries      3
inquiries       3
notices         3
order_details   8
orders          5
products        10
reviews         4
users           8
```

#### 6.1.2 소스와 타겟 레코드 수 비교 검증
```bash
# 소스(MSSQL)와 타겟(MariaDB) 레코드 수 동시 비교
echo "=== 데이터 이관 정합성 검증 ==="
echo ""
echo "📊 소스 데이터 (MSSQL - 한글 테이블):"
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -d sourceDB -Q "
SELECT '사용자' as 테이블명, COUNT(*) as 레코드수 FROM 사용자
UNION ALL SELECT '상품', COUNT(*) FROM 상품
UNION ALL SELECT '주문', COUNT(*) FROM 주문
UNION ALL SELECT '주문상세', COUNT(*) FROM 주문상세
UNION ALL SELECT '카테고리', COUNT(*) FROM 카테고리
ORDER BY 테이블명" -h -1

echo ""
echo "📊 타겟 데이터 (MariaDB - 영어 테이블):"
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT 'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_details', COUNT(*) FROM order_details
UNION ALL SELECT 'categories', COUNT(*) FROM categories
ORDER BY table_name"
```

**예상 출력:**
```
=== 데이터 이관 정합성 검증 ===

📊 소스 데이터 (MSSQL - 한글 테이블):
카테고리    9
사용자      8
상품        10
주문        5
주문상세    8

📊 타겟 데이터 (MariaDB - 영어 테이블):
categories      9
order_details   8
orders          5
products        10
users           8
```

### 6.2 데이터 변환 결과 상세 확인

#### 6.2.1 사용자 테이블 변환 결과 (한글 → 영어)
```bash
# 사용자 테이블 전체 데이터 확인 (컬럼명과 값 변환 확인)
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    user_id as '사용자ID',
    name as '이름',
    email as '이메일',
    gender as '성별(변환됨)',
    occupation as '직업',
    is_active as '활성여부',
    DATE_FORMAT(migrated_at, '%Y-%m-%d %H:%i:%s') as '이관시점'
FROM users
ORDER BY user_id;"

# 성별 변환 확인 (남성→MALE, 여성→FEMALE)
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    gender as '변환된_성별',
    COUNT(*) as '개수'
FROM users
GROUP BY gender
ORDER BY gender;"
```

**예상 출력 (사용자 테이블):**
```
사용자ID | 이름    | 이메일                  | 성별(변환됨) | 직업     | 활성여부 | 이관시점
1       | 김철수   | kim.cs@example.com     | MALE        | 개발자    | 1       | 2024-07-01 10:30:17
2       | 이영희   | lee.yh@example.com     | FEMALE      | 디자이너  | 1       | 2024-07-01 10:30:17
3       | 박민수   | park.ms@example.com    | MALE        | 마케터    | 1       | 2024-07-01 10:30:17
```

**성별 변환 결과:**
```
변환된_성별  개수
FEMALE      4
MALE        4
```

#### 6.2.2 주문 테이블 상태값 변환 확인
```bash
# 주문 테이블 상태값 변환 결과 확인
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    order_id as '주문ID',
    order_number as '주문번호',
    order_status as '주문상태(변환됨)',
    payment_method as '결제방법',
    total_amount as '총금액',
    DATE_FORMAT(migrated_at, '%Y-%m-%d %H:%i:%s') as '이관시점'
FROM orders
ORDER BY order_id;"

# 주문상태 변환 통계 (배송완료→DELIVERED, 배송중→SHIPPING 등)
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    order_status as '변환된_주문상태',
    COUNT(*) as '개수'
FROM orders
GROUP BY order_status
ORDER BY order_status;"
```

**예상 출력 (주문 테이블):**
```
주문ID | 주문번호        | 주문상태(변환됨) | 결제방법  | 총금액      | 이관시점
1     | ORD-2024-001   | DELIVERED       | 신용카드   | 934000.00  | 2024-07-01 10:30:18
2     | ORD-2024-002   | SHIPPING        | 계좌이체   | 174000.00  | 2024-07-01 10:30:18
3     | ORD-2024-003   | ORDER_RECEIVED  | 신용카드   | 2499000.00 | 2024-07-01 10:30:18
```

**주문상태 변환 결과:**
```
변환된_주문상태    개수
DELIVERED        2
ORDER_RECEIVED   1
PREPARING        1
SHIPPING         1
```

#### 6.2.3 상품 테이블 판매상태 변환 확인
```bash
# 상품 테이블 판매상태 변환 확인
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    product_id as '상품ID',
    product_name as '상품명',
    category as '카테고리',
    price as '가격',
    sales_status as '판매상태(변환됨)',
    stock_quantity as '재고수량',
    DATE_FORMAT(migrated_at, '%Y-%m-%d %H:%i:%s') as '이관시점'
FROM products
ORDER BY product_id
LIMIT 5;"

# 판매상태 변환 통계 (판매중→ON_SALE 등)
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT
    sales_status as '변환된_판매상태',
    COUNT(*) as '개수'
FROM products
GROUP BY sales_status;"
```

**예상 출력 (상품 테이블):**
```
상품ID | 상품명           | 카테고리  | 가격      | 판매상태(변환됨) | 재고수량 | 이관시점
1     | 갤럭시 스마트폰   | 스마트폰  | 899000.00 | ON_SALE         | 50      | 2024-07-01 10:30:19
2     | 아이폰 프로       | 스마트폰  | 1299000.00| ON_SALE         | 30      | 2024-07-01 10:30:19
3     | 맥북 프로         | 노트북    | 2499000.00| ON_SALE         | 20      | 2024-07-01 10:30:19
```

#### 6.2.4 migrated_at 컬럼 확인 (이관 시점 추적)
```bash
# 모든 테이블의 이관 시점 확인
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT 'users' as table_name,
       MIN(migrated_at) as first_migrated,
       MAX(migrated_at) as last_migrated,
       COUNT(*) as total_records
FROM users WHERE migrated_at IS NOT NULL
UNION ALL
SELECT 'products', MIN(migrated_at), MAX(migrated_at), COUNT(*)
FROM products WHERE migrated_at IS NOT NULL
UNION ALL
SELECT 'orders', MIN(migrated_at), MAX(migrated_at), COUNT(*)
FROM orders WHERE migrated_at IS NOT NULL;"
```

**예상 출력:**
```
table_name | first_migrated      | last_migrated       | total_records
users      | 2024-07-01 10:30:17 | 2024-07-01 10:30:17 | 8
products   | 2024-07-01 10:30:19 | 2024-07-01 10:30:19 | 10
orders     | 2024-07-01 10:30:18 | 2024-07-01 10:30:18 | 5
```

### 6.3 웹 UI에서 결과 확인

#### 6.3.1 환경별 웹 관리 도구 접속

##### 🍎 ARM64 환경 (Apple Silicon)
**Adminer 사용** (phpMyAdmin 대신)
1. **Adminer 접속**: http://localhost:8082
2. **MSSQL 접속 (소스 확인)**:
   - 시스템: `MS SQL (beta)`
   - 서버: `mssql-dev`
   - 사용자: `sa`
   - 비밀번호: `DevPassword123!`
   - 데이터베이스: `sourceDB`

3. **MariaDB 접속 (타겟 확인)**:
   - 시스템: `MySQL`
   - 서버: `mariadb-dev`
   - 사용자: `root`
   - 비밀번호: `DevPassword123!`
   - 데이터베이스: `targetDB`

##### 💻 Intel 환경
**phpMyAdmin + Adminer 사용**
1. **phpMyAdmin (MariaDB)**: http://localhost:8081
   - 사용자: `root`
   - 비밀번호: `DevPassword123!`
   - 데이터베이스: `targetDB`

2. **Adminer (MSSQL)**: http://localhost:8082
   - 시스템: `MS SQL (beta)`
   - 서버: `mssql-dev`
   - 사용자: `sa`
   - 비밀번호: `DevPassword123!`

#### 6.3.2 웹 UI에서 확인할 주요 포인트

**✅ 이관 전 확인사항 (MSSQL - sourceDB)**
- 테이블명: `사용자`, `상품`, `주문` (한글)
- 컬럼명: `사용자ID`, `이름`, `성별` (한글)
- 값: `남성`, `여성`, `배송완료`, `판매중` (한글)

**✅ 이관 후 확인사항 (MariaDB - targetDB)**
- 테이블명: `users`, `products`, `orders` (영어)
- 컬럼명: `user_id`, `name`, `gender` (영어)
- 값: `MALE`, `FEMALE`, `DELIVERED`, `ON_SALE` (영어)
- 추가 컬럼: `migrated_at` (이관 시점 기록)

#### 6.3.3 웹 UI 단계별 확인 가이드

**Step 1: 소스 데이터 확인 (MSSQL)**
1. Adminer에서 MSSQL 접속
2. `sourceDB` → `사용자` 테이블 클릭
3. 한글 컬럼명과 값 확인: `성별` 컬럼에 `남성`, `여성` 값 확인
4. `주문` 테이블에서 `주문상태` 컬럼의 `배송완료`, `배송중` 값 확인

**Step 2: 타겟 데이터 확인 (MariaDB)**
1. Adminer에서 MariaDB 접속 (또는 phpMyAdmin)
2. `targetDB` → `users` 테이블 클릭
3. 영어 컬럼명과 변환된 값 확인: `gender` 컬럼에 `MALE`, `FEMALE` 값 확인
4. `orders` 테이블에서 `order_status` 컬럼의 `DELIVERED`, `SHIPPING` 값 확인
5. `migrated_at` 컬럼에 이관 시점이 기록되었는지 확인

**Step 3: 데이터 정합성 확인**
- 소스와 타겟의 레코드 수가 일치하는지 확인
- 각 테이블의 데이터가 올바르게 변환되었는지 확인
- 외래키 관계가 유지되었는지 확인 (예: `orders.user_id` → `users.user_id`)

## 📊 Step 7: 성능 및 통계 확인

### 7.1 배치 실행 통계 확인
```bash
# 배치 실행 통계 API 호출
curl -X GET "http://localhost:8080/actuator/metrics/batch.job.duration"

# 처리된 레코드 수 확인
curl -X GET "http://localhost:8080/actuator/metrics/batch.item.read"
curl -X GET "http://localhost:8080/actuator/metrics/batch.item.write"
```

### 7.2 성능 로그 분석
```bash
# 성능 로그 확인
tail -20 logs/batch-performance.log

# 처리 속도 계산 (예시)
grep "Processing Rate" logs/batch-performance.log | tail -5
```

**성능 로그 예시:**
```
2024-07-01 10:30:20.123 - === Migration completed for table: users ===
2024-07-01 10:30:20.124 - Duration: 5 seconds
2024-07-01 10:30:20.125 - Records Read: 8
2024-07-01 10:30:20.126 - Records Written: 8
2024-07-01 10:30:20.127 - Processing Rate: 1 records/second
2024-07-01 10:30:20.128 - Success Rate: 100.00%
```

### 7.3 데이터 정합성 검증
```bash
# 소스와 타겟 레코드 수 비교 스크립트 생성
cat > verify-migration.sh << 'EOF'
#!/bin/bash

echo "=== 데이터 이관 정합성 검증 ==="

# MSSQL 레코드 수 조회
echo "📊 소스 데이터 (MSSQL):"
docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -d sourceDB -Q "
SELECT '사용자' as 테이블명, COUNT(*) as 레코드수 FROM 사용자
UNION ALL SELECT '상품', COUNT(*) FROM 상품
UNION ALL SELECT '주문', COUNT(*) FROM 주문" -h -1

echo ""
echo "📊 타겟 데이터 (MariaDB):"
docker exec -it mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT 'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'orders', COUNT(*) FROM orders"

echo ""
echo "✅ 검증 완료"
EOF

chmod +x verify-migration.sh
./verify-migration.sh
```

## 🔧 Step 8: 문제 해결 및 디버깅

### 8.1 환경별 일반적인 문제와 해결방법

| 문제 | 증상 | ARM64 해결방법 | Intel 해결방법 |
|------|------|----------------|----------------|
| **컨테이너 시작 실패** | `docker-compose up` 실패 | `docker-compose -f docker-compose-arm64.yml down -v` 후 재시작 | `docker-compose down -v` 후 재시작 |
| **MSSQL 연결 실패** | Connection refused | Azure SQL Edge 상태 확인, 포트 1433 확인 | MSSQL Server 상태 확인 |
| **sqlcmd 없음 오류** | `sqlcmd: command not found` | `./init-mssql-simple.sh` 사용 | 기본 sqlcmd 사용 |
| **MariaDB 연결 실패** | Access denied | 비밀번호 확인, 사용자 권한 확인 | 동일 |
| **배치 실행 실패** | Job execution failed | 로그 확인, DB 연결 상태 확인 | 동일 |
| **데이터 변환 오류** | Transformation error | 매핑 테이블 확인, 데이터 타입 확인 | 동일 |
| **플랫폼 호환성 오류** | `platform does not match` | `docker-compose-arm64.yml` 사용 | 기본 compose 파일 사용 |

### 8.2 ARM64 환경 특별 문제 해결

#### 8.2.1 sqlcmd 관련 문제
```bash
# 문제: sqlcmd를 찾을 수 없음
# 해결: Python 방식 사용
./init-mssql-simple.sh

# 또는 수동 설치 시도
./install-sqlcmd.sh

# 설치 후 경로 확인
docker exec -it mssql-dev find / -name "sqlcmd" -type f 2>/dev/null
```

#### 8.2.2 Azure SQL Edge 연결 문제
```bash
# TrustServerCertificate 옵션 필수
docker exec -it mssql-dev python3 << 'EOF'
import pyodbc
conn = pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=master;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes')
print("연결 성공")
conn.close()
EOF
```

#### 8.2.3 phpMyAdmin 호환성 문제
```bash
# ARM64에서는 Adminer 사용
# http://localhost:8082 접속
# 시스템: MySQL, 서버: mariadb-dev
```

### 8.2 디버깅 명령어
```bash
# 컨테이너 상태 확인
docker-compose ps

# 특정 컨테이너 로그 확인
docker-compose logs mssql-dev
docker-compose logs mariadb-dev

# 컨테이너 내부 접속
docker exec -it mssql-dev bash
docker exec -it mariadb-dev bash

# 네트워크 연결 확인
docker network ls
docker network inspect docker_batch-network

# 포트 사용 확인
netstat -an | grep 1433
netstat -an | grep 3306
netstat -an | grep 8080
```

### 8.3 데이터 초기화 (재테스트용)
```bash
# 모든 컨테이너 중지 및 볼륨 삭제
docker-compose down -v

# 애플리케이션 로그 삭제
rm -rf logs/*

# 빌드 캐시 정리
./gradlew clean

# 처음부터 다시 시작
docker-compose up -d
./init-mssql.sh
```

## 🎯 Step 9: 운영환경 테스트 (선택사항)

### 9.1 운영환경 컨테이너 시작
```bash
# 운영환경 디렉토리로 이동
cd ../prod

# 운영환경 컨테이너 시작
docker-compose up -d

# Master-Slave 복제 설정
./setup-replication.sh
```

### 9.2 복제 상태 확인
```bash
# Master 상태 확인
docker exec -it mariadb-master mysql -u root -pProdPassword123! -e "SHOW MASTER STATUS;"

# Slave 상태 확인
docker exec -it mariadb-slave mysql -u root -pProdPassword123! -e "SHOW SLAVE STATUS\G"
```

### 9.3 운영환경 배치 실행
```bash
# 운영 프로파일로 애플리케이션 실행
java -jar ../../build/libs/batch-migration-1.0-SNAPSHOT.jar --spring.profiles.active=prod &

# 대용량 데이터 이관 테스트
curl -X POST "http://localhost:8080/api/migration/start?chunkSize=2000&skipLimit=500"
```

## 🏁 완료 체크리스트

### ✅ 환경별 테스트 완료 확인

#### 🍎 ARM64 환경 (Apple Silicon) 체크리스트
- [ ] `./start-arm64.sh` 스크립트 정상 실행
- [ ] `docker-compose-arm64.yml` 컨테이너 정상 시작
- [ ] Azure SQL Edge (MSSQL) 연결 성공
- [ ] **sqlcmd 문제 해결**: `./init-mssql-simple.sh` 또는 `./install-sqlcmd.sh` 성공
- [ ] **Python 방식 초기화**: pyodbc를 통한 SQL 실행 성공
- [ ] MariaDB 연결 성공
- [ ] Adminer 웹 UI 접속 성공 (http://localhost:8082)
- [ ] MSSQL 샘플 데이터 생성 완료 (한글 테이블/컬럼/값)
- [ ] **Python으로 데이터 확인**: 테이블 레코드 수 확인 성공
- [ ] MariaDB 타겟 테이블 생성 완료 (영어 테이블/컬럼)
- [ ] 이관 전 빈 테이블 상태 확인
- [ ] 애플리케이션 빌드 성공 (Java 17)
- [ ] 배치 애플리케이션 정상 시작
- [ ] 전체 데이터 이관 성공
- [ ] 한글→영어 변환 정상 동작 확인
- [ ] 이관 후 데이터 존재 확인
- [ ] 데이터 정합성 검증 완료 (소스 vs 타겟 레코드 수)
- [ ] 값 변환 확인 (남성→MALE, 배송완료→DELIVERED 등)
- [ ] migrated_at 컬럼 이관 시점 기록 확인
- [ ] 성능 로그 정상 출력
- [ ] Adminer에서 최종 결과 확인 완료
- [ ] **TrustServerCertificate 옵션**: Azure SQL Edge 연결 시 필수 옵션 적용

#### 💻 Intel 환경 체크리스트
- [ ] `docker-compose up -d` 정상 실행
- [ ] MSSQL Server 2022 연결 성공
- [ ] MariaDB 연결 성공
- [ ] phpMyAdmin 접속 성공 (http://localhost:8081)
- [ ] Adminer 접속 성공 (http://localhost:8082)
- [ ] 나머지 체크리스트는 ARM64와 동일

### 📊 최종 결과 요약 스크립트

#### ARM64 환경용
```bash
# ARM64 환경 최종 결과 요약
echo "=== ARM64 환경 데이터 이관 테스트 완료 ==="
echo "📅 테스트 일시: $(date)"
echo "🏗️ 사용 환경: ARM64 (Apple Silicon)"
echo "🐳 Docker Compose: docker-compose-arm64.yml"
echo "🔢 이관된 테이블 수: $(docker exec mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'targetDB' AND TABLE_TYPE = 'BASE TABLE'" -s -N)"

# 실제 레코드 수 계산
TOTAL_RECORDS=$(docker exec mariadb-dev mysql -u root -pDevPassword123! -D targetDB -e "
SELECT (SELECT COUNT(*) FROM users) +
       (SELECT COUNT(*) FROM products) +
       (SELECT COUNT(*) FROM orders) +
       (SELECT COUNT(*) FROM order_details) +
       (SELECT COUNT(*) FROM categories) +
       (SELECT COUNT(*) FROM reviews) +
       (SELECT COUNT(*) FROM notices) +
       (SELECT COUNT(*) FROM coupons) +
       (SELECT COUNT(*) FROM deliveries) +
       (SELECT COUNT(*) FROM inquiries) as total" -s -N)

echo "📊 총 이관된 레코드 수: $TOTAL_RECORDS"
echo "🌐 웹 관리 도구: Adminer (http://localhost:8082)"
echo "✅ 테스트 상태: 성공"
echo ""
echo "🔍 주요 변환 확인:"
echo "  - 테이블명: 사용자 → users, 상품 → products, 주문 → orders"
echo "  - 컬럼명: 사용자ID → user_id, 이름 → name, 성별 → gender"
echo "  - 값 변환: 남성 → MALE, 여성 → FEMALE, 배송완료 → DELIVERED"
echo "  - 이관 시점: migrated_at 컬럼에 기록됨"
```

#### Intel 환경용
```bash
# Intel 환경 최종 결과 요약
echo "=== Intel 환경 데이터 이관 테스트 완료 ==="
echo "📅 테스트 일시: $(date)"
echo "🏗️ 사용 환경: Intel/AMD64"
echo "🐳 Docker Compose: docker-compose.yml"
echo "🌐 웹 관리 도구: phpMyAdmin (http://localhost:8081), Adminer (http://localhost:8082)"
# 나머지는 ARM64와 동일
```

### 🔧 환경별 정리 명령어

#### ARM64 환경 정리
```bash
# ARM64 환경 컨테이너 정리
cd docker/dev
docker-compose -f docker-compose-arm64.yml down -v
docker system prune -f
```

#### Intel 환경 정리
```bash
# Intel 환경 컨테이너 정리
cd docker/dev
docker-compose down -v
docker system prune -f
```

## 🎉 축하합니다!

완전한 Docker 기반 MSSQL → MariaDB 데이터 이관 테스트를 성공적으로 완료했습니다!

### 🚀 다음 단계
1. **운영환경 테스트**: `docker/prod` 디렉토리에서 Master-Slave 환경 테스트
2. **성능 튜닝**: 청크 사이즈 조절 및 대용량 데이터 테스트
3. **모니터링 강화**: 추가 로그 분석 및 알림 설정
4. **자동화**: CI/CD 파이프라인에 통합

이제 실제 운영 환경에서도 안전하게 데이터 이관을 수행할 수 있습니다! 🎯

## 🔧 애플리케이션 설정

### 1. application.yml 환경별 설정 확인

#### 개발환경 설정
```yaml
spring:
  profiles:
    active: dev
  datasource:
    source:
      jdbc-url: jdbc:sqlserver://localhost:1433;databaseName=sourceDB;trustServerCertificate=true
      username: sa
      password: DevPassword123!
    target:
      jdbc-url: jdbc:mariadb://localhost:3306/targetDB
      username: root
      password: DevPassword123!
```

#### 운영환경 설정
```yaml
spring:
  profiles:
    active: prod
  datasource:
    source:
      jdbc-url: jdbc:sqlserver://localhost:1434;databaseName=sourceDB;trustServerCertificate=true
      username: sa
      password: ProdPassword123!
    target:
      jdbc-url: jdbc:mariadb://localhost:3307/targetDB
      username: batch_user
      password: BatchPassword123!
    target-slave:
      jdbc-url: jdbc:mariadb://localhost:3308/targetDB
      username: batch_user
      password: BatchPassword123!
```

## 🧪 테스트 실행

### 1. 단위 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.example.batch.*Test"

# 테스트 결과 확인
open build/reports/tests/test/index.html
```

### 2. 통합 테스트 실행
```bash
# Testcontainers를 사용한 통합 테스트
./gradlew integrationTest

# Docker 환경에서 애플리케이션 테스트
./gradlew bootRun --args="--spring.profiles.active=dev"
```

### 3. 배치 작업 테스트

#### 3.1 개발환경에서 배치 실행
```bash
# 애플리케이션 시작
java -jar build/libs/batch-migration-1.0-SNAPSHOT.jar --spring.profiles.active=dev

# REST API를 통한 배치 실행
curl -X POST "http://localhost:8080/api/migration/start?chunkSize=100"

# 특정 테이블 이관 테스트
curl -X POST "http://localhost:8080/api/migration/table/사용자"
```

#### 3.2 운영환경에서 배치 실행
```bash
# 운영환경 프로파일로 실행
java -jar build/libs/batch-migration-1.0-SNAPSHOT.jar --spring.profiles.active=prod

# 대용량 데이터 이관 테스트
curl -X POST "http://localhost:8080/api/migration/start?chunkSize=2000&skipLimit=500"
```

## 📊 모니터링 및 로그 확인

### 1. 애플리케이션 로그
```bash
# 실시간 로그 모니터링
tail -f logs/batch-migration.log

# 배치 처리 로그
tail -f logs/batch-processing.log

# 에러 로그
tail -f logs/batch-errors.log
```

### 2. 데이터베이스 로그
```bash
# MSSQL 로그 확인
docker logs mssql-dev

# MariaDB Master 로그
docker logs mariadb-master

# MariaDB Slave 로그
docker logs mariadb-slave
```

### 3. 성능 모니터링
```bash
# 컨테이너 리소스 사용량 확인
docker stats

# 특정 컨테이너 상세 정보
docker inspect mariadb-master
```

## 🔍 문제 해결

### 1. 컨테이너 시작 실패
```bash
# 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs [서비스명]

# 컨테이너 재시작
docker-compose restart [서비스명]
```

### 2. 데이터베이스 연결 실패
```bash
# 네트워크 확인
docker network ls
docker network inspect [네트워크명]

# 포트 확인
netstat -an | grep [포트번호]
```

### 3. 복제 설정 문제
```bash
# Master 바이너리 로그 확인
docker exec -it mariadb-master mysql -u root -pProdPassword123! -e "SHOW BINARY LOGS;"

# Slave 복제 에러 확인
docker exec -it mariadb-slave mysql -u root -pProdPassword123! -e "SHOW SLAVE STATUS\G" | grep Error
```

## 🧹 환경 정리

### 1. 개발환경 정리
```bash
cd docker/dev
docker-compose down -v  # 볼륨까지 삭제
docker system prune -f  # 사용하지 않는 리소스 정리
```

### 2. 운영환경 정리
```bash
cd docker/prod
docker-compose down -v
docker system prune -f
```

### 3. 전체 정리
```bash
# 모든 컨테이너 중지 및 삭제
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)

# 모든 이미지 삭제 (주의!)
docker rmi $(docker images -q)

# 모든 볼륨 삭제 (주의!)
docker volume prune -f
```

## 📈 성능 테스트

### 1. 부하 테스트
```bash
# 대용량 데이터 생성 (MSSQL)
docker exec -it mssql-prod /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P ProdPassword123! -d sourceDB -Q "EXEC GenerateLargeDataSet"

# 다양한 청크 사이즈로 성능 테스트
for chunk_size in 500 1000 2000 5000; do
    echo "Testing chunk size: $chunk_size"
    curl -X POST "http://localhost:8080/api/migration/start?chunkSize=$chunk_size"
    sleep 60  # 1분 대기
done
```

### 2. 동시성 테스트
```bash
# 여러 테이블 동시 이관
curl -X POST "http://localhost:8080/api/migration/table/사용자" &
curl -X POST "http://localhost:8080/api/migration/table/상품" &
curl -X POST "http://localhost:8080/api/migration/table/주문" &
wait
```

이 가이드를 따라하면 Docker 환경에서 안전하고 효율적으로 데이터 이관 배치 프로그램을 테스트할 수 있습니다.
