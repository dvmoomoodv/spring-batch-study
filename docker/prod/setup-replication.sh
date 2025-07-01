#!/bin/bash

# MariaDB Master-Slave 복제 설정 스크립트

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

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 컨테이너 상태 확인
check_container_status() {
    local container_name=$1
    if ! docker ps | grep -q "$container_name"; then
        log_error "$container_name 컨테이너가 실행되지 않았습니다."
        return 1
    fi
    return 0
}

# 데이터베이스 연결 대기
wait_for_database() {
    local container_name=$1
    local max_attempts=30
    local attempt=1
    
    log_info "$container_name 데이터베이스 연결 대기 중..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker exec $container_name mysql -u root -pProdPassword123! -e "SELECT 1" >/dev/null 2>&1; then
            log_success "$container_name 데이터베이스 연결 성공"
            return 0
        fi
        
        log_info "연결 시도 $attempt/$max_attempts..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    log_error "$container_name 데이터베이스 연결 실패"
    return 1
}

log_info "=== MariaDB Master-Slave 복제 설정 시작 ==="

# 1. 컨테이너 상태 확인
log_info "컨테이너 상태 확인 중..."
check_container_status "mariadb-master" || exit 1
check_container_status "mariadb-slave" || exit 1

# 2. 데이터베이스 연결 대기
wait_for_database "mariadb-master" || exit 1
wait_for_database "mariadb-slave" || exit 1

# 3. Master 상태 확인 및 바이너리 로그 정보 획득
log_info "Master 상태 확인 중..."
MASTER_STATUS=$(docker exec mariadb-master mysql -u root -pProdPassword123! -e "SHOW MASTER STATUS;" --batch --skip-column-names)

if [ -z "$MASTER_STATUS" ]; then
    log_error "Master 상태를 가져올 수 없습니다."
    exit 1
fi

# 바이너리 로그 파일명과 위치 추출
MASTER_LOG_FILE=$(echo $MASTER_STATUS | awk '{print $1}')
MASTER_LOG_POS=$(echo $MASTER_STATUS | awk '{print $2}')

log_info "Master 바이너리 로그: $MASTER_LOG_FILE"
log_info "Master 로그 위치: $MASTER_LOG_POS"

# 4. Slave에서 복제 설정
log_info "Slave 복제 설정 중..."

# 기존 복제 설정 중지 (있다면)
docker exec mariadb-slave mysql -u root -pProdPassword123! -e "STOP SLAVE;" 2>/dev/null || true

# 복제 설정
CHANGE_MASTER_SQL="CHANGE MASTER TO 
    MASTER_HOST='mariadb-master',
    MASTER_USER='repl_user',
    MASTER_PASSWORD='ReplPassword123!',
    MASTER_LOG_FILE='$MASTER_LOG_FILE',
    MASTER_LOG_POS=$MASTER_LOG_POS;"

docker exec mariadb-slave mysql -u root -pProdPassword123! -e "$CHANGE_MASTER_SQL"

if [ $? -eq 0 ]; then
    log_success "Slave 복제 설정 완료"
else
    log_error "Slave 복제 설정 실패"
    exit 1
fi

# 5. 복제 시작
log_info "복제 시작 중..."
docker exec mariadb-slave mysql -u root -pProdPassword123! -e "START SLAVE;"

if [ $? -eq 0 ]; then
    log_success "복제 시작 완료"
else
    log_error "복제 시작 실패"
    exit 1
fi

# 6. 복제 상태 확인
log_info "복제 상태 확인 중..."
sleep 5

SLAVE_STATUS=$(docker exec mariadb-slave mysql -u root -pProdPassword123! -e "SHOW SLAVE STATUS\G")

# Slave_IO_Running과 Slave_SQL_Running 상태 확인
IO_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_IO_Running:" | awk '{print $2}')
SQL_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_SQL_Running:" | awk '{print $2}')

log_info "Slave_IO_Running: $IO_RUNNING"
log_info "Slave_SQL_Running: $SQL_RUNNING"

if [ "$IO_RUNNING" = "Yes" ] && [ "$SQL_RUNNING" = "Yes" ]; then
    log_success "복제가 정상적으로 작동하고 있습니다!"
else
    log_error "복제 설정에 문제가 있습니다."
    
    # 에러 정보 출력
    LAST_IO_ERROR=$(echo "$SLAVE_STATUS" | grep "Last_IO_Error:" | cut -d: -f2-)
    LAST_SQL_ERROR=$(echo "$SLAVE_STATUS" | grep "Last_SQL_Error:" | cut -d: -f2-)
    
    if [ -n "$LAST_IO_ERROR" ] && [ "$LAST_IO_ERROR" != " " ]; then
        log_error "IO 에러: $LAST_IO_ERROR"
    fi
    
    if [ -n "$LAST_SQL_ERROR" ] && [ "$LAST_SQL_ERROR" != " " ]; then
        log_error "SQL 에러: $LAST_SQL_ERROR"
    fi
    
    exit 1
fi

# 7. 복제 테스트
log_info "복제 테스트 중..."

# Master에 테스트 데이터 삽입
TEST_DB="targetDB"
TEST_TABLE="replication_test"

docker exec mariadb-master mysql -u root -pProdPassword123! -D $TEST_DB -e "
CREATE TABLE IF NOT EXISTS $TEST_TABLE (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_data VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO $TEST_TABLE (test_data) VALUES ('Replication Test - $(date)');
"

# 잠시 대기 후 Slave에서 데이터 확인
sleep 3

SLAVE_COUNT=$(docker exec mariadb-slave mysql -u root -pProdPassword123! -D $TEST_DB -e "SELECT COUNT(*) FROM $TEST_TABLE;" --batch --skip-column-names 2>/dev/null || echo "0")

if [ "$SLAVE_COUNT" -gt "0" ]; then
    log_success "복제 테스트 성공! Slave에 $SLAVE_COUNT 개의 레코드가 복제되었습니다."
    
    # 테스트 테이블 정리
    docker exec mariadb-master mysql -u root -pProdPassword123! -D $TEST_DB -e "DROP TABLE IF EXISTS $TEST_TABLE;"
else
    log_warn "복제 테스트 실패 또는 복제 지연. 수동으로 확인해주세요."
fi

# 8. 최종 상태 요약
log_info "=== 복제 설정 완료 요약 ==="
log_info "Master 컨테이너: mariadb-master (포트: 3307)"
log_info "Slave 컨테이너: mariadb-slave (포트: 3308)"
log_info "복제 사용자: repl_user"
log_info "바이너리 로그 파일: $MASTER_LOG_FILE"
log_info "바이너리 로그 위치: $MASTER_LOG_POS"

log_info "=== 관리 명령어 ==="
log_info "Master 상태 확인: docker exec mariadb-master mysql -u root -pProdPassword123! -e \"SHOW MASTER STATUS;\""
log_info "Slave 상태 확인: docker exec mariadb-slave mysql -u root -pProdPassword123! -e \"SHOW SLAVE STATUS\\G\""
log_info "복제 중지: docker exec mariadb-slave mysql -u root -pProdPassword123! -e \"STOP SLAVE;\""
log_info "복제 시작: docker exec mariadb-slave mysql -u root -pProdPassword123! -e \"START SLAVE;\""

log_success "MariaDB Master-Slave 복제 설정이 완료되었습니다!"
