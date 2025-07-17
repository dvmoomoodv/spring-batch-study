# 통합 테스트 목록 (Integration Test List)

## 📋 문서 정보
- **프로젝트명**: MSSQL to MySQL 대규모 데이터 이관 시스템
- **테스트 유형**: 통합 테스트 (Integration Test)
- **총 테스트 수**: 50개
- **테스트 환경**: Docker Testcontainers
- **작성일**: 2024-07-16

## 🎯 1. 통합 테스트 개요

### 1.1 테스트 전략
- **테스트 프레임워크**: Spring Boot Test, Testcontainers, Spring Batch Test
- **실제 환경**: MySQL, MSSQL 실제 컨테이너 사용
- **데이터 격리**: 각 테스트마다 독립적인 데이터 환경
- **테스트 순서**: @TestMethodOrder로 실행 순서 제어

### 1.2 테스트 범위
| 테스트 유형 | 테스트 대상 | 테스트 수 | 비율 |
|-------------|-------------|-----------|------|
| **API 통합** | REST API 전체 플로우 | 15개 | 30% |
| **배치 통합** | Spring Batch Job/Step | 20개 | 40% |
| **DB 통합** | 데이터베이스 연동 | 15개 | 30% |
| **총계** | - | **50개** | **100%** |

### 1.3 테스트 환경 구성
```yaml
# Docker Containers
- MSSQL: mcr.microsoft.com/azure-sql-edge:latest
- MySQL: mysql:8.0
- 네트워크: 격리된 Docker 네트워크
- 데이터: 각 테스트별 독립적인 데이터셋
```

## 🌐 2. API 통합 테스트 (15개)

### 2.1 전체 시스템 API 플로우 테스트 (8개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-001** | `fullMigrationApiFlow_Success` | 전체 이관 API 플로우 | API→Service→Batch→DB 전체 연동 |
| **IT-002** | `tableMigrationApiFlow_Success` | 테이블 이관 API 플로우 | 특정 테이블 전체 플로우 |
| **IT-003** | `domainMigrationApiFlow_Success` | 도메인 이관 API 플로우 | 도메인별 이관 플로우 |
| **IT-004** | `migrationStatusApiFlow_Success` | 상태 조회 API 플로우 | 실시간 상태 조회 |
| **IT-005** | `migrationControlApiFlow_Success` | 제어 API 플로우 | 시작/중지/재시작 플로우 |
| **IT-006** | `configurationApiFlow_Success` | 설정 API 플로우 | 설정 조회/변경 플로우 |
| **IT-007** | `monitoringApiFlow_Success` | 모니터링 API 플로우 | 메트릭/로그 조회 플로우 |
| **IT-008** | `errorHandlingApiFlow_Success` | 오류 처리 API 플로우 | 오류 상황 처리 플로우 |

### 2.2 API 보안 및 권한 테스트 (4개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-009** | `apiAuthentication_Success` | API 인증 테스트 | JWT 토큰 인증 |
| **IT-010** | `apiAuthorization_Success` | API 권한 테스트 | 역할 기반 접근 제어 |
| **IT-011** | `apiRateLimit_Success` | API 속도 제한 테스트 | 요청 빈도 제한 |
| **IT-012** | `apiInputValidation_Success` | API 입력 검증 테스트 | SQL Injection 방어 |

### 2.3 API 성능 및 안정성 테스트 (3개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-013** | `apiConcurrentAccess_Success` | API 동시 접근 테스트 | 동시 요청 처리 |
| **IT-014** | `apiLoadTest_Success` | API 부하 테스트 | 높은 부하 상황 처리 |
| **IT-015** | `apiFailover_Success` | API 장애 복구 테스트 | 장애 상황 복구 |

## 🔄 3. 배치 통합 테스트 (20개)

