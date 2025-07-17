# 단위 테스트 목록 (Unit Test List)

## 📋 문서 정보
- **프로젝트명**: MSSQL to MySQL 대규모 데이터 이관 시스템
- **테스트 유형**: 단위 테스트 (Unit Test)
- **총 테스트 수**: 140개
- **커버리지 목표**: 85% 이상
- **작성일**: 2024-07-16

## 🎯 1. 단위 테스트 개요

### 1.1 테스트 전략
- **테스트 프레임워크**: JUnit 5, Mockito, Spring Boot Test
- **모킹 전략**: 외부 의존성 모킹으로 격리된 테스트
- **테스트 패턴**: Given-When-Then 패턴 적용
- **네이밍 규칙**: `메서드명_시나리오_예상결과` 형식

### 1.2 테스트 범위
| 계층 | 테스트 대상 | 테스트 수 | 비율 |
|------|-------------|-----------|------|
| **Controller** | REST API 엔드포인트 | 32개 | 23% |
| **Service** | 비즈니스 로직 | 42개 | 30% |
| **Processor** | 데이터 변환 로직 | 28개 | 20% |
| **Utility** | 유틸리티 함수 | 38개 | 27% |
| **총계** | - | **140개** | **100%** |

## 🌐 2. Controller 단위 테스트 (32개)

### 2.1 MigrationController 테스트 (12개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UC-001** | `startFullMigration_Success` | 전체 이관 시작 - 성공 | HTTP 200, jobExecutionId 반환 |
| **UC-002** | `startFullMigration_InvalidParameters` | 잘못된 파라미터 전달 | HTTP 400, 에러 메시지 |
| **UC-003** | `startTableMigration_Success` | 특정 테이블 이관 - 성공 | HTTP 200, tableName 포함 |
| **UC-004** | `startTableMigration_TableNotFound` | 존재하지 않는 테이블 | HTTP 400, TABLE_NOT_FOUND |
| **UC-005** | `stopMigration_Success` | 배치 중지 - 성공 | HTTP 200, STOPPED 상태 |
| **UC-006** | `restartMigration_Success` | 배치 재시작 - 성공 | HTTP 200, RESTARTED 상태 |
| **UC-007** | `getAllJobs_Success` | Job 목록 조회 - 성공 | HTTP 200, 페이징 정보 |
| **UC-008** | `startDomainMigration_Success` | 도메인 이관 - 성공 | HTTP 200, domainName 포함 |
| **UC-009** | `startMigration_JobAlreadyRunning` | 중복 실행 시도 | HTTP 409, JOB_ALREADY_RUNNING |
| **UC-010** | `startMigration_UnauthorizedAccess` | 권한 없는 접근 | HTTP 401, UNAUTHORIZED |
| **UC-011** | `getMigrationStatus_Success` | 상태 조회 - 성공 | HTTP 200, 진행률 정보 |
| **UC-012** | `validateRequestParameters` | 요청 파라미터 검증 | 파라미터 유효성 검사 |

### 2.2 MonitoringController 테스트 (8개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UC-013** | `getJobStatus_Success` | Job 상태 조회 - 성공 | HTTP 200, 상태 정보 |
| **UC-014** | `getJobStatus_NotFound` | 존재하지 않는 Job | HTTP 404, JOB_NOT_FOUND |
| **UC-015** | `getMigrationProgress_Success` | 진행률 조회 - 성공 | HTTP 200, 진행률 % |
| **UC-016** | `getPerformanceMetrics_Success` | 성능 메트릭 조회 | HTTP 200, 메트릭 데이터 |
| **UC-017** | `getJobLogs_Success` | Job 로그 조회 - 성공 | HTTP 200, 로그 데이터 |
| **UC-018** | `getErrorSummary_Success` | 오류 요약 조회 | HTTP 200, 오류 통계 |
| **UC-019** | `getRealTimeStatus_WebSocket` | 실시간 상태 조회 | WebSocket 연결 |
| **UC-020** | `getMetrics_DateRange` | 기간별 메트릭 조회 | 날짜 범위 필터링 |

