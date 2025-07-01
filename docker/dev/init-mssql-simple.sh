#!/bin/bash

# Azure SQL Edge 간단 초기화 스크립트 (sqlcmd 없이)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# SQL 파일을 직접 복사하여 실행
execute_sql_file() {
    local sql_file=$1
    local description=$2
    
    log_info "$description 실행 중..."
    
    # SQL 파일 내용을 읽어서 한 줄씩 실행
    docker exec --user root mssql-dev bash -c "
        # Python 설치 (간단한 방법)
        apt-get update > /dev/null 2>&1
        apt-get install -y python3 python3-pip > /dev/null 2>&1
        pip3 install pyodbc > /dev/null 2>&1
        
        # Python 스크립트로 SQL 실행
        python3 << 'PYTHON_EOF'
import pyodbc
import sys

def execute_sql_file(file_path):
    try:
        # 연결 문자열 (TrustServerCertificate=yes 추가)
        conn_str = 'DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=master;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes'
        
        print(f'SQL Server 연결 중...')
        conn = pyodbc.connect(conn_str)
        cursor = conn.cursor()
        
        # SQL 파일 읽기
        with open(file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        print(f'SQL 파일 읽기 완료: {file_path}')
        
        # GO로 분리하여 배치 실행
        batches = [batch.strip() for batch in sql_content.split('GO') if batch.strip()]
        
        print(f'총 {len(batches)}개의 SQL 배치 실행')
        
        for i, batch in enumerate(batches, 1):
            try:
                print(f'배치 {i}/{len(batches)} 실행 중...')
                cursor.execute(batch)
                conn.commit()
                print(f'배치 {i} 실행 성공')
            except Exception as e:
                print(f'배치 {i} 실행 오류: {e}')
                # 계속 진행
                continue
        
        cursor.close()
        conn.close()
        print('SQL 파일 실행 완료')
        return True
        
    except Exception as e:
        print(f'SQL 실행 중 오류: {e}')
        return False

# SQL 파일 실행
if execute_sql_file('$sql_file'):
    print('SUCCESS')
else:
    print('FAILED')
    sys.exit(1)
PYTHON_EOF
    "
    
    if [ $? -eq 0 ]; then
        log_success "$description 완료"
        return 0
    else
        log_error "$description 실패"
        return 1
    fi
}

# 데이터 확인
verify_data() {
    log_info "데이터 확인 중..."
    
    docker exec --user root mssql-dev python3 << 'PYTHON_EOF'
import pyodbc

try:
    # sourceDB에 연결
    conn_str = 'DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=sourceDB;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes'
    conn = pyodbc.connect(conn_str)
    cursor = conn.cursor()
    
    # 테이블 목록 확인
    cursor.execute("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'")
    tables = [row[0] for row in cursor.fetchall()]
    print(f'생성된 테이블: {", ".join(tables)}')
    
    # 주요 테이블 레코드 수 확인
    test_tables = ['사용자', '상품', '주문']
    for table in test_tables:
        if table in tables:
            cursor.execute(f"SELECT COUNT(*) FROM [{table}]")
            count = cursor.fetchone()[0]
            print(f'{table} 테이블: {count}개 레코드')
        else:
            print(f'{table} 테이블: 없음')
    
    cursor.close()
    conn.close()
    print('데이터 확인 완료')
    
except Exception as e:
    print(f'데이터 확인 중 오류: {e}')
PYTHON_EOF
    
    if [ $? -eq 0 ]; then
        log_success "데이터 확인 완료"
        return 0
    else
        log_error "데이터 확인 실패"
        return 1
    fi
}

# 연결 대기
wait_for_sql_server() {
    local max_attempts=30
    local attempt=1
    
    log_info "SQL Server 연결 대기 중..."
    
    while [ $attempt -le $max_attempts ]; do
        # Python으로 연결 테스트
        if docker exec mssql-dev python3 -c "
import pyodbc
try:
    conn = pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=master;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes')
    conn.close()
    print('SUCCESS')
except:
    print('FAILED')
" 2>/dev/null | grep -q "SUCCESS"; then
            log_success "SQL Server 연결 성공"
            return 0
        fi
        
        log_info "연결 시도 $attempt/$max_attempts..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    log_error "SQL Server 연결 실패"
    return 1
}

# 메인 실행
main() {
    log_info "=== Azure SQL Edge 간단 초기화 시작 ==="
    
    # 컨테이너 상태 확인
    if ! docker ps | grep -q mssql-dev; then
        log_error "mssql-dev 컨테이너가 실행되지 않았습니다"
        exit 1
    fi
    
    # SQL Server 연결 대기
    if ! wait_for_sql_server; then
        exit 1
    fi
    
    # 데이터베이스 생성
    if execute_sql_file "/docker-entrypoint-initdb.d/01-create-database.sql" "데이터베이스 및 테이블 생성"; then
        # 샘플 데이터 삽입
        execute_sql_file "/docker-entrypoint-initdb.d/02-insert-sample-data.sql" "샘플 데이터 삽입"
    fi
    
    # 결과 확인
    verify_data
    
    log_success "=== 초기화 완료 ==="
    log_info "이제 다음 명령어로 데이터를 확인할 수 있습니다:"
    echo "docker exec -it mssql-dev python3 -c \"
import pyodbc
conn = pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=sourceDB;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes')
cursor = conn.cursor()
cursor.execute('SELECT COUNT(*) FROM 사용자')
print('사용자 테이블 레코드 수:', cursor.fetchone()[0])
conn.close()
\""
}

main "$@"
