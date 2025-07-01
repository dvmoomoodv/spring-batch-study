package com.example.batch.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Step 실행 상태를 모니터링하는 리스너
 * 상세한 진행 상황과 성능 지표를 로깅
 */
public class MigrationStepListener implements StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MigrationStepListener.class);
    
    private final String tableName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public MigrationStepListener(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        startTime = LocalDateTime.now();
        logger.info("=== Starting migration for table: {} ===", tableName);
        logger.info("Step: {}", stepExecution.getStepName());
        logger.info("Job: {}", stepExecution.getJobExecution().getJobInstance().getJobName());
        logger.info("Start time: {}", startTime);
        
        // Job Parameters 로깅
        stepExecution.getJobExecution().getJobParameters().getParameters().forEach((key, value) -> 
            logger.info("Job Parameter - {}: {}", key, value.getValue()));
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);
        
        // 실행 결과 통계
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long skipCount = stepExecution.getSkipCount();
        long filterCount = stepExecution.getFilterCount();
        long commitCount = stepExecution.getCommitCount();
        long rollbackCount = stepExecution.getRollbackCount();
        
        logger.info("=== Migration completed for table: {} ===", tableName);
        logger.info("End time: {}", endTime);
        logger.info("Duration: {} seconds", duration.getSeconds());
        logger.info("Exit Status: {}", stepExecution.getExitStatus());
        
        // 상세 통계 로깅
        logger.info("--- Processing Statistics ---");
        logger.info("Records Read: {}", readCount);
        logger.info("Records Written: {}", writeCount);
        logger.info("Records Skipped: {}", skipCount);
        logger.info("Records Filtered: {}", filterCount);
        logger.info("Commits: {}", commitCount);
        logger.info("Rollbacks: {}", rollbackCount);
        
        // 성능 지표 계산
        if (duration.getSeconds() > 0) {
            long recordsPerSecond = readCount / duration.getSeconds();
            logger.info("Processing Rate: {} records/second", recordsPerSecond);
        }
        
        // 성공률 계산
        if (readCount > 0) {
            double successRate = ((double) writeCount / readCount) * 100;
            logger.info("Success Rate: {:.2f}%", successRate);
        }
        
        // 오류 정보 로깅
        if (stepExecution.getFailureExceptions() != null && !stepExecution.getFailureExceptions().isEmpty()) {
            logger.error("--- Failure Information ---");
            stepExecution.getFailureExceptions().forEach(exception -> 
                logger.error("Exception: {}", exception.getMessage(), exception));
        }
        
        // 경고 및 권장사항
        logRecommendations(stepExecution, duration);
        
        logger.info("=== End of migration summary for table: {} ===", tableName);
        
        return stepExecution.getExitStatus();
    }

    /**
     * 성능 분석 및 권장사항 로깅
     */
    private void logRecommendations(StepExecution stepExecution, Duration duration) {
        long readCount = stepExecution.getReadCount();
        long rollbackCount = stepExecution.getRollbackCount();
        long skipCount = stepExecution.getSkipCount();
        
        logger.info("--- Performance Analysis & Recommendations ---");
        
        // 처리 시간 분석
        if (duration.getSeconds() > 3600) { // 1시간 이상
            logger.warn("Migration took over 1 hour. Consider:");
            logger.warn("- Increasing chunk size for better performance");
            logger.warn("- Adding database indexes on frequently queried columns");
            logger.warn("- Running during off-peak hours");
        }
        
        // 롤백 분석
        if (rollbackCount > 0) {
            double rollbackRate = ((double) rollbackCount / stepExecution.getCommitCount()) * 100;
            if (rollbackRate > 5) {
                logger.warn("High rollback rate ({:.2f}%). Consider:", rollbackRate);
                logger.warn("- Reviewing data quality in source database");
                logger.warn("- Adjusting skip limit or retry logic");
                logger.warn("- Checking target database constraints");
            }
        }
        
        // 스킵 분석
        if (skipCount > 0) {
            double skipRate = ((double) skipCount / readCount) * 100;
            if (skipRate > 1) {
                logger.warn("High skip rate ({:.2f}%). Review skipped records for:", skipRate);
                logger.warn("- Data quality issues");
                logger.warn("- Constraint violations");
                logger.warn("- Transformation logic errors");
            }
        }
        
        // 성능 권장사항
        if (readCount > 0 && duration.getSeconds() > 0) {
            long recordsPerSecond = readCount / duration.getSeconds();
            if (recordsPerSecond < 100) {
                logger.warn("Low processing rate ({} records/sec). Consider:", recordsPerSecond);
                logger.warn("- Optimizing database queries");
                logger.warn("- Increasing connection pool size");
                logger.warn("- Reviewing processor logic complexity");
            }
        }
    }
}