### 2.3 ConfigController 테스트 (8개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UC-021** | `getBatchConfig_Success` | 배치 설정 조회 | HTTP 200, 설정 정보 |
| **UC-022** | `updateBatchConfig_Success` | 배치 설정 변경 | HTTP 200, 변경 확인 |
| **UC-023** | `getDatasourceConfig_Success` | 데이터소스 설정 조회 | HTTP 200, DB 설정 |
| **UC-024** | `getMappingRules_Success` | 매핑 규칙 조회 | HTTP 200, 매핑 정보 |
| **UC-025** | `updateMappingRules_Success` | 매핑 규칙 변경 | HTTP 200, 규칙 업데이트 |
| **UC-026** | `validateConfiguration` | 설정 유효성 검증 | 설정값 검증 로직 |
| **UC-027** | `resetConfiguration` | 설정 초기화 | 기본값 복원 |
| **UC-028** | `exportConfiguration` | 설정 내보내기 | JSON 형태 내보내기 |

### 2.4 HealthController 테스트 (4개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UC-029** | `checkDatabaseHealth_Success` | DB 연결 상태 확인 | HTTP 200, 연결 상태 |
| **UC-030** | `checkBatchHealth_Success` | 배치 시스템 상태 | HTTP 200, 시스템 상태 |
| **UC-031** | `checkSystemHealth_Success` | 전체 시스템 상태 | HTTP 200, 종합 상태 |
| **UC-032** | `getMemoryStatus_Success` | 메모리 사용량 조회 | HTTP 200, 메모리 정보 |

## 💼 3. Service 단위 테스트 (42개)

### 3.1 MigrationService 테스트 (18개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **US-001** | `startFullMigration_Success` | 전체 이관 시작 - 성공 | JobLauncher 호출, 결과 반환 |
| **US-002** | `startFullMigration_JobAlreadyRunning` | Job 이미 실행 중 | MigrationException 발생 |
| **US-003** | `startTableMigration_Success` | 테이블 이관 - 성공 | 단일 테이블 Job 실행 |
| **US-004** | `startTableMigration_TableNotFound` | 존재하지 않는 테이블 | IllegalArgumentException |
| **US-005** | `startDomainMigration_Success` | 도메인 이관 - 성공 | 도메인 Job 실행 |
| **US-006** | `stopMigration_Success` | 배치 중지 - 성공 | Job 중지 처리 |
| **US-007** | `restartMigration_Success` | 배치 재시작 - 성공 | Job 재시작 처리 |
| **US-008** | `getJobStatus_Success` | Job 상태 조회 | 상태 정보 반환 |
| **US-009** | `validateJobParameters_InvalidChunkSize` | 잘못된 청크 크기 | IllegalArgumentException |
| **US-010** | `validateJobParameters_InvalidSkipLimit` | 잘못된 스킵 제한 | IllegalArgumentException |
| **US-011** | `preventConcurrentExecution_SameTable` | 동일 테이블 중복 실행 | MigrationException |
| **US-012** | `createJobParameters_Success` | Job 파라미터 생성 | 파라미터 검증 |
| **US-013** | `calculateEstimatedTime` | 예상 소요 시간 계산 | 시간 계산 로직 |
| **US-014** | `handleJobFailure` | Job 실패 처리 | 실패 처리 로직 |
| **US-015** | `scheduleJob_Success` | Job 스케줄링 | 스케줄 등록 |
| **US-016** | `cancelScheduledJob` | 스케줄된 Job 취소 | 스케줄 취소 |
| **US-017** | `getJobHistory` | Job 실행 이력 조회 | 이력 정보 반환 |
| **US-018** | `cleanupOldJobs` | 오래된 Job 정리 | 정리 로직 실행 |