### 3.1 Job 실행 통합 테스트 (10개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-016** | `dataMigrationJobIntegration_Success` | 전체 이관 Job 실행 | 10개 Step 순차 실행 |
| **IT-017** | `userMigrationJobIntegration_Success` | 사용자 이관 Job 실행 | 사용자 도메인 Job |
| **IT-018** | `productMigrationJobIntegration_Success` | 상품 이관 Job 실행 | 상품 도메인 Job |
| **IT-019** | `orderMigrationJobIntegration_Success` | 주문 이관 Job 실행 | 주문 도메인 Job |
| **IT-020** | `paymentMigrationJobIntegration_Success` | 결제 이관 Job 실행 | 결제 도메인 Job |
| **IT-021** | `inventoryMigrationJobIntegration_Success` | 재고 이관 Job 실행 | 재고 도메인 Job |
| **IT-022** | `deliveryMigrationJobIntegration_Success` | 배송 이관 Job 실행 | 배송 도메인 Job |
| **IT-023** | `customerMigrationJobIntegration_Success` | 고객 이관 Job 실행 | 고객 서비스 Job |
| **IT-024** | `marketingMigrationJobIntegration_Success` | 마케팅 이관 Job 실행 | 마케팅 도메인 Job |
| **IT-025** | `systemMigrationJobIntegration_Success` | 시스템 이관 Job 실행 | 시스템 관리 Job |

### 3.2 Step 실행 통합 테스트 (6개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-026** | `stepExecutionOrder_Success` | Step 실행 순서 검증 | 의존성 순서 확인 |
| **IT-027** | `stepDataFlow_Success` | Step 간 데이터 플로우 | 데이터 전달 확인 |
| **IT-028** | `stepErrorHandling_Success` | Step 오류 처리 | Skip/Retry 동작 |
| **IT-029** | `stepPerformance_Success` | Step 성능 검증 | 처리 속도 확인 |
| **IT-030** | `stepRestart_Success` | Step 재시작 검증 | 실패 후 재시작 |
| **IT-031** | `stepParallelExecution_Success` | Step 병렬 실행 | 독립 Step 병렬 처리 |

### 3.3 배치 메타데이터 통합 테스트 (4개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-032** | `jobRepository_Success` | Job Repository 통합 | 메타데이터 저장/조회 |
| **IT-033** | `jobExecution_Success` | Job Execution 관리 | 실행 이력 관리 |
| **IT-034** | `stepExecution_Success` | Step Execution 관리 | Step 실행 이력 |
| **IT-035** | `batchMetrics_Success` | 배치 메트릭 수집 | 성능 지표 수집 |

## 💾 4. 데이터베이스 통합 테스트 (15개)

### 4.1 데이터 이관 검증 테스트 (8개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-036** | `dataIntegrityValidation_Success` | 데이터 정합성 검증 | 소스↔타겟 데이터 일치 |
| **IT-037** | `dataTransformationValidation_Success` | 데이터 변환 검증 | 한글→영어 변환 확인 |
| **IT-038** | `foreignKeyConstraints_Success` | 외래키 제약조건 검증 | 참조 무결성 확인 |
| **IT-039** | `dataTypeConversion_Success` | 데이터 타입 변환 검증 | 타입 호환성 확인 |
| **IT-040** | `nullValueHandling_Success` | NULL 값 처리 검증 | NULL 값 처리 확인 |
| **IT-041** | `duplicateDataHandling_Success` | 중복 데이터 처리 | 중복 처리 로직 |
| **IT-042** | `largeDataMigration_Success` | 대용량 데이터 이관 | 100만 건 이상 처리 |
| **IT-043** | `incrementalMigration_Success` | 증분 이관 검증 | 변경분만 이관 |

### 4.2 데이터베이스 연결 및 트랜잭션 테스트 (4개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-044** | `multiDatabaseConnection_Success` | 다중 DB 연결 | MSSQL, MySQL 동시 연결 |
| **IT-045** | `transactionManagement_Success` | 트랜잭션 관리 | 커밋/롤백 처리 |
| **IT-046** | `connectionPooling_Success` | 연결 풀 관리 | 연결 풀 효율성 |
| **IT-047** | `databaseFailover_Success` | DB 장애 복구 | 연결 실패 시 복구 |

### 4.3 성능 및 최적화 테스트 (3개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **IT-048** | `queryPerformance_Success` | 쿼리 성능 검증 | 쿼리 실행 시간 |
| **IT-049** | `indexEffectiveness_Success` | 인덱스 효과 검증 | 인덱스 성능 향상 |
| **IT-050** | `batchInsertPerformance_Success` | 배치 INSERT 성능 | 대량 INSERT 성능 |

