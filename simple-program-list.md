# 대규모 데이터 이관 시스템 프로그램 목록 (간소화)

## 📋 프로젝트 개요
- **프로젝트명**: MSSQL to MySQL 대규모 데이터 이관 시스템
- **규모**: 600개 테이블 → 200개 테이블 (10개 도메인)
- **아키텍처**: Spring Boot + Spring Batch + MySQL
- **작성일**: 2024-07-16

## 🎯 1. 시스템 구성 및 네이밍 규칙

### 1.1 도메인 구성
| 도메인 | 영문명 | 소스 테이블 | 타겟 테이블 | 복잡도 |
|--------|--------|-------------|-------------|---------|
| 사용자 관리 | USER | 80개 | 25개 | 중간 |
| 상품 관리 | PRODUCT | 100개 | 30개 | 높음 |
| 주문 관리 | ORDER | 120개 | 35개 | 높음 |
| 결제 관리 | PAYMENT | 60개 | 20개 | 높음 |
| 재고 관리 | INVENTORY | 50개 | 15개 | 중간 |
| 배송 관리 | DELIVERY | 40개 | 12개 | 중간 |
| 고객 서비스 | CUSTOMER | 45개 | 18개 | 중간 |
| 마케팅 | MARKETING | 35개 | 15개 | 낮음 |
| 분석 | ANALYTICS | 50개 | 20개 | 높음 |
| 시스템 관리 | SYSTEM | 20개 | 10개 | 낮음 |

### 1.2 네이밍 규칙
- **Job**: `{Domain}MigrationJob` (예: UserMigrationJob)
- **Step**: `{Domain}{Table}MigrationStep` (예: UserAccountMigrationStep)
- **Controller**: `{Domain}MigrationController` (예: UserMigrationController)
- **Service**: `{Domain}MigrationService` (예: UserMigrationService)

## 📁 2. 핵심 프로그램 목록

### 2.1 공통 기반 시스템 (15개)

| 프로그램ID | 프로그램명 | 파일명 | 기능 |
|-----------|-----------|--------|------|
| **MAIN-001** | 메인 애플리케이션 | BatchMigrationApplication.java | Spring Boot 시작점 |
| **CONF-001** | 데이터소스 설정 | DataSourceConfig.java | MSSQL, MySQL 설정 |
| **CONF-002** | 배치 기본 설정 | BatchConfig.java | Spring Batch 설정 |
| **CONF-003** | 보안 설정 | SecurityConfig.java | API 보안 설정 |
| **CTRL-001** | 메인 컨트롤러 | MainMigrationController.java | 전체 이관 제어 API |
| **CTRL-002** | 모니터링 컨트롤러 | MonitoringController.java | 진행 상황 조회 API |
| **SERV-001** | 메인 서비스 | MainMigrationService.java | 전체 이관 로직 |
| **SERV-002** | 모니터링 서비스 | MonitoringService.java | 상태 모니터링 |
| **UTIL-001** | 테이블 매핑 유틸 | TableMappingUtil.java | 테이블명 변환 |
| **UTIL-002** | 데이터 변환 유틸 | DataTransformUtil.java | 데이터 변환 |
| **MODL-001** | 공통 데이터 모델 | DataRecord.java | 이관 데이터 모델 |
| **MODL-002** | 배치 설정 모델 | BatchConfiguration.java | 설정 정보 모델 |
| **EXCP-001** | 공통 예외 | MigrationException.java | 이관 관련 예외 |
| **LIST-001** | Job 리스너 | JobExecutionListener.java | Job 실행 모니터링 |
| **LIST-002** | Step 리스너 | StepExecutionListener.java | Step 실행 모니터링 |

### 2.2 도메인별 프로그램 (10개 도메인 × 6개 = 60개)

