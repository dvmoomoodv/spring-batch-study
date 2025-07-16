# MSSQL to MariaDB 데이터 이관 API 설계서

## 📋 목차
1. [API 개요](#api-개요)
2. [인증 및 보안](#인증-및-보안)
3. [공통 응답 형식](#공통-응답-형식)
4. [API 엔드포인트](#api-엔드포인트)
5. [에러 코드](#에러-코드)
6. [사용 예시](#사용-예시)

## 🎯 API 개요

### 기본 정보
- **Base URL**: `http://localhost:8080/api/migration`
- **Content-Type**: `application/json`
- **HTTP Methods**: GET, POST, PUT
- **API Version**: v1.0

### 주요 기능
- 전체 데이터 이관 실행
- 특정 테이블 이관 실행
- 배치 설정 조회 및 변경
- 실시간 진행 상황 모니터링

## 🔐 인증 및 보안

### 현재 버전
- **인증 방식**: None (개발 환경)
- **HTTPS**: Optional

### 운영 환경 권장사항
- **인증 방식**: JWT Token 또는 API Key
- **HTTPS**: Required
- **Rate Limiting**: 분당 10회 요청 제한

## 📊 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": {
    // 응답 데이터
  },
  "message": "작업이 성공적으로 완료되었습니다",
  "timestamp": "2024-07-16T10:30:00Z"
}
```

### 실패 응답
```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "오류 메시지",
  "details": "상세 오류 정보",
  "timestamp": "2024-07-16T10:30:00Z"
}
```

## 🚀 API 엔드포인트

### 1. 전체 데이터 이관 시작

#### `POST /api/migration/start`

전체 테이블에 대한 데이터 이관을 시작합니다.

**Request Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| chunkSize | Integer | No | 1000 | 청크 단위 처리 크기 |
| skipLimit | Integer | No | 100 | 허용 가능한 오류 개수 |

**Request Example**
```bash
curl -X POST "http://localhost:8080/api/migration/start?chunkSize=500&skipLimit=50" \
  -H "Content-Type: application/json"
```

**Response Example**
```json
{
  "success": true,
  "jobExecutionId": 1,
  "jobInstanceId": 1,
  "status": "STARTED",
  "startTime": "2024-07-16T10:30:00",
  "message": "Migration job started successfully"
}
```

**Response Fields**
| 필드 | 타입 | 설명 |
|------|------|------|
| success | Boolean | 요청 성공 여부 |
| jobExecutionId | Long | 배치 실행 ID |
| jobInstanceId | Long | 배치 인스턴스 ID |
| status | String | 배치 상태 (STARTED, RUNNING, COMPLETED, FAILED) |
| startTime | DateTime | 시작 시간 |
| message | String | 응답 메시지 |

### 2. 특정 테이블 이관

#### `POST /api/migration/table/{tableName}`

지정된 테이블만 이관합니다.

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| tableName | String | Yes | 이관할 테이블명 (한글) |

**Request Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| whereClause | String | No | WHERE 조건절 |
| chunkSize | Integer | No | 청크 크기 |

**Request Example**
```bash
# 전체 사용자 테이블 이관
curl -X POST "http://localhost:8080/api/migration/table/사용자" \
  -H "Content-Type: application/json"

# 조건부 주문 테이블 이관
curl -X POST "http://localhost:8080/api/migration/table/주문?whereClause=주문일시>='2024-01-01'&chunkSize=200" \
  -H "Content-Type: application/json"
```

**Response Example**
```json
{
  "success": true,
  "tableName": "사용자",
  "jobExecutionId": 2,
  "status": "STARTED",
  "message": "Table migration started successfully"
}
```

### 3. 배치 설정 조회

#### `GET /api/migration/config`

현재 배치 설정 정보를 조회합니다.

**Request Example**
```bash
curl -X GET "http://localhost:8080/api/migration/config" \
  -H "Content-Type: application/json"
```

**Response Example**
```json
{
  "chunkSize": 1000,
  "skipLimit": 100,
  "retryLimit": 3
}
```

### 4. 배치 설정 변경

#### `PUT /api/migration/config`

배치 설정을 동적으로 변경합니다.

**Request Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| chunkSize | Integer | No | 청크 크기 |
| skipLimit | Integer | No | 스킵 제한 |
| retryLimit | Integer | No | 재시도 제한 |

**Request Example**
```bash
curl -X PUT "http://localhost:8080/api/migration/config?chunkSize=2000&skipLimit=200" \
  -H "Content-Type: application/json"
```

**Response Example**
```json
{
  "success": true,
  "message": "Configuration updated successfully",
  "currentConfig": {
    "chunkSize": 2000,
    "skipLimit": 200,
    "retryLimit": 3
  }
}
```

### 5. 배치 상태 조회 (확장 예정)

#### `GET /api/migration/status/{jobExecutionId}`

특정 배치 작업의 상태를 조회합니다.

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| jobExecutionId | Long | Yes | 배치 실행 ID |

**Response Example**
```json
{
  "jobExecutionId": 1,
  "status": "RUNNING",
  "progress": {
    "totalTables": 10,
    "completedTables": 3,
    "currentTable": "주문",
    "processedRecords": 1500,
    "totalRecords": 5000,
    "progressPercentage": 30.0
  },
  "startTime": "2024-07-16T10:30:00",
  "estimatedEndTime": "2024-07-16T10:45:00",
  "errors": []
}
```

### 6. 배치 중지 (확장 예정)

#### `POST /api/migration/stop/{jobExecutionId}`

실행 중인 배치 작업을 중지합니다.

**Response Example**
```json
{
  "success": true,
  "jobExecutionId": 1,
  "status": "STOPPED",
  "message": "Migration job stopped successfully"
}
```

## ❌ 에러 코드

| 에러 코드 | HTTP 상태 | 설명 | 해결방법 |
|-----------|-----------|------|----------|
| JOB_ALREADY_RUNNING | 409 | 이미 실행 중인 작업이 있음 | 기존 작업 완료 후 재시도 |
| JOB_EXECUTION_FAILED | 500 | 배치 실행 실패 | 로그 확인 후 재시도 |
| INVALID_TABLE_NAME | 400 | 잘못된 테이블명 | 올바른 테이블명 확인 |
| DATABASE_CONNECTION_ERROR | 500 | 데이터베이스 연결 오류 | 연결 상태 확인 |
| INVALID_PARAMETERS | 400 | 잘못된 파라미터 | 파라미터 형식 확인 |
| CONFIGURATION_ERROR | 500 | 설정 오류 | 설정 파일 확인 |

## 📝 사용 예시

### 시나리오 1: 전체 데이터 이관

```bash
# 1. 현재 설정 확인
curl -X GET "http://localhost:8080/api/migration/config"

# 2. 필요시 설정 변경
curl -X PUT "http://localhost:8080/api/migration/config?chunkSize=500"

# 3. 전체 이관 시작
curl -X POST "http://localhost:8080/api/migration/start"

# 4. 상태 확인 (주기적으로)
curl -X GET "http://localhost:8080/api/migration/status/1"
```

### 시나리오 2: 특정 테이블 이관

```bash
# 1. 사용자 테이블만 이관
curl -X POST "http://localhost:8080/api/migration/table/사용자"

# 2. 조건부 주문 데이터 이관
curl -X POST "http://localhost:8080/api/migration/table/주문?whereClause=주문일시>='2024-01-01'"

# 3. 대용량 상품 데이터 이관 (청크 크기 조정)
curl -X POST "http://localhost:8080/api/migration/table/상품?chunkSize=2000"
```

### 시나리오 3: 성능 튜닝

```bash
# 1. 소량 데이터용 설정
curl -X PUT "http://localhost:8080/api/migration/config?chunkSize=100&skipLimit=10"

# 2. 대량 데이터용 설정
curl -X PUT "http://localhost:8080/api/migration/config?chunkSize=5000&skipLimit=500"

# 3. 설정 적용 후 이관 실행
curl -X POST "http://localhost:8080/api/migration/start"
```

## 🔧 개발자 가이드

### API 테스트
```bash
# Postman Collection 또는 curl 스크립트 사용
# 모든 API 엔드포인트에 대한 테스트 케이스 포함
```

### 로그 모니터링
```bash
# 실시간 로그 확인
tail -f logs/batch-migration.log

# API 호출 로그 확인
grep "MigrationController" logs/batch-migration.log
```

### 성능 모니터링
- Spring Actuator 엔드포인트 활용
- 배치 실행 메트릭 수집
- 데이터베이스 성능 모니터링

이 API 설계서는 개발 진행에 따라 지속적으로 업데이트됩니다.
