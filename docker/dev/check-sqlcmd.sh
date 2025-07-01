#!/bin/bash

# Azure SQL Edge sqlcmd 경로 확인 스크립트

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
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

# sqlcmd 경로 확인
check_sqlcmd_paths() {
    log_info "Azure SQL Edge에서 sqlcmd 경로 확인 중..."
    
    # 가능한 경로들 확인
    paths=(
        "sqlcmd"
        "/opt/mssql-tools/bin/sqlcmd"
        "/opt/mssql-tools18/bin/sqlcmd"
        "/usr/bin/sqlcmd"
        "/usr/local/bin/sqlcmd"
    )
    
    for path in "${paths[@]}"; do
        log_info "경로 확인: $path"
        if docker exec mssql-dev test -f "$path" 2>/dev/null || docker exec mssql-dev which "$path" >/dev/null 2>&1; then
            log_success "찾음: $path"
            
            # 연결 테스트
            if docker exec mssql-dev $path -S localhost -U sa -P DevPassword123! -Q "SELECT 1" >/dev/null 2>&1; then
                log_success "연결 테스트 성공: $path"
                echo "SQLCMD_PATH=$path"
                return 0
            else
                log_error "연결 테스트 실패: $path"
            fi
        else
            echo "  ❌ 없음: $path"
        fi
    done
    
    log_error "사용 가능한 sqlcmd를 찾을 수 없습니다"
    return 1
}

# 컨테이너 내부 구조 확인
explore_container() {
    log_info "컨테이너 내부 구조 확인..."
    
    echo "=== /opt 디렉토리 구조 ==="
    docker exec mssql-dev find /opt -name "*sql*" -type d 2>/dev/null || echo "mssql 관련 디렉토리 없음"
    
    echo ""
    echo "=== sqlcmd 파일 검색 ==="
    docker exec mssql-dev find / -name "sqlcmd" -type f 2>/dev/null || echo "sqlcmd 파일 없음"
    
    echo ""
    echo "=== PATH 환경변수 ==="
    docker exec mssql-dev echo '$PATH' 2>/dev/null
    
    echo ""
    echo "=== 설치된 패키지 확인 ==="
    docker exec mssql-dev dpkg -l | grep -i sql 2>/dev/null || echo "dpkg 명령어 없음"
}

# 메인 실행
main() {
    log_info "=== Azure SQL Edge sqlcmd 경로 확인 시작 ==="
    
    # 컨테이너 실행 상태 확인
    if ! docker ps | grep -q mssql-dev; then
        log_error "mssql-dev 컨테이너가 실행되지 않았습니다"
        exit 1
    fi
    
    # sqlcmd 경로 확인
    if check_sqlcmd_paths; then
        log_success "sqlcmd 경로 확인 완료"
    else
        log_error "sqlcmd 경로 확인 실패"
        echo ""
        explore_container
    fi
}

main "$@"
