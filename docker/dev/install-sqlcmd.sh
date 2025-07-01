#!/bin/bash

# Azure SQL Edge에 sqlcmd 설치 스크립트

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

# sqlcmd 설치
install_sqlcmd() {
    log_info "Azure SQL Edge에 sqlcmd 설치 중..."

    # 컨테이너에서 root 권한으로 설치 실행
    docker exec --user root mssql-dev bash -c "
        # 디렉토리 생성
        mkdir -p /var/lib/apt/lists/partial

        # 패키지 업데이트
        apt-get update

        # 필요한 패키지 설치
        apt-get install -y curl gnupg2 software-properties-common apt-transport-https ca-certificates

        # Microsoft 키 추가
        curl -fsSL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor -o /usr/share/keyrings/microsoft-prod.gpg

        # Microsoft 저장소 추가 (수동으로)
        echo 'deb [arch=amd64,arm64,armhf signed-by=/usr/share/keyrings/microsoft-prod.gpg] https://packages.microsoft.com/ubuntu/20.04/prod focal main' > /etc/apt/sources.list.d/msprod.list

        # 패키지 목록 업데이트
        apt-get update

        # mssql-tools18 설치
        ACCEPT_EULA=Y apt-get install -y mssql-tools18

        # PATH에 추가
        echo 'export PATH=\"\$PATH:/opt/mssql-tools18/bin\"' >> /root/.bashrc

        # 심볼릭 링크 생성
        ln -sf /opt/mssql-tools18/bin/sqlcmd /usr/local/bin/sqlcmd

        # 권한 설정
        chmod +x /opt/mssql-tools18/bin/sqlcmd
    "

    # 설치 확인
    if docker exec --user root mssql-dev test -f /opt/mssql-tools18/bin/sqlcmd; then
        log_success "sqlcmd 설치 성공: /opt/mssql-tools18/bin/sqlcmd"
        return 0
    else
        log_error "sqlcmd 설치 실패, 대체 방법 시도"
        return 1
    fi
}

# 연결 테스트
test_connection() {
    log_info "sqlcmd 연결 테스트 중..."

    # 여러 경로 시도
    local sqlcmd_paths=(
        "/opt/mssql-tools18/bin/sqlcmd"
        "/usr/local/bin/sqlcmd"
        "sqlcmd"
    )

    for path in "${sqlcmd_paths[@]}"; do
        log_info "연결 테스트: $path"
        if docker exec mssql-dev $path -S localhost -U sa -P DevPassword123! -Q "SELECT @@VERSION" -C 2>/dev/null; then
            log_success "sqlcmd 연결 테스트 성공: $path"
            echo "WORKING_SQLCMD_PATH=$path"
            return 0
        fi
    done

    log_error "모든 sqlcmd 경로에서 연결 테스트 실패"
    return 1
}

# 메인 실행
main() {
    log_info "=== sqlcmd 설치 시작 ==="
    
    # 컨테이너 상태 확인
    if ! docker ps | grep -q mssql-dev; then
        log_error "mssql-dev 컨테이너가 실행되지 않았습니다"
        exit 1
    fi
    
    # sqlcmd 설치
    if install_sqlcmd; then
        # 연결 테스트
        test_connection
    else
        exit 1
    fi
    
    log_success "=== sqlcmd 설치 완료 ==="
}

main "$@"