#### 2.2.1 USER 도메인 (6개)
| 프로그램ID | 프로그램명 | 파일명 | 기능 |
|-----------|-----------|--------|------|
| **USER-001** | 사용자 Job 설정 | UserMigrationJobConfig.java | 사용자 도메인 Job |
| **USER-002** | 사용자 Step 설정 | UserMigrationStepConfig.java | 사용자 테이블 Step들 |
| **USER-003** | 사용자 컨트롤러 | UserMigrationController.java | 사용자 이관 API |
| **USER-004** | 사용자 서비스 | UserMigrationService.java | 사용자 이관 로직 |
| **USER-005** | 사용자 Reader | UserItemReader.java | 사용자 데이터 읽기 |
| **USER-006** | 사용자 Processor | UserDataProcessor.java | 사용자 데이터 변환 |

#### 2.2.2 PRODUCT 도메인 (6개)
| 프로그램ID | 프로그램명 | 파일명 | 기능 |
|-----------|-----------|--------|------|
| **PROD-001** | 상품 Job 설정 | ProductMigrationJobConfig.java | 상품 도메인 Job |
| **PROD-002** | 상품 Step 설정 | ProductMigrationStepConfig.java | 상품 테이블 Step들 |
| **PROD-003** | 상품 컨트롤러 | ProductMigrationController.java | 상품 이관 API |
| **PROD-004** | 상품 서비스 | ProductMigrationService.java | 상품 이관 로직 |
| **PROD-005** | 상품 Reader | ProductItemReader.java | 상품 데이터 읽기 |
| **PROD-006** | 상품 Processor | ProductDataProcessor.java | 상품 데이터 변환 |

#### 2.2.3 ORDER 도메인 (6개)
| 프로그램ID | 프로그램명 | 파일명 | 기능 |
|-----------|-----------|--------|------|
| **ORDR-001** | 주문 Job 설정 | OrderMigrationJobConfig.java | 주문 도메인 Job |
| **ORDR-002** | 주문 Step 설정 | OrderMigrationStepConfig.java | 주문 테이블 Step들 |
| **ORDR-003** | 주문 컨트롤러 | OrderMigrationController.java | 주문 이관 API |
| **ORDR-004** | 주문 서비스 | OrderMigrationService.java | 주문 이관 로직 |
| **ORDR-005** | 주문 Reader | OrderItemReader.java | 주문 데이터 읽기 |
| **ORDR-006** | 주문 Processor | OrderDataProcessor.java | 주문 데이터 변환 |

#### 2.2.4 기타 도메인 (7개 × 6개 = 42개)
- **PAYMENT 도메인**: PAYM-001 ~ PAYM-006
- **INVENTORY 도메인**: INVT-001 ~ INVT-006
- **DELIVERY 도메인**: DELV-001 ~ DELV-006
- **CUSTOMER 도메인**: CUST-001 ~ CUST-006
- **MARKETING 도메인**: MKTG-001 ~ MKTG-006
- **ANALYTICS 도메인**: ANLY-001 ~ ANLY-006
- **SYSTEM 도메인**: SYST-001 ~ SYST-006

### 2.3 공통 처리 컴포넌트 (10개)

| 프로그램ID | 프로그램명 | 파일명 | 기능 |
|-----------|-----------|--------|------|
| **COMM-001** | 공통 Writer | DatabaseItemWriter.java | 모든 도메인 공통 Writer |
| **COMM-002** | 배치 팩토리 | BatchJobFactory.java | 동적 Job 생성 |
| **COMM-003** | Step 팩토리 | BatchStepFactory.java | 동적 Step 생성 |
| **COMM-004** | 검증 프로세서 | ValidationProcessor.java | 데이터 검증 |
| **COMM-005** | 오류 핸들러 | ErrorHandler.java | 오류 처리 |
| **COMM-006** | 메트릭 수집기 | MetricsCollector.java | 성능 메트릭 |
| **COMM-007** | 알림 서비스 | NotificationService.java | 배치 완료 알림 |
| **COMM-008** | 스케줄러 | BatchScheduler.java | 배치 스케줄링 |
| **COMM-009** | 설정 관리자 | ConfigurationManager.java | 동적 설정 관리 |
| **COMM-010** | 로그 관리자 | LogManager.java | 구조화된 로깅 |

