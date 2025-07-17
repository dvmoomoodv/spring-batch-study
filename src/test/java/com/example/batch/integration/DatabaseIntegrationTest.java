package com.example.batch.integration;

import com.example.batch.model.BatchConfiguration;
import com.example.batch.model.MigrationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("데이터베이스 통합 테스트")
class DatabaseIntegrationTest {

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

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private JdbcTemplate sourceJdbcTemplate;
    private JdbcTemplate targetJdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Source Database (MSSQL)
        registry.add("spring.datasource.source.url", mssql::getJdbcUrl);
        registry.add("spring.datasource.source.username", mssql::getUsername);
        registry.add("spring.datasource.source.password", mssql::getPassword);
        registry.add("spring.datasource.source.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");

        // Target Database (MySQL)
        registry.add("spring.datasource.target.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.target.username", mysql::getUsername);
        registry.add("spring.datasource.target.password", mysql::getPassword);
        registry.add("spring.datasource.target.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeEach
    void setUp(@Autowired DataSource sourceDataSource, @Autowired DataSource targetDataSource) {
        sourceJdbcTemplate = new JdbcTemplate(sourceDataSource);
        targetJdbcTemplate = new JdbcTemplate(targetDataSource);
        
        // 테스트 데이터 초기화
        setupTestData();
    }

    @Test
    @Order(1)
    @DisplayName("전체 데이터 이관 통합 테스트")
    void fullMigrationIntegrationTest() {
        // Given: 소스 데이터 확인
        int sourceUserCount = getSourceRecordCount("사용자");
        int sourceProductCount = getSourceRecordCount("상품");
        int sourceOrderCount = getSourceRecordCount("주문");
        
        assertThat(sourceUserCount).isGreaterThan(0);
        assertThat(sourceProductCount).isGreaterThan(0);
        assertThat(sourceOrderCount).isGreaterThan(0);

        // When: 전체 이관 실행
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/migration/start?chunkSize=100&skipLimit=10",
                null,
                String.class
        );

        // Then: 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 배치 완료 대기 (최대 5분)
        await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    int targetUserCount = getTargetRecordCount("users");
                    int targetProductCount = getTargetRecordCount("products");
                    int targetOrderCount = getTargetRecordCount("orders");
                    
                    assertThat(targetUserCount).isEqualTo(sourceUserCount);
                    assertThat(targetProductCount).isEqualTo(sourceProductCount);
                    assertThat(targetOrderCount).isEqualTo(sourceOrderCount);
                });

        // 데이터 변환 검증
        verifyDataTransformation();
    }

    @Test
    @Order(2)
    @DisplayName("특정 테이블 이관 통합 테스트")
    void singleTableMigrationTest() {
        // Given
        String tableName = "사용자";
        int sourceCount = getSourceRecordCount(tableName);
        
        // 타겟 테이블 초기화
        targetJdbcTemplate.execute("DELETE FROM users");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/migration/table/" + tableName + "?chunkSize=50",
                null,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 배치 완료 대기
        await().atMost(2, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    int targetCount = getTargetRecordCount("users");
                    assertThat(targetCount).isEqualTo(sourceCount);
                });

        // 사용자 데이터 변환 검증
        verifyUserDataTransformation();
    }

    @Test
    @Order(3)
    @DisplayName("도메인별 이관 통합 테스트")
    void domainMigrationTest() {
        // Given
        String domainName = "USER";
        
        // 관련 테이블 초기화
        targetJdbcTemplate.execute("DELETE FROM users");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/migration/domain/" + domainName,
                null,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 배치 완료 대기
        await().atMost(3, TimeUnit.MINUTES)
                .pollInterval(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    int userCount = getTargetRecordCount("users");
                    assertThat(userCount).isGreaterThan(0);
                });
    }

    @Test
    @Order(4)
    @DisplayName("대용량 데이터 처리 성능 테스트")
    void largeDataPerformanceTest() {
        // Given: 대용량 테스트 데이터 생성 (10,000건)
        generateLargeTestData(10000);
        
        long startTime = System.currentTimeMillis();

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/migration/table/사용자?chunkSize=1000",
                null,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        await().atMost(10, TimeUnit.MINUTES)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    int targetCount = getTargetRecordCount("users");
                    assertThat(targetCount).isEqualTo(10000);
                });

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 성능 검증: 10,000건을 10분 이내 처리 (분당 1,000건 이상)
        assertThat(duration).isLessThan(600_000); // 10분
        
        double recordsPerSecond = 10000.0 / (duration / 1000.0);
        assertThat(recordsPerSecond).isGreaterThan(16.67); // 분당 1,000건 = 초당 16.67건
    }

    @Test
    @Order(5)
    @DisplayName("오류 처리 및 복구 테스트")
    void errorHandlingAndRecoveryTest() {
        // Given: 잘못된 데이터 삽입
        insertInvalidTestData();

        // When: 이관 실행 (일부 오류 발생 예상)
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/migration/table/사용자?chunkSize=10&skipLimit=5",
                null,
                String.class
        );

        // Then: 부분 성공 확인
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        await().atMost(2, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    int targetCount = getTargetRecordCount("users");
                    int sourceCount = getSourceRecordCount("사용자");
                    
                    // 일부 레코드는 스킵되었지만 대부분 성공
                    assertThat(targetCount).isGreaterThan(sourceCount * 0.8); // 80% 이상 성공
                });
    }

    @Test
    @Order(6)
    @DisplayName("동시 실행 제한 테스트")
    void concurrentExecutionLimitTest() {
        // Given: 첫 번째 배치 실행
        ResponseEntity<String> firstResponse = restTemplate.postForEntity(
                "/api/migration/start",
                null,
                String.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // When: 동일한 배치 재실행 시도
        ResponseEntity<String> secondResponse = restTemplate.postForEntity(
                "/api/migration/start",
                null,
                String.class
        );

        // Then: 충돌 오류 발생
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private void setupTestData() {
        // 사용자 테스트 데이터
        sourceJdbcTemplate.execute("""
            IF NOT EXISTS (SELECT * FROM 사용자 WHERE 사용자ID = 1)
            INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) 
            VALUES (1, '김철수', 'kim@test.com', '남성', '개발자', 1)
        """);

        // 상품 테스트 데이터
        sourceJdbcTemplate.execute("""
            IF NOT EXISTS (SELECT * FROM 상품 WHERE 상품ID = 1)
            INSERT INTO 상품 (상품ID, 상품명, 카테고리, 가격, 판매상태) 
            VALUES (1, '테스트상품', '전자제품', 100000, '판매중')
        """);

        // 주문 테스트 데이터
        sourceJdbcTemplate.execute("""
            IF NOT EXISTS (SELECT * FROM 주문 WHERE 주문ID = 1)
            INSERT INTO 주문 (주문ID, 주문번호, 주문상태, 결제방법, 총금액) 
            VALUES (1, 'ORD-TEST-001', '배송완료', '신용카드', 100000)
        """);
    }

    private void generateLargeTestData(int count) {
        sourceJdbcTemplate.execute("DELETE FROM 사용자");
        
        for (int i = 1; i <= count; i++) {
            sourceJdbcTemplate.execute(String.format("""
                INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) 
                VALUES (%d, '사용자%d', 'user%d@test.com', '%s', '직업%d', 1)
            """, i, i, i, (i % 2 == 0 ? "남성" : "여성"), i % 10));
        }
    }

    private void insertInvalidTestData() {
        // 일부 잘못된 데이터 삽입 (예: NULL 제약 위반)
        try {
            sourceJdbcTemplate.execute("""
                INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) 
                VALUES (9999, NULL, 'invalid@test.com', '남성', '개발자', 1)
            """);
        } catch (Exception e) {
            // 의도적으로 무시
        }
    }

    private int getSourceRecordCount(String tableName) {
        return sourceJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private int getTargetRecordCount(String tableName) {
        return targetJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private void verifyDataTransformation() {
        // 사용자 데이터 변환 검증
        verifyUserDataTransformation();
        
        // 상품 데이터 변환 검증
        verifyProductDataTransformation();
        
        // 주문 데이터 변환 검증
        verifyOrderDataTransformation();
    }

    private void verifyUserDataTransformation() {
        List<Map<String, Object>> users = targetJdbcTemplate.queryForList(
                "SELECT user_id, name, email, gender, occupation, is_active, migrated_at FROM users LIMIT 5");
        
        assertThat(users).isNotEmpty();
        
        for (Map<String, Object> user : users) {
            assertThat(user.get("user_id")).isNotNull();
            assertThat(user.get("name")).isNotNull();
            assertThat(user.get("gender")).isIn("MALE", "FEMALE"); // 한글 → 영어 변환 확인
            assertThat(user.get("migrated_at")).isNotNull(); // 이관 시점 기록 확인
        }
    }

    private void verifyProductDataTransformation() {
        List<Map<String, Object>> products = targetJdbcTemplate.queryForList(
                "SELECT product_id, product_name, category, price, sales_status, migrated_at FROM products LIMIT 5");
        
        for (Map<String, Object> product : products) {
            assertThat(product.get("sales_status")).isIn("ON_SALE", "OUT_OF_STOCK"); // 판매상태 변환 확인
            assertThat(product.get("migrated_at")).isNotNull();
        }
    }

    private void verifyOrderDataTransformation() {
        List<Map<String, Object>> orders = targetJdbcTemplate.queryForList(
                "SELECT order_id, order_number, order_status, payment_method, total_amount, migrated_at FROM orders LIMIT 5");
        
        for (Map<String, Object> order : orders) {
            assertThat(order.get("order_status")).isIn("DELIVERED", "SHIPPING", "ORDER_RECEIVED"); // 주문상태 변환 확인
            assertThat(order.get("payment_method")).isIn("CREDIT_CARD", "BANK_TRANSFER"); // 결제방법 변환 확인
            assertThat(order.get("migrated_at")).isNotNull();
        }
    }
}