### 3.2 MonitoringService 테스트 (8개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **US-019** | `getJobStatus_Success` | Job 상태 조회 | 상태 정보 수집 |
| **US-020** | `getMigrationProgress_Success` | 진행률 계산 | 진행률 % 계산 |
| **US-021** | `getPerformanceMetrics_Success` | 성능 메트릭 수집 | 메트릭 데이터 수집 |
| **US-022** | `collectRealTimeMetrics` | 실시간 메트릭 수집 | 실시간 데이터 수집 |
| **US-023** | `calculateProcessingSpeed` | 처리 속도 계산 | 속도 계산 로직 |
| **US-024** | `detectAnomalies` | 이상 상황 감지 | 임계값 기반 감지 |
| **US-025** | `generateReport` | 리포트 생성 | 리포트 데이터 생성 |
| **US-026** | `sendAlert` | 알림 발송 | 알림 로직 실행 |

### 3.3 기타 Service 테스트 (16개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **US-027** | `ConfigService.updateBatchConfig` | 배치 설정 업데이트 | 설정 변경 로직 |
| **US-028** | `ConfigService.validateConfig` | 설정 유효성 검증 | 검증 로직 |
| **US-029** | `ValidationService.validateTableData` | 테이블 데이터 검증 | 데이터 유효성 검사 |
| **US-030** | `ValidationService.checkDataIntegrity` | 데이터 무결성 검사 | 무결성 검증 |
| **US-031** | `TableMappingService.getTableMapping` | 테이블 매핑 조회 | 매핑 정보 반환 |
| **US-032** | `TableMappingService.updateMapping` | 매핑 정보 업데이트 | 매핑 변경 |
| **US-033** | `NotificationService.sendJobStart` | Job 시작 알림 | 알림 발송 |
| **US-034** | `NotificationService.sendJobComplete` | Job 완료 알림 | 완료 알림 |
| **US-035** | `NotificationService.sendError` | 오류 알림 | 오류 알림 발송 |
| **US-036** | `SecurityService.validateAccess` | 접근 권한 검증 | 권한 확인 |
| **US-037** | `SecurityService.encryptSensitiveData` | 민감 데이터 암호화 | 암호화 처리 |
| **US-038** | `CacheService.getCachedData` | 캐시 데이터 조회 | 캐시 조회 |
| **US-039** | `CacheService.updateCache` | 캐시 업데이트 | 캐시 갱신 |
| **US-040** | `MetricsService.recordMetric` | 메트릭 기록 | 메트릭 저장 |
| **US-041** | `MetricsService.aggregateMetrics` | 메트릭 집계 | 집계 계산 |
| **US-042** | `SchedulerService.scheduleTask` | 작업 스케줄링 | 스케줄 등록 |

## 🔄 4. Processor 단위 테스트 (28개)

### 4.1 DataTransformProcessor 테스트 (16개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UP-001** | `process_UserData_Success` | 사용자 데이터 변환 | 한글→영어 변환 |
| **UP-002** | `process_ProductData_Success` | 상품 데이터 변환 | 상품 정보 변환 |
| **UP-003** | `process_OrderData_StatusTransformation` | 주문 상태 변환 | 상태값 변환 |
| **UP-004** | `process_UnknownTable_ThrowsException` | 존재하지 않는 테이블 | DataTransformException |
| **UP-005** | `process_MissingRequiredColumn_ThrowsException` | 필수 컬럼 누락 | DataTransformException |
| **UP-006** | `process_DataTypeConversionError_ThrowsException` | 타입 변환 오류 | DataTransformException |
| **UP-007** | `process_NullValues_Success` | NULL 값 처리 | NULL 값 허용 |
| **UP-008** | `process_LargeData_Performance` | 대용량 데이터 처리 | 성능 검증 |
| **UP-009** | `transformTableName_Success` | 테이블명 변환 | 매핑 테이블 사용 |
| **UP-010** | `transformColumnName_Success` | 컬럼명 변환 | 컬럼 매핑 |
| **UP-011** | `transformValue_Success` | 값 변환 | 값 매핑 규칙 |
| **UP-012** | `addMetadata_Success` | 메타데이터 추가 | migrated_at 추가 |
| **UP-013** | `validateDataTypes` | 데이터 타입 검증 | 타입 호환성 |
| **UP-014** | `handleSpecialCharacters` | 특수 문자 처리 | 인코딩 처리 |
| **UP-015** | `processEmptyRecord` | 빈 레코드 처리 | 빈 데이터 처리 |
| **UP-016** | `processDuplicateData` | 중복 데이터 처리 | 중복 처리 로직 |