## 📊 3. 프로그램 통계 요약

### 3.1 전체 프로그램 수
| 구분 | 개수 | 비고 |
|------|------|------|
| **공통 기반 시스템** | 15개 | 전체 시스템 기반 |
| **도메인별 프로그램** | 60개 | 10개 도메인 × 6개 |
| **공통 처리 컴포넌트** | 10개 | 재사용 가능한 컴포넌트 |
| **총계** | **85개** | 전체 프로그램 |

### 3.2 도메인별 복잡도 및 공수
| 도메인 | 테이블 수 | 복잡도 | 예상 공수 (M/D) |
|--------|-----------|---------|-----------------|
| **USER** | 80→25 | 중간 | 28 |
| **PRODUCT** | 100→30 | 높음 | 42 |
| **ORDER** | 120→35 | 높음 | 50 |
| **PAYMENT** | 60→20 | 높음 | 35 |
| **INVENTORY** | 50→15 | 중간 | 25 |
| **DELIVERY** | 40→12 | 중간 | 21 |
| **CUSTOMER** | 45→18 | 중간 | 25 |
| **MARKETING** | 35→15 | 낮음 | 17 |
| **ANALYTICS** | 50→20 | 높음 | 31 |
| **SYSTEM** | 20→10 | 낮음 | 11 |
| **총계** | **600→200** | - | **285** |

## 🔧 4. 주요 API 엔드포인트

### 4.1 전체 시스템 API
| 엔드포인트 | 메서드 | 기능 |
|-----------|--------|------|
| `/api/migration/start` | POST | 전체 이관 시작 |
| `/api/migration/status` | GET | 전체 진행 상황 |
| `/api/migration/stop` | POST | 전체 이관 중지 |

### 4.2 도메인별 API (각 도메인당 5개씩)
| 도메인 | 주요 엔드포인트 | 기능 |
|--------|----------------|------|
| **USER** | `/api/user/migration/start` | 사용자 도메인 이관 |
| **PRODUCT** | `/api/product/migration/start` | 상품 도메인 이관 |
| **ORDER** | `/api/order/migration/start` | 주문 도메인 이관 |
| **기타** | `/api/{domain}/migration/start` | 각 도메인별 이관 |

## 📈 5. 개발 우선순위

### 5.1 Phase 1: 기반 시스템 (1-4주)
1. **공통 기반 시스템** (15개)
2. **공통 처리 컴포넌트** (10개)

### 5.2 Phase 2: 핵심 도메인 (5-12주)
1. **USER 도메인** (기준 도메인)
2. **SYSTEM 도메인** (단순 도메인)
3. **MARKETING 도메인** (단순 도메인)

### 5.3 Phase 3: 복잡 도메인 (13-20주)
1. **PRODUCT 도메인** (복잡)
2. **ORDER 도메인** (복잡)
3. **PAYMENT 도메인** (복잡)
4. **ANALYTICS 도메인** (복잡)

### 5.4 Phase 4: 나머지 도메인 (21-24주)
1. **INVENTORY 도메인** (중간)
2. **DELIVERY 도메인** (중간)
3. **CUSTOMER 도메인** (중간)

## 🎯 6. 성공 기준

### 6.1 기능적 성공 기준
- ✅ 10개 도메인 모두 독립적 실행 가능
- ✅ 600개 → 200개 테이블 완전 이관
- ✅ 도메인 간 의존성 순서 제어
- ✅ 실시간 도메인별 진행률 모니터링

### 6.2 기술적 성공 기준
- ✅ 85개 프로그램 모두 정상 동작
- ✅ 도메인당 평균 처리 속도: 시간당 10만 건
- ✅ 전체 시스템 가용성: 99.9%
- ✅ 코드 재사용률: 70% 이상

이 간소화된 프로그램 목록은 600개 테이블을 200개로 통합하는 대규모 프로젝트를 10개 도메인으로 체계적으로 관리할 수 있도록 설계되었습니다.