## 🔧 5. 테스트 환경 및 설정

### 5.1 Testcontainers 설정
```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("targetdb")
    .withUsername("test")
    .withPassword("test")
    .withInitScript("init-mysql.sql");

@Container
static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/azure-sql-edge:latest")
    .withPassword("Test123!")
    .withEnv("ACCEPT_EULA", "Y")
    .withInitScript("init-mssql.sql");
```

### 5.2 테스트 데이터 관리
| 데이터 유형 | 레코드 수 | 용도 | 생성 방법 |
|-------------|-----------|------|-----------|
| **기본 테스트 데이터** | 각 테이블 10건 | 기능 검증 | SQL 스크립트 |
| **중간 테스트 데이터** | 각 테이블 1,000건 | 성능 검증 | Java 코드 생성 |
| **대용량 테스트 데이터** | 각 테이블 100,000건 | 부하 테스트 | 배치 생성 |
| **오류 테스트 데이터** | 의도적 오류 데이터 | 오류 처리 검증 | 수동 생성 |

### 5.3 테스트 실행 환경
```yaml
# 테스트 실행 요구사항
- Docker: 20.10 이상
- 메모리: 최소 8GB (권장 16GB)
- 디스크: 최소 10GB 여유 공간
- 네트워크: 인터넷 연결 (컨테이너 이미지 다운로드)
```

## 📊 6. 테스트 실행 및 관리

### 6.1 테스트 실행 명령어
```bash
# 전체 통합 테스트 실행
./gradlew integrationTest

# 특정 유형 테스트 실행
./gradlew test --tests "*ApiIntegrationTest"
./gradlew test --tests "*BatchIntegrationTest"
./gradlew test --tests "*DatabaseIntegrationTest"

# 성능 테스트 포함 실행
./gradlew test --tests "*IntegrationTest" -Dspring.profiles.active=integration
```

### 6.2 테스트 실행 시간
| 테스트 유형 | 예상 실행 시간 | 병렬 실행 | 비고 |
|-------------|----------------|-----------|------|
| **API 통합 테스트** | 10분 | 가능 | 경량 테스트 |
| **배치 통합 테스트** | 30분 | 제한적 | 순차 실행 필요 |
| **DB 통합 테스트** | 20분 | 가능 | 데이터 격리 필요 |
| **전체 통합 테스트** | 60분 | 혼합 | 총 실행 시간 |

### 6.3 테스트 품질 관리
| 품질 지표 | 목표값 | 측정 방법 | 개선 방안 |
|-----------|--------|-----------|-----------|
| **테스트 통과율** | 95% 이상 | 자동화된 실행 | 실패 원인 분석 |
| **테스트 안정성** | 98% 이상 | 반복 실행 | 환경 표준화 |
| **데이터 정합성** | 100% | 검증 로직 | 검증 강화 |
| **성능 기준 달성** | 90% 이상 | 성능 측정 | 최적화 작업 |

### 6.4 CI/CD 통합
```yaml
# GitHub Actions 워크플로우
name: Integration Tests
on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Run Integration Tests
        run: ./gradlew integrationTest
        
      - name: Publish Test Results
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Integration Test Results
          path: build/test-results/integrationTest/*.xml
          reporter: java-junit
```

### 6.5 테스트 결과 분석
- **성공률 추적**: 일별/주별 성공률 모니터링
- **실행 시간 분석**: 성능 저하 구간 식별
- **실패 패턴 분석**: 반복적 실패 원인 파악
- **리소스 사용량**: 메모리/CPU 사용량 모니터링

### 6.6 테스트 유지보수
- **정기 업데이트**: 월 1회 테스트 케이스 검토
- **환경 업데이트**: 컨테이너 이미지 정기 업데이트
- **성능 기준 조정**: 시스템 성능 변화에 따른 기준 조정
- **테스트 데이터 관리**: 테스트 데이터 정기 갱신

이 통합 테스트 목록은 대규모 데이터 이관 시스템의 전체적인 동작을 검증하고 시스템 간 연동을 보장하기 위한 포괄적인 테스트 계획을 제시합니다.
