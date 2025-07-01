#!/bin/bash

# ARM64 (Apple Silicon) 환경용 Docker 시작 스크립트

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

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 시스템 아키텍처 확인
check_architecture() {
    ARCH=$(uname -m)
    log_info "시스템 아키텍처: $ARCH"
    
    if [[ "$ARCH" == "arm64" ]]; then
        log_success "ARM64 환경 감지됨"
        return 0
    else
        log_warn "ARM64가 아닌 환경입니다. 일반 docker-compose.yml을 사용하세요."
        return 1
    fi
}

# Docker 상태 확인
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker가 설치되지 않았습니다."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker가 실행되지 않았습니다. Docker Desktop을 시작하세요."
        exit 1
    fi
    
    log_success "Docker 상태 정상"
}

# 기존 컨테이너 정리
cleanup_containers() {
    log_info "기존 컨테이너 정리 중..."
    
    # ARM64 호환 compose 파일이 있으면 사용
    if [[ -f "docker-compose-arm64.yml" ]]; then
        docker-compose -f docker-compose-arm64.yml down -v 2>/dev/null || true
    fi
    
    # 기본 compose 파일로도 정리 시도
    docker-compose down -v 2>/dev/null || true
    
    log_success "컨테이너 정리 완료"
}

# 컨테이너 시작
start_containers() {
    log_info "ARM64 호환 컨테이너 시작 중..."
    
    # ARM64 전용 compose 파일 사용
    if [[ -f "docker-compose-arm64.yml" ]]; then
        docker-compose -f docker-compose-arm64.yml up -d
    else
        log_warn "ARM64 전용 compose 파일이 없습니다. 기본 파일을 사용합니다."
        docker-compose up -d
    fi
    
    if [[ $? -eq 0 ]]; then
        log_success "컨테이너 시작 완료"
    else
        log_error "컨테이너 시작 실패"
        exit 1
    fi
}

# 컨테이너 상태 확인
check_container_status() {
    log_info "컨테이너 상태 확인 중..."
    
    # 컨테이너 목록 출력
    if [[ -f "docker-compose-arm64.yml" ]]; then
        docker-compose -f docker-compose-arm64.yml ps
    else
        docker-compose ps
    fi
    
    # 헬스체크 대기
    log_info "데이터베이스 헬스체크 대기 중..."
    sleep 30
    
    # MSSQL 연결 테스트
    if docker exec mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -Q "SELECT 1" &>/dev/null; then
        log_success "MSSQL 연결 성공"
    else
        log_error "MSSQL 연결 실패"
    fi
    
    # MariaDB 연결 테스트
    if docker exec mariadb-dev mysql -u root -pDevPassword123! -e "SELECT 1" &>/dev/null; then
        log_success "MariaDB 연결 성공"
    else
        log_error "MariaDB 연결 실패"
    fi
}

# MSSQL 초기화
initialize_mssql() {
    log_info "MSSQL 데이터베이스 초기화 중..."
    
    # 초기화 스크립트 실행
    if [[ -f "./init-mssql.sh" ]]; then
        ./init-mssql.sh
    else
        log_warn "init-mssql.sh 파일이 없습니다. 수동으로 초기화하세요."
        log_info "수동 초기화 명령어:"
        echo "docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/01-create-database.sql"
        echo "docker exec -it mssql-dev /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -i /docker-entrypoint-initdb.d/02-insert-sample-data.sql"
    fi
}

# 접속 정보 출력
print_access_info() {
    log_info "=== 접속 정보 ==="
    echo "📊 Adminer (DB 관리): http://localhost:8082"
    echo "   - MSSQL: 시스템=MS SQL, 서버=mssql-dev, 사용자=sa, 비밀번호=DevPassword123!"
    echo "   - MariaDB: 시스템=MySQL, 서버=mariadb-dev, 사용자=root, 비밀번호=DevPassword123!"
    echo ""
    echo "🔧 MySQL 클라이언트 (CLI):"
    echo "   docker exec -it mysql-client-dev mysql -h mariadb-dev -u root -pDevPassword123!"
    echo ""
    echo "📝 로그 확인:"
    echo "   docker-compose logs -f [서비스명]"
    echo ""
    echo "🛑 중지:"
    echo "   docker-compose down -v"
}

# 메인 실행
main() {
    log_info "=== ARM64 환경용 Docker 개발환경 시작 ==="
    
    # 아키텍처 확인
    if ! check_architecture; then
        exit 1
    fi
    
    # Docker 상태 확인
    check_docker
    
    # 기존 컨테이너 정리
    cleanup_containers
    
    # 컨테이너 시작
    start_containers
    
    # 상태 확인
    check_container_status
    
    # MSSQL 초기화
    initialize_mssql
    
    # 접속 정보 출력
    print_access_info
    
    log_success "=== ARM64 환경 개발환경 시작 완료 ==="
}

# 스크립트 실행
main "$@"