### 4.2 기타 Processor 테스트 (12개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UP-017** | `ValidationProcessor.validateData` | 데이터 검증 | 유효성 검사 |
| **UP-018** | `ValidationProcessor.checkConstraints` | 제약조건 검사 | 제약조건 검증 |
| **UP-019** | `TypeConversionProcessor.convertTypes` | 타입 변환 | 데이터 타입 변환 |
| **UP-020** | `TypeConversionProcessor.handleConversionError` | 변환 오류 처리 | 오류 처리 |
| **UP-021** | `CompositeProcessor.processChain` | 복합 처리 | 여러 프로세서 연결 |
| **UP-022** | `CompositeProcessor.handleChainError` | 체인 오류 처리 | 체인 중단 처리 |
| **UP-023** | `FilterProcessor.filterData` | 데이터 필터링 | 조건부 필터링 |
| **UP-024** | `FilterProcessor.applyBusinessRules` | 비즈니스 규칙 적용 | 규칙 기반 처리 |
| **UP-025** | `EnrichmentProcessor.enrichData` | 데이터 보강 | 추가 정보 보강 |
| **UP-026** | `EnrichmentProcessor.lookupReference` | 참조 데이터 조회 | 외부 데이터 조회 |
| **UP-027** | `AggregationProcessor.aggregateData` | 데이터 집계 | 집계 처리 |
| **UP-028** | `AggregationProcessor.calculateSummary` | 요약 계산 | 요약 통계 |

## 🛠️ 5. Utility 단위 테스트 (38개)

### 5.1 매핑 유틸리티 테스트 (18개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UU-001** | `TableMappingUtil.getTargetTableName_Success` | 테이블명 매핑 조회 | 한글→영어 매핑 |
| **UU-002** | `TableMappingUtil.getTargetTableName_NotFound` | 매핑 없는 테이블 | 예외 발생 |
| **UU-003** | `ColumnMappingUtil.getTargetColumnName_Success` | 컬럼명 매핑 조회 | 컬럼 매핑 |
| **UU-004** | `ColumnMappingUtil.getColumnType_Success` | 컬럼 타입 조회 | 타입 정보 |
| **UU-005** | `ValueTransformUtil.transformValue_Success` | 값 변환 | 값 매핑 |
| **UU-006** | `ValueTransformUtil.transformEnum_Success` | 열거형 변환 | ENUM 매핑 |
| **UU-007** | `ValueTransformUtil.transformBoolean_Success` | 불린 변환 | Boolean 매핑 |
| **UU-008** | `ValueTransformUtil.transformDate_Success` | 날짜 변환 | 날짜 형식 변환 |
| **UU-009** | `MappingCacheUtil.getCachedMapping` | 매핑 캐시 조회 | 캐시 활용 |
| **UU-010** | `MappingCacheUtil.updateCache` | 캐시 업데이트 | 캐시 갱신 |
| **UU-011** | `MappingValidatorUtil.validateMapping` | 매핑 유효성 검증 | 매핑 검증 |
| **UU-012** | `MappingLoaderUtil.loadMappingRules` | 매핑 규칙 로드 | 규칙 로딩 |
| **UU-013** | `MappingExportUtil.exportMappings` | 매핑 내보내기 | 매핑 내보내기 |
| **UU-014** | `MappingImportUtil.importMappings` | 매핑 가져오기 | 매핑 가져오기 |
| **UU-015** | `MappingCompareUtil.compareMappings` | 매핑 비교 | 매핑 차이 분석 |
| **UU-016** | `MappingStatUtil.getMappingStats` | 매핑 통계 | 매핑 사용 통계 |
| **UU-017** | `MappingBackupUtil.backupMappings` | 매핑 백업 | 매핑 백업 |
| **UU-018** | `MappingRestoreUtil.restoreMappings` | 매핑 복원 | 매핑 복원 |

