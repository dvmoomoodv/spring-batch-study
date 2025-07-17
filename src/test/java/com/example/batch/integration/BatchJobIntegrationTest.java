package com.example.batch.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Collection;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@SpringBatchTest
@Testcontainers
@DisplayName("배치 Job 통합 테스트")
class BatchJobIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("targetdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/azure-sql-edge:latest")
            .withPassword("Test123!")
            .withEnv("ACCEPT_EULA", "Y");

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private Job dataMigrationJob;

    private JdbcTemplate sourceJdbcTemplate;
    private JdbcTemplate targetJdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.source.url", mssql::getJdbcUrl);
        registry.add("spring.datasource.source.username", mssql::getUsername);
        registry.add("spring.datasource.source.password", mssql::getPassword);
        registry.add("spring.datasource.source.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");

        registry.add("spring.datasource.target.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.target.username", mysql::getUsername);
        registry.add("spring.datasource.target.password", mysql::getPassword);
        registry.add("spring.datasource.target.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeEach
    void setUp(@Autowired DataSource sourceDataSource, @Autowired DataSource targetDataSource) {
        sourceJdbcTemplate = new JdbcTemplate(sourceDataSource);
        targetJdbcTemplate = new JdbcTemplate(targetDataSource);
        
        // Job 실행 이력 정리
        jobRepositoryTestUtils.removeJobExecutions();
        
        // 테스트 데이터 준비
        setupTestData();
    }

    @Test
    @DisplayName("전체 데이터 이관 Job 실행 테스트")
    void dataMigrationJobTest() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("startTime", System.currentTimeMillis())
                .addString("triggeredBy", "TEST")
                .addLong("chunkSize", 100L)
                .addLong("skipLimit", 10L)
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // Step 실행 검증
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        assertThat(stepExecutions).isNotEmpty();

        // 각 Step 결과 검증
        for (StepExecution stepExecution : stepExecutions) {
            assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(stepExecution.getReadCount()).isGreaterThanOrEqualTo(0);
            assertThat(stepExecution.getWriteCount()).isEqualTo(stepExecution.getReadCount());
            assertThat(stepExecution.getSkipCount()).isLessThanOrEqualTo(10); // skipLimit 이내
        }

        // 데이터 이관 결과 검증
        verifyMigrationResults();
    }

    @Test
    @DisplayName("사용자 이관 Step 단독 실행 테스트")
    void userMigrationStepTest() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("userMigrationStep", jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("userMigrationStep");
        assertThat(stepExecution.getReadCount()).isGreaterThan(0);
        assertThat(stepExecution.getWriteCount()).isEqualTo(stepExecution.getReadCount());
        assertThat(stepExecution.getSkipCount()).isEqualTo(0);

        // 사용자 데이터 이관 검증
        int sourceCount = sourceJdbcTemplate.queryForObject("SELECT COUNT(*) FROM 사용자", Integer.class);
        int targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(targetCount).isEqualTo(sourceCount);
    }

    @Test
    @DisplayName("상품 이관 Step 단독 실행 테스트")
    void productMigrationStepTest() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("productMigrationStep", jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("productMigrationStep");

        // 상품 데이터 이관 검증
        int sourceCount = sourceJdbcTemplate.queryForObject("SELECT COUNT(*) FROM 상품", Integer.class);
        int targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        assertThat(targetCount).isEqualTo(sourceCount);

        // 상품 데이터 변환 검증
        String salesStatus = targetJdbcTemplate.queryForObject(
                "SELECT sales_status FROM products WHERE product_id = 1", String.class);
        assertThat(salesStatus).isIn("ON_SALE", "OUT_OF_STOCK");
    }

    @Test
    @DisplayName("주문 이관 Step 단독 실행 테스트")
    void orderMigrationStepTest() throws Exception {
        // Given: 사용자 데이터 먼저 이관 (외래키 의존성)
        jobLauncherTestUtils.launchStep("userMigrationStep");

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("orderMigrationStep", jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("orderMigrationStep");

        // 주문 데이터 이관 검증
        int sourceCount = sourceJdbcTemplate.queryForObject("SELECT COUNT(*) FROM 주문", Integer.class);
        int targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        assertThat(targetCount).isEqualTo(sourceCount);

        // 주문 상태 변환 검증
        String orderStatus = targetJdbcTemplate.queryForObject(
                "SELECT order_status FROM orders WHERE order_id = 1", String.class);
        assertThat(orderStatus).isIn("DELIVERED", "SHIPPING", "ORDER_RECEIVED", "PREPARING");
    }

    @Test
    @DisplayName("Step 실행 순서 검증 테스트")
    void stepExecutionOrderTest() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Step 실행 순서 검증
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        String[] expectedStepOrder = {
                "userMigrationStep",
                "categoryMigrationStep", 
                "productMigrationStep",
                "orderMigrationStep",
                "orderDetailMigrationStep"
        };

        int index = 0;
        for (StepExecution stepExecution : stepExecutions) {
            if (index < expectedStepOrder.length) {
                assertThat(stepExecution.getStepName()).isEqualTo(expectedStepOrder[index]);
                index++;
            }
        }
    }

    @Test
    @DisplayName("배치 실행 중 오류 처리 테스트")
    void errorHandlingTest() throws Exception {
        // Given: 잘못된 데이터 삽입
        sourceJdbcTemplate.execute("""
            INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) 
            VALUES (9999, NULL, 'error@test.com', '남성', '개발자', 1)
        """);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addLong("skipLimit", 5L) // 오류 허용 개수
                .toJobParameters();

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("userMigrationStep", jobParameters);

        // Then: 일부 오류가 있어도 Step 완료
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getSkipCount()).isGreaterThan(0); // 스킵된 레코드 존재
        assertThat(stepExecution.getSkipCount()).isLessThanOrEqualTo(5); // skipLimit 이내
    }

    @Test
    @DisplayName("청크 크기별 성능 테스트")
    void chunkSizePerformanceTest() throws Exception {
        // Given: 대용량 테스트 데이터 생성
        generateLargeTestData(1000);

        // 청크 크기별 성능 비교
        long[] chunkSizes = {10, 100, 500};
        long[] executionTimes = new long[chunkSizes.length];

        for (int i = 0; i < chunkSizes.length; i++) {
            // 타겟 테이블 초기화
            targetJdbcTemplate.execute("DELETE FROM users");
            jobRepositoryTestUtils.removeJobExecutions();

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis() + i) // 유니크한 파라미터
                    .addLong("chunkSize", chunkSizes[i])
                    .toJobParameters();

            // When
            long startTime = System.currentTimeMillis();
            JobExecution jobExecution = jobLauncherTestUtils.launchStep("userMigrationStep", jobParameters);
            long endTime = System.currentTimeMillis();

            executionTimes[i] = endTime - startTime;

            // Then
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            
            int targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            assertThat(targetCount).isEqualTo(1000);
        }

        // 성능 결과 출력 (로그)
        for (int i = 0; i < chunkSizes.length; i++) {
            System.out.printf("Chunk Size: %d, Execution Time: %d ms%n", 
                    chunkSizes[i], executionTimes[i]);
        }

        // 일반적으로 적절한 청크 크기가 가장 빠름
        assertThat(executionTimes[1]).isLessThan(executionTimes[0]); // 100 < 10
    }

    @Test
    @DisplayName("Job 재시작 테스트")
    void jobRestartTest() throws Exception {
        // Given: 실패하도록 설정된 Job 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addLong("skipLimit", 0L) // 오류 시 즉시 실패
                .toJobParameters();

        // 잘못된 데이터로 실패 유도
        sourceJdbcTemplate.execute("""
            INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) 
            VALUES (9998, NULL, 'fail@test.com', '남성', '개발자', 1)
        """);

        // When: 첫 번째 실행 (실패 예상)
        JobExecution firstExecution = jobLauncherTestUtils.launchStep("userMigrationStep", jobParameters);

        // Then: 실패 확인
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

        // Given: 문제 데이터 수정
        sourceJdbcTemplate.execute("DELETE FROM 사용자 WHERE 사용자ID = 9998");

        // When: 재시작
        JobExecution restartExecution = jobLauncherTestUtils.launchStep("userMigrationStep", jobParameters);

        // Then: 성공 확인
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    private void setupTestData() {
        // 기존 데이터 정리
        sourceJdbcTemplate.execute("DELETE FROM 주문");
        sourceJdbcTemplate.execute("DELETE FROM 상품");
        sourceJdbcTemplate.execute("DELETE FROM 카테고리");
        sourceJdbcTemplate.execute("DELETE FROM 사용자");

        targetJdbcTemplate.execute("DELETE FROM orders");
        targetJdbcTemplate.execute("DELETE FROM products");
        targetJdbcTemplate.execute("DELETE FROM categories");
        targetJdbcTemplate.execute("DELETE FROM users");

        // 테스트 데이터 삽입
        sourceJdbcTemplate.execute("""
            INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) VALUES
            (1, '김철수', 'kim@test.com', '남성', '개발자', 1),
            (2, '이영희', 'lee@test.com', '여성', '디자이너', 1),
            (3, '박민수', 'park@test.com', '남성', '마케터', 1)
        """);

        sourceJdbcTemplate.execute("""
            INSERT INTO 카테고리 (카테고리ID, 카테고리명, 상위카테고리ID) VALUES
            (1, '전자제품', NULL),
            (2, '스마트폰', 1),
            (3, '노트북', 1)
        """);

        sourceJdbcTemplate.execute("""
            INSERT INTO 상품 (상품ID, 상품명, 카테고리ID, 가격, 판매상태) VALUES
            (1, '갤럭시폰', 2, 800000, '판매중'),
            (2, '아이폰', 2, 1200000, '판매중'),
            (3, '맥북', 3, 2000000, '품절')
        """);

        sourceJdbcTemplate.execute("""
            INSERT INTO 주문 (주문ID, 사용자ID, 주문번호, 주문상태, 결제방법, 총금액) VALUES
            (1, 1, 'ORD-001', '배송완료', '신용카드', 800000),
            (2, 2, 'ORD-002', '배송중', '계좌이체', 1200000),
            (3, 3, 'ORD-003', '주문접수', '신용카드', 2000000)
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

    private void verifyMigrationResults() {
        // 레코드 수 검증
        int sourceUserCount = sourceJdbcTemplate.queryForObject("SELECT COUNT(*) FROM 사용자", Integer.class);
        int targetUserCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(targetUserCount).isEqualTo(sourceUserCount);

        int sourceProductCount = sourceJdbcTemplate.queryForObject("SELECT COUNT(*) FROM 상품", Integer.class);
        int targetProductCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        assertThat(targetProductCount).isEqualTo(sourceProductCount);

        // migrated_at 컬럼 확인
        int migratedUserCount = targetJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE migrated_at IS NOT NULL", Integer.class);
        assertThat(migratedUserCount).isEqualTo(targetUserCount);
    }
}
