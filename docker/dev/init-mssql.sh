#!/bin/bash

# MSSQL (Azure SQL Edge) 초기화 스크립트 for ARM64

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# sqlcmd 경로 확인 및 설정
get_sqlcmd_path() {
    # Azure SQL Edge에서 sqlcmd 경로 확인
    if docker exec mssql-dev which sqlcmd >/dev/null 2>&1; then
        echo "sqlcmd"
    elif docker exec mssql-dev test -f /opt/mssql-tools/bin/sqlcmd >/dev/null 2>&1; then
        echo "/opt/mssql-tools/bin/sqlcmd"
    elif docker exec mssql-dev test -f /opt/mssql-tools18/bin/sqlcmd >/dev/null 2>&1; then
        echo "/opt/mssql-tools18/bin/sqlcmd"
    else
        log_error "sqlcmd를 찾을 수 없습니다"
        return 1
    fi
}

# MSSQL 연결 대기
wait_for_mssql() {
    local max_attempts=30
    local attempt=1
    local sqlcmd_path

    log_info "MSSQL 서버 연결 대기 중..."

    # sqlcmd 경로 확인
    sqlcmd_path=$(get_sqlcmd_path)
    if [ $? -ne 0 ]; then
        return 1
    fi

    log_info "sqlcmd 경로: $sqlcmd_path"

    while [ $attempt -le $max_attempts ]; do
        if docker exec mssql-dev $sqlcmd_path -S localhost -U sa -P DevPassword123! -Q "SELECT 1" >/dev/null 2>&1; then
            log_success "MSSQL 서버 연결 성공"
            return 0
        fi

        log_info "연결 시도 $attempt/$max_attempts..."
        sleep 5
        attempt=$((attempt + 1))
    done

    log_error "MSSQL 서버 연결 실패"
    return 1
}

# 데이터베이스 및 테이블 생성
create_database_and_tables() {
    local sqlcmd_path

    log_info "데이터베이스 및 테이블 생성 중..."

    # sqlcmd 경로 확인
    sqlcmd_path=$(get_sqlcmd_path)
    if [ $? -ne 0 ]; then
        return 1
    fi

    # 데이터베이스 생성 스크립트 실행
    log_info "데이터베이스 생성 스크립트 실행 중..."
    docker exec mssql-dev $sqlcmd_path -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/01-create-database.sql

    if [ $? -eq 0 ]; then
        log_success "데이터베이스 및 테이블 생성 완료"
    else
        log_error "데이터베이스 생성 실패"
        return 1
    fi

    # 샘플 데이터 삽입
    log_info "샘플 데이터 삽입 중..."
    docker exec mssql-dev $sqlcmd_path -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/02-insert-sample-data.sql

    if [ $? -eq 0 ]; then
        log_success "샘플 데이터 삽입 완료"
    else
        log_error "샘플 데이터 삽입 실패"
        return 1
    fi
}

# 데이터 확인
verify_data() {
    local sqlcmd_path

    log_info "데이터 확인 중..."

    # sqlcmd 경로 확인
    sqlcmd_path=$(get_sqlcmd_path)
    if [ $? -ne 0 ]; then
        return 1
    fi

    # 사용자 테이블 레코드 수 확인
    USER_COUNT=$(docker exec mssql-dev $sqlcmd_path -S localhost -U sa -P DevPassword123! -d sourceDB -Q "SELECT COUNT(*) FROM 사용자" -h -1 -W | tr -d ' ')

    if [ "$USER_COUNT" -gt "0" ]; then
        log_success "사용자 테이블에 $USER_COUNT 개의 레코드가 있습니다"
    else
        log_error "사용자 테이블에 데이터가 없습니다"
    fi

    # 상품 테이블 레코드 수 확인
    PRODUCT_COUNT=$(docker exec mssql-dev $sqlcmd_path -S localhost -U sa -P DevPassword123! -d sourceDB -Q "SELECT COUNT(*) FROM 상품" -h -1 -W | tr -d ' ')

    if [ "$PRODUCT_COUNT" -gt "0" ]; then
        log_success "상품 테이블에 $PRODUCT_COUNT 개의 레코드가 있습니다"
    else
        log_error "상품 테이블에 데이터가 없습니다"
    fi
}

# 메인 실행
main() {
    log_info "=== MSSQL 개발환경 초기화 시작 ==="
    
    # MSSQL 연결 대기
    if ! wait_for_mssql; then
        exit 1
    fi
    
    # 데이터베이스 및 테이블 생성
    if ! create_database_and_tables; then
        exit 1
    fi
    
    # 데이터 확인
    verify_data
    
    log_success "=== MSSQL 개발환경 초기화 완료 ==="
}

# 스크립트 실행
main "$@"