### 5.2 기타 유틸리티 테스트 (20개)
| 테스트 ID | 테스트 메서드명 | 테스트 시나리오 | 검증 내용 |
|-----------|----------------|-----------------|-----------|
| **UU-019** | `SqlBuilder.buildSelectQuery` | SELECT 쿼리 생성 | 동적 쿼리 생성 |
| **UU-020** | `SqlBuilder.buildInsertQuery` | INSERT 쿼리 생성 | 배치 INSERT |
| **UU-021** | `SqlBuilder.addWhereClause` | WHERE 절 추가 | 조건절 생성 |
| **UU-022** | `DateTimeUtil.formatDateTime` | 날짜 포맷팅 | 날짜 형식 변환 |
| **UU-023** | `DateTimeUtil.parseDateTime` | 날짜 파싱 | 문자열→날짜 |
| **UU-024** | `DateTimeUtil.calculateDuration` | 기간 계산 | 시간 차이 계산 |
| **UU-025** | `StringUtil.convertToSnakeCase` | Snake Case 변환 | 문자열 변환 |
| **UU-026** | `StringUtil.convertToCamelCase` | Camel Case 변환 | 문자열 변환 |
| **UU-027** | `StringUtil.sanitizeString` | 문자열 정제 | 특수문자 제거 |
| **UU-028** | `ValidationUtil.isValidEmail` | 이메일 검증 | 이메일 형식 |
| **UU-029** | `ValidationUtil.isValidPhoneNumber` | 전화번호 검증 | 전화번호 형식 |
| **UU-030** | `ValidationUtil.isValidDate` | 날짜 검증 | 날짜 유효성 |
| **UU-031** | `EncryptionUtil.encrypt` | 데이터 암호화 | 암호화 처리 |
| **UU-032** | `EncryptionUtil.decrypt` | 데이터 복호화 | 복호화 처리 |
| **UU-033** | `CompressionUtil.compress` | 데이터 압축 | 압축 처리 |
| **UU-034** | `CompressionUtil.decompress` | 데이터 압축 해제 | 압축 해제 |
| **UU-035** | `FileUtil.readFile` | 파일 읽기 | 파일 처리 |
| **UU-036** | `FileUtil.writeFile` | 파일 쓰기 | 파일 생성 |
| **UU-037** | `JsonUtil.toJson` | JSON 변환 | 객체→JSON |
| **UU-038** | `JsonUtil.fromJson` | JSON 파싱 | JSON→객체 |

## 📊 6. 테스트 실행 및 관리

### 6.1 테스트 실행 명령어
```bash
# 전체 단위 테스트 실행
./gradlew test --tests "*Test"

# 특정 계층 테스트 실행
./gradlew test --tests "*ControllerTest"
./gradlew test --tests "*ServiceTest"
./gradlew test --tests "*ProcessorTest"
./gradlew test --tests "*UtilTest"

# 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

### 6.2 테스트 품질 기준
| 품질 지표 | 목표값 | 측정 방법 |
|-----------|--------|-----------|
| **테스트 통과율** | 100% | 모든 테스트 통과 |
| **코드 커버리지** | 85% 이상 | JaCoCo 측정 |
| **테스트 실행 시간** | 5분 이내 | 단위 테스트만 |
| **테스트 안정성** | 99% 이상 | 반복 실행 성공률 |

### 6.3 테스트 유지보수
- **정기 리뷰**: 월 1회 테스트 코드 리뷰
- **리팩토링**: 중복 코드 제거 및 구조 개선
- **업데이트**: 새 기능 추가 시 테스트 추가
- **성능 모니터링**: 테스트 실행 시간 모니터링

이 단위 테스트 목록은 대규모 데이터 이관 시스템의 품질을 보장하기 위한 포괄적인 테스트 계획을 제시합니다.
