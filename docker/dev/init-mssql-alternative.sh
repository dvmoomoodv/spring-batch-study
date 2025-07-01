#!/bin/bash

# Azure SQL Edge용 대체 초기화 스크립트 (sqlcmd 없이)

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

# sqlcmd 설치 시도
install_sqlcmd() {
    log_info "sqlcmd 설치 시도 중..."
    
    # mssql-tools 설치 시도
    docker exec mssql-dev bash -c "
        export DEBIAN_FRONTEND=noninteractive
        apt-get update
        apt-get install -y curl gnupg
        curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add -
        curl https://packages.microsoft.com/config/ubuntu/20.04/prod.list > /etc/apt/sources.list.d/msprod.list
        apt-get update
        ACCEPT_EULA=Y apt-get install -y mssql-tools18
        echo 'export PATH=\"\$PATH:/opt/mssql-tools18/bin\"' >> ~/.bashrc
    " 2>/dev/null
    
    # 설치 확인
    if docker exec mssql-dev test -f /opt/mssql-tools18/bin/sqlcmd; then
        log_success "sqlcmd 설치 성공: /opt/mssql-tools18/bin/sqlcmd"
        echo "/opt/mssql-tools18/bin/sqlcmd"
        return 0
    else
        log_warn "sqlcmd 설치 실패, 대체 방법 사용"
        return 1
    fi
}

# Python을 사용한 SQL 실행
execute_sql_with_python() {
    local sql_file=$1
    local database=${2:-"master"}
    
    log_info "Python을 사용하여 SQL 실행: $sql_file"
    
    # Python 스크립트 생성
    docker exec mssql-dev bash -c "cat > /tmp/execute_sql.py << 'EOF'
import pyodbc
import sys
import os

def execute_sql_file(sql_file, database='master'):
    try:
        # 연결 문자열
        conn_str = f'DRIVER={{ODBC Driver 17 for SQL Server}};SERVER=localhost;DATABASE={database};UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes'
        
        # 연결
        conn = pyodbc.connect(conn_str)
        cursor = conn.cursor()
        
        # SQL 파일 읽기
        with open(sql_file, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        # GO로 분리된 배치 실행
        batches = sql_content.split('GO')
        
        for batch in batches:
            batch = batch.strip()
            if batch:
                try:
                    cursor.execute(batch)
                    conn.commit()
                    print(f'배치 실행 성공')
                except Exception as e:
                    print(f'배치 실행 오류: {e}')
                    continue
        
        cursor.close()
        conn.close()
        print('SQL 파일 실행 완료')
        return True
        
    except Exception as e:
        print(f'오류: {e}')
        return False

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('사용법: python execute_sql.py <sql_file> [database]')
        sys.exit(1)
    
    sql_file = sys.argv[1]
    database = sys.argv[2] if len(sys.argv) > 2 else 'master'
    
    if execute_sql_file(sql_file, database):
        sys.exit(0)
    else:
        sys.exit(1)
EOF"

    # Python 및 pyodbc 설치
    docker exec mssql-dev bash -c "
        apt-get update
        apt-get install -y python3 python3-pip unixodbc-dev
        pip3 install pyodbc
    " 2>/dev/null
    
    # SQL 실행
    if docker exec mssql-dev python3 /tmp/execute_sql.py "$sql_file" "$database"; then
        log_success "Python을 통한 SQL 실행 성공"
        return 0
    else
        log_error "Python을 통한 SQL 실행 실패"
        return 1
    fi
}

# 직접 SQL Server에 연결하여 실행
execute_sql_direct() {
    local sql_content="$1"
    local database=${2:-"master"}
    
    log_info "직접 SQL 실행 중..."
    
    # osql 시도 (있다면)
    if docker exec mssql-dev which osql >/dev/null 2>&1; then
        echo "$sql_content" | docker exec -i mssql-dev osql -S localhost -U sa -P DevPassword123! -d "$database"
        return $?
    fi
    
    # isql 시도 (있다면)
    if docker exec mssql-dev which isql >/dev/null 2>&1; then
        echo "$sql_content" | docker exec -i mssql-dev isql -v -k "DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=$database;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes"
        return $?
    fi
    
    log_warn "직접 SQL 실행 도구를 찾을 수 없습니다"
    return 1
}

# 데이터베이스 초기화
initialize_database() {
    log_info "=== 데이터베이스 초기화 시작 ==="
    
    # 1. sqlcmd 설치 시도
    sqlcmd_path=$(install_sqlcmd)
    
    if [ $? -eq 0 ]; then
        log_info "sqlcmd 사용하여 초기화"
        
        # sqlcmd로 실행
        docker exec mssql-dev $sqlcmd_path -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/01-create-database.sql
        docker exec mssql-dev $sqlcmd_path -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/02-insert-sample-data.sql
        
    else
        log_info "Python 방법으로 초기화 시도"
        
        # Python으로 실행
        execute_sql_with_python "/docker-entrypoint-initdb.d/01-create-database.sql" "master"
        execute_sql_with_python "/docker-entrypoint-initdb.d/02-insert-sample-data.sql" "sourceDB"
    fi
    
    # 결과 확인
    verify_initialization
}

# 초기화 결과 확인
verify_initialization() {
    log_info "초기화 결과 확인 중..."
    
    # 데이터베이스 존재 확인 (Python 사용)
    docker exec mssql-dev bash -c "cat > /tmp/check_db.py << 'EOF'
import pyodbc
try:
    conn_str = 'DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=master;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes'
    conn = pyodbc.connect(conn_str)
    cursor = conn.cursor()
    
    # 데이터베이스 확인
    cursor.execute(\"SELECT name FROM sys.databases WHERE name = 'sourceDB'\")
    result = cursor.fetchone()
    
    if result:
        print('sourceDB 데이터베이스 존재함')
        
        # sourceDB로 연결하여 테이블 확인
        conn.close()
        conn_str = 'DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=sourceDB;UID=sa;PWD=DevPassword123!;TrustServerCertificate=yes'
        conn = pyodbc.connect(conn_str)
        cursor = conn.cursor()
        
        cursor.execute(\"SELECT COUNT(*) FROM 사용자\")
        user_count = cursor.fetchone()[0]
        print(f'사용자 테이블 레코드 수: {user_count}')
        
        cursor.execute(\"SELECT COUNT(*) FROM 상품\")
        product_count = cursor.fetchone()[0]
        print(f'상품 테이블 레코드 수: {product_count}')
        
    else:
        print('sourceDB 데이터베이스 없음')
        
    conn.close()
    
except Exception as e:
    print(f'확인 중 오류: {e}')
EOF"

    if docker exec mssql-dev python3 /tmp/check_db.py; then
        log_success "데이터베이스 초기화 확인 완료"
    else
        log_error "데이터베이스 초기화 확인 실패"
    fi
}

# 메인 실행
main() {
    log_info "=== Azure SQL Edge 대체 초기화 시작 ==="
    
    # 컨테이너 상태 확인
    if ! docker ps | grep -q mssql-dev; then
        log_error "mssql-dev 컨테이너가 실행되지 않았습니다"
        exit 1
    fi
    
    # 연결 대기
    log_info "SQL Server 연결 대기 중..."
    sleep 10
    
    # 초기화 실행
    initialize_database
    
    log_success "=== 초기화 완료 ==="
}

main "$@"
