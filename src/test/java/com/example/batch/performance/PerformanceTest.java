package com.example.batch.performance;

import com.example.batch.model.BatchConfiguration;
import com.example.batch.model.MigrationResult;
import com.example.batch.service.MigrationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("performance")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("성능 테스트")
class PerformanceTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("targetdb")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--max_connections=200", "--innodb_buffer_pool_size=1G");

    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/azure-sql-edge:latest")
            .withPassword("Test123!")
            .withEnv("ACCEPT_EULA", "Y");

    @Autowired
    private MigrationService migrationService;

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
    }

    @Test
    @Order(1)
    @DisplayName("10만 건 데이터 이관 성능 테스트")
    void performanceTest_100K_Records() throws Exception {
        // Given: 10만 건 테스트 데이터 생성
        int recordCount = 100_000;
        generateLargeTestData(recordCount);

        BatchConfiguration config = BatchConfiguration.builder()
                .chunkSize(2000)
                .skipLimit(100)
                .retryLimit(3)
                .build();

        // When: 이관 실행 및 시간 측정
        long startTime = System.currentTimeMillis();
        MigrationResult result = migrationService.startTableMigration("사용자", config);
        
        // 완료 대기
        waitForJobCompletion(result.getJobExecutionId(), 10, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        // Then: 성능 기준 검증
        long duration = endTime - startTime;
        long recordsPerSecond = recordCount * 1000 / duration;

        System.out.printf("=== 10만 건 성능 테스트 결과 ===%n");
        System.out.printf("처리 시간: %d ms (%.2f초)%n", duration, duration / 1000.0);
        System.out.printf("처리 속도: %d records/sec%n", recordsPerSecond);
        System.out.printf("청크 크기: %d%n", config.getChunkSize());

        // 성능 기준: 초당 2,000건 이상, 50초 이내
        assertThat(recordsPerSecond).isGreaterThan(2000);
        assertThat(duration).isLessThan(50_000);

        // 데이터 정합성 검증
        int targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(targetCount).isEqualTo(recordCount);
    }

    @Test
    @Order(2)
    @DisplayName("100만 건 데이터 이관 성능 테스트")
    void performanceTest_1Million_Records() throws Exception {
        // Given: 100만 건 테스트 데이터 생성
        int recordCount = 1_000_000;
        generateLargeTestData(recordCount);

        BatchConfiguration config = BatchConfiguration.builder()
                .chunkSize(5000)
                .skipLimit(1000)
                .retryLimit(3)
                .build();

        // When: 이관 실행 및 시간 측정
        long startTime = System.currentTimeMillis();
        MigrationResult result = migrationService.startTableMigration("사용자", config);
        
        // 완료 대기 (최대 15분)
        waitForJobCompletion(result.getJobExecutionId(), 15, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        // Then: 성능 기준 검증
        long duration = endTime - startTime;
        long recordsPerSecond = recordCount * 1000 / duration;

        System.out.printf("=== 100만 건 성능 테스트 결과 ===%n");
        System.out.printf("처리 시간: %d ms (%.2f분)%n", duration, duration / 60000.0);
        System.out.printf("처리 속도: %d records/sec%n", recordsPerSecond);
        System.out.printf("청크 크기: %d%n", config.getChunkSize());

        // 성능 기준: 초당 1,000건 이상, 15분 이내
        assertThat(recordsPerSecond).isGreaterThan(1000);
        assertThat(duration).isLessThan(900_000); // 15분

        // 데이터 정합성 검증
        int targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(targetCount).isEqualTo(recordCount);
    }

    @Test
    @Order(3)
    @DisplayName("청크 크기별 성능 비교 테스트")
    void chunkSizePerformanceComparison() throws Exception {
        // Given: 5만 건 테스트 데이터
        int recordCount = 50_000;
        int[] chunkSizes = {500, 1000, 2000, 5000, 10000};
        
        System.out.printf("=== 청크 크기별 성능 비교 (레코드 수: %d) ===%n", recordCount);
        System.out.printf("%-10s %-15s %-15s %-15s%n", "청크크기", "처리시간(ms)", "처리속도(r/s)", "메모리사용량");
        System.out.println("--------------------------------------------------------");

        for (int chunkSize : chunkSizes) {
            // 데이터 초기화
            generateLargeTestData(recordCount);
            targetJdbcTemplate.execute("DELETE FROM users");

            BatchConfiguration config = BatchConfiguration.builder()
                    .chunkSize(chunkSize)
                    .skipLimit(100)
                    .build();

            // 메모리 사용량 측정 시작
            Runtime runtime = Runtime.getRuntime();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            // 성능 측정
            long startTime = System.currentTimeMillis();
            MigrationResult result = migrationService.startTableMigration("사용자", config);
            waitForJobCompletion(result.getJobExecutionId(), 5, TimeUnit.MINUTES);
            long endTime = System.currentTimeMillis();

            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;

            long duration = endTime - startTime;
            long recordsPerSecond = recordCount * 1000 / duration;

            System.out.printf("%-10d %-15d %-15d %-15s%n", 
                    chunkSize, duration, recordsPerSecond, formatMemory(memoryUsed));

            // 데이터 정합성 검증
            int targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            assertThat(targetCount).isEqualTo(recordCount);
        }
    }

    @Test
    @Order(4)
    @DisplayName("동시 실행 성능 테스트")
    void concurrentMigrationPerformanceTest() throws InterruptedException, ExecutionException {
        // Given: 각 도메인별 테스트 데이터 준비
        String[] domains = {"USER", "PRODUCT", "ORDER", "PAYMENT", "INVENTORY"};
        int recordsPerDomain = 20_000;

        // 각 도메인별 데이터 생성
        for (String domain : domains) {
            generateDomainTestData(domain, recordsPerDomain);
        }

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(domains.length);
        List<Future<PerformanceResult>> futures = new ArrayList<>();

        // When: 5개 도메인 동시 실행
        long overallStartTime = System.currentTimeMillis();

        for (String domain : domains) {
            Future<PerformanceResult> future = executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    BatchConfiguration config = BatchConfiguration.builder()
                            .chunkSize(1000)
                            .skipLimit(100)
                            .build();

                    MigrationResult result = migrationService.startDomainMigration(domain, config);
                    waitForJobCompletion(result.getJobExecutionId(), 10, TimeUnit.MINUTES);
                    
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    
                    return new PerformanceResult(domain, recordsPerDomain, duration);
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // 모든 작업 완료 대기 (최대 30분)
        boolean completed = latch.await(30, TimeUnit.MINUTES);
        assertThat(completed).isTrue();

        long overallEndTime = System.currentTimeMillis();
        long overallDuration = overallEndTime - overallStartTime;

        // Then: 결과 분석
        System.out.printf("=== 동시 실행 성능 테스트 결과 ===%n");
        System.out.printf("전체 처리 시간: %d ms (%.2f분)%n", overallDuration, overallDuration / 60000.0);
        System.out.printf("총 처리 레코드: %d건%n", domains.length * recordsPerDomain);
        System.out.printf("전체 처리 속도: %d records/sec%n", 
                (domains.length * recordsPerDomain * 1000) / overallDuration);

        System.out.println("\n도메인별 상세 결과:");
        System.out.printf("%-10s %-15s %-15s %-15s%n", "도메인", "레코드수", "처리시간(ms)", "처리속도(r/s)");
        System.out.println("--------------------------------------------------------");

        for (Future<PerformanceResult> future : futures) {
            PerformanceResult result = future.get();
            long recordsPerSecond = result.recordCount * 1000 / result.duration;
            System.out.printf("%-10s %-15d %-15d %-15d%n", 
                    result.domain, result.recordCount, result.duration, recordsPerSecond);
        }

        // 성능 기준: 전체 30분 이내, 각 도메인 평균 초당 500건 이상
        assertThat(overallDuration).isLessThan(1_800_000); // 30분
        
        for (Future<PerformanceResult> future : futures) {
            PerformanceResult result = future.get();
            long recordsPerSecond = result.recordCount * 1000 / result.duration;
            assertThat(recordsPerSecond).isGreaterThan(500);
        }

        executor.shutdown();
    }

    @Test
    @Order(5)
    @DisplayName("메모리 사용량 모니터링 테스트")
    void memoryUsageMonitoringTest() throws Exception {
        // Given: 대용량 데이터
        int recordCount = 200_000;
        generateLargeTestData(recordCount);

        BatchConfiguration config = BatchConfiguration.builder()
                .chunkSize(1000)
                .skipLimit(100)
                .build();

        // 메모리 모니터링 스레드 시작
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        Thread monitorThread = new Thread(memoryMonitor);
        monitorThread.start();

        // When: 이관 실행
        long startTime = System.currentTimeMillis();
        MigrationResult result = migrationService.startTableMigration("사용자", config);
        waitForJobCompletion(result.getJobExecutionId(), 10, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        // 모니터링 중지
        memoryMonitor.stop();
        monitorThread.join();

        // Then: 메모리 사용량 분석
        System.out.printf("=== 메모리 사용량 분석 ===%n");
        System.out.printf("최대 메모리 사용량: %s%n", formatMemory(memoryMonitor.getMaxMemoryUsed()));
        System.out.printf("평균 메모리 사용량: %s%n", formatMemory(memoryMonitor.getAverageMemoryUsed()));
        System.out.printf("처리 시간: %d ms%n", endTime - startTime);

        // 메모리 사용량이 8GB를 초과하지 않아야 함
        assertThat(memoryMonitor.getMaxMemoryUsed()).isLessThan(8L * 1024 * 1024 * 1024); // 8GB
    }

    private void generateLargeTestData(int count) {
        sourceJdbcTemplate.execute("DELETE FROM 사용자");
        
        // 배치로 데이터 삽입 (성능 향상)
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            batchArgs.add(new Object[]{
                    i, 
                    "사용자" + i, 
                    "user" + i + "@test.com", 
                    (i % 2 == 0 ? "남성" : "여성"), 
                    "직업" + (i % 10), 
                    1
            });
            
            // 1000건씩 배치 처리
            if (i % 1000 == 0) {
                sourceJdbcTemplate.batchUpdate(
                        "INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) VALUES (?, ?, ?, ?, ?, ?)",
                        batchArgs
                );
                batchArgs.clear();
            }
        }
        
        // 나머지 데이터 처리
        if (!batchArgs.isEmpty()) {
            sourceJdbcTemplate.batchUpdate(
                    "INSERT INTO 사용자 (사용자ID, 이름, 이메일, 성별, 직업, 활성여부) VALUES (?, ?, ?, ?, ?, ?)",
                    batchArgs
            );
        }
    }

    private void generateDomainTestData(String domain, int count) {
        // 도메인별 테스트 데이터 생성 로직
        // 실제 구현에서는 각 도메인에 맞는 테이블에 데이터 생성
        switch (domain) {
            case "USER":
                generateLargeTestData(count);
                break;
            case "PRODUCT":
                // 상품 테이블 데이터 생성
                break;
            // 기타 도메인...
        }
    }

    private void waitForJobCompletion(Long jobExecutionId, long timeout, TimeUnit unit) throws InterruptedException {
        long timeoutMillis = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            MigrationResult status = migrationService.getJobStatus(jobExecutionId);
            if ("COMPLETED".equals(status.getStatus()) || "FAILED".equals(status.getStatus())) {
                if ("FAILED".equals(status.getStatus())) {
                    throw new RuntimeException("Job failed: " + status.getMessage());
                }
                return;
            }
            Thread.sleep(1000); // 1초 대기
        }
        
        throw new RuntimeException("Job did not complete within timeout");
    }

    private String formatMemory(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static class PerformanceResult {
        final String domain;
        final int recordCount;
        final long duration;

        PerformanceResult(String domain, int recordCount, long duration) {
            this.domain = domain;
            this.recordCount = recordCount;
            this.duration = duration;
        }
    }

    private static class MemoryMonitor implements Runnable {
        private volatile boolean running = true;
        private long maxMemoryUsed = 0;
        private long totalMemoryUsed = 0;
        private int measurements = 0;

        @Override
        public void run() {
            Runtime runtime = Runtime.getRuntime();
            
            while (running) {
                long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
                maxMemoryUsed = Math.max(maxMemoryUsed, memoryUsed);
                totalMemoryUsed += memoryUsed;
                measurements++;
                
                try {
                    Thread.sleep(1000); // 1초마다 측정
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public void stop() {
            running = false;
        }

        public long getMaxMemoryUsed() {
            return maxMemoryUsed;
        }

        public long getAverageMemoryUsed() {
            return measurements > 0 ? totalMemoryUsed / measurements : 0;
        }
    }
}
