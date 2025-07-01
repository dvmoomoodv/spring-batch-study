#!/bin/bash

# MSSQL to MariaDB 데이터 이관 배치 실행 스크립트

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

# 기본 설정
JAR_FILE="build/libs/batch-migration-1.0-SNAPSHOT.jar"
DEFAULT_PROFILE="dev"
DEFAULT_CHUNK_SIZE=1000
DEFAULT_SKIP_LIMIT=100

# 사용법 출력
usage() {
    echo "사용법: $0 [옵션]"
    echo ""
    echo "옵션:"
    echo "  -p, --profile PROFILE     실행 프로파일 (dev|prod) [기본값: $DEFAULT_PROFILE]"
    echo "  -c, --chunk-size SIZE     청크 사이즈 [기본값: $DEFAULT_CHUNK_SIZE]"
    echo "  -s, --skip-limit LIMIT    스킵 제한 [기본값: $DEFAULT_SKIP_LIMIT]"
    echo "  -t, --table TABLE         특정 테이블만 이관"
    echo "  -w, --where CONDITION     WHERE 조건"
    echo "  -m, --memory MEMORY       JVM 힙 메모리 (예: 2g, 4g)"
    echo "  -h, --help               이 도움말 출력"
    echo ""
    echo "예시:"
    echo "  $0 --profile prod --chunk-size 2000"
    echo "  $0 --table users --where \"created_date >= '2024-01-01'\""
    echo "  $0 --profile prod --memory 4g"
}

# 파라미터 파싱
PROFILE=$DEFAULT_PROFILE
CHUNK_SIZE=$DEFAULT_CHUNK_SIZE
SKIP_LIMIT=$DEFAULT_SKIP_LIMIT
TABLE=""
WHERE_CONDITION=""
MEMORY=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -c|--chunk-size)
            CHUNK_SIZE="$2"
            shift 2
            ;;
        -s|--skip-limit)
            SKIP_LIMIT="$2"
            shift 2
            ;;
        -t|--table)
            TABLE="$2"
            shift 2
            ;;
        -w|--where)
            WHERE_CONDITION="$2"
            shift 2
            ;;
        -m|--memory)
            MEMORY="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            log_error "알 수 없는 옵션: $1"
            usage
            exit 1
            ;;
    esac
done

# JAR 파일 존재 확인
if [ ! -f "$JAR_FILE" ]; then
    log_error "JAR 파일을 찾을 수 없습니다: $JAR_FILE"
    log_info "먼저 빌드를 실행하세요: ./gradlew build"
    exit 1
fi

# 로그 디렉토리 생성
mkdir -p logs

# JVM 옵션 설정
JVM_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication"

if [ -n "$MEMORY" ]; then
    JVM_OPTS="$JVM_OPTS -Xms${MEMORY} -Xmx${MEMORY}"
    log_info "JVM 메모리 설정: $MEMORY"
else
    JVM_OPTS="$JVM_OPTS -Xms1g -Xmx2g"
fi

# 애플리케이션 옵션 설정
APP_OPTS="--spring.profiles.active=$PROFILE"
APP_OPTS="$APP_OPTS --batch.chunk-size=$CHUNK_SIZE"
APP_OPTS="$APP_OPTS --batch.skip-limit=$SKIP_LIMIT"

# 실행 정보 출력
log_info "=== 데이터 이관 배치 실행 ==="
log_info "프로파일: $PROFILE"
log_info "청크 사이즈: $CHUNK_SIZE"
log_info "스킵 제한: $SKIP_LIMIT"

if [ -n "$TABLE" ]; then
    log_info "대상 테이블: $TABLE"
fi

if [ -n "$WHERE_CONDITION" ]; then
    log_info "WHERE 조건: $WHERE_CONDITION"
fi

log_info "JVM 옵션: $JVM_OPTS"
log_info "애플리케이션 옵션: $APP_OPTS"
log_info "=========================="

# 실행 시작
log_info "배치 애플리케이션을 시작합니다..."

# 백그라운드에서 실행하고 PID 저장
java $JVM_OPTS -jar "$JAR_FILE" $APP_OPTS &
PID=$!

echo $PID > batch-migration.pid
log_success "배치 애플리케이션이 시작되었습니다 (PID: $PID)"

# 애플리케이션이 시작될 때까지 대기
log_info "애플리케이션 시작을 기다리는 중..."
sleep 10

# 애플리케이션 상태 확인
if kill -0 $PID 2>/dev/null; then
    log_success "애플리케이션이 정상적으로 실행 중입니다"
    
    # REST API를 통한 배치 실행
    if [ -n "$TABLE" ]; then
        # 특정 테이블 이관
        API_URL="http://localhost:8080/api/migration/table/$TABLE"
        if [ -n "$WHERE_CONDITION" ]; then
            API_URL="$API_URL?whereClause=$(echo "$WHERE_CONDITION" | sed 's/ /%20/g')"
        fi
        
        log_info "특정 테이블 이관을 시작합니다: $TABLE"
        curl -X POST "$API_URL" -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "배치 실행 요청을 보냈습니다"
    else
        # 전체 이관
        log_info "전체 데이터 이관을 시작합니다"
        curl -X POST "http://localhost:8080/api/migration/start" -H "Content-Type: application/json" | jq '.' 2>/dev/null || echo "배치 실행 요청을 보냈습니다"
    fi
    
    echo ""
    log_info "=== 모니터링 명령어 ==="
    log_info "실시간 로그 확인: tail -f logs/batch-migration.log"
    log_info "배치 처리 로그: tail -f logs/batch-processing.log"
    log_info "에러 로그: tail -f logs/batch-errors.log"
    log_info "애플리케이션 중지: kill $PID"
    log_info "======================="
    
else
    log_error "애플리케이션 시작에 실패했습니다"
    exit 1
fi
