package com.example.batch.job;

import com.example.batch.config.BatchProperties;
import com.example.batch.model.DataRecord;
import com.example.batch.processor.DataTransformProcessor;
import com.example.batch.reader.DatabaseItemReader;
import com.example.batch.writer.DatabaseItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 데이터 이관 Job 설정
 * MSSQL에서 MariaDB로의 테이블별 데이터 이관 Job 정의
 */
@Configuration
public class DataMigrationJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataMigrationJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource sourceDataSource;
    private final DataSource targetDataSource;
    private final BatchProperties batchProperties;
    private final DatabaseItemReader databaseItemReader;
    private final DataTransformProcessor dataTransformProcessor;
    private final DatabaseItemWriter databaseItemWriter;

    public DataMigrationJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("sourceDataSource") DataSource sourceDataSource,
            @Qualifier("targetDataSource") DataSource targetDataSource,
            BatchProperties batchProperties,
            DatabaseItemReader databaseItemReader,
            DataTransformProcessor dataTransformProcessor,
            DatabaseItemWriter databaseItemWriter) {
        
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.sourceDataSource = sourceDataSource;
        this.targetDataSource = targetDataSource;
        this.batchProperties = batchProperties;
        this.databaseItemReader = databaseItemReader;
        this.dataTransformProcessor = dataTransformProcessor;
        this.databaseItemWriter = databaseItemWriter;
    }

    /**
     * 메인 데이터 이관 Job
     */
    @Bean
    public Job dataMigrationJob() {
        logger.info("Creating data migration job with properties: {}", batchProperties);
        
        return new JobBuilder("dataMigrationJob", jobRepository)
                .start(migrationStep("users", null))  // 예시: users 테이블
                .next(migrationStep("orders", null))   // 예시: orders 테이블
                .next(migrationStep("products", null)) // 예시: products 테이블
                .build();
    }

    /**
     * 테이블별 이관 Step 생성
     * 
     * @param tableName 이관할 테이블명
     * @param whereClause WHERE 조건 (선택사항)
     * @return Step
     */
    @Bean
    public Step migrationStep(String tableName, String whereClause) {
        logger.info("Creating migration step for table: {} with chunk size: {}", 
            tableName, batchProperties.getChunkSize());

        return new StepBuilder(tableName + "MigrationStep", jobRepository)
                .<DataRecord, DataRecord>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(createTableReader(tableName, whereClause))
                .processor(dataTransformProcessor)
                .writer(databaseItemWriter)
                .faultTolerant()
                .skipLimit(batchProperties.getSkipLimit())
                .skip(Exception.class)
                .retryLimit(batchProperties.getRetryLimit())
                .retry(Exception.class)
                .listener(new MigrationStepListener(tableName))
                .build();
    }

    /**
     * 테이블별 ItemReader 생성
     */
    private JdbcCursorItemReader<DataRecord> createTableReader(String tableName, String whereClause) {
        return databaseItemReader.createReader(
            sourceDataSource, 
            tableName, 
            whereClause, 
            batchProperties.getChunkSize()
        );
    }

    /**
     * 특정 테이블만 이관하는 Job (동적 생성용)
     */
    public Job createSingleTableMigrationJob(String tableName, String whereClause) {
        logger.info("Creating single table migration job for: {}", tableName);
        
        return new JobBuilder(tableName + "MigrationJob", jobRepository)
                .start(migrationStep(tableName, whereClause))
                .build();
    }

    /**
     * 커스텀 쿼리를 사용하는 Step 생성
     */
    public Step createCustomQueryStep(String stepName, String customQuery) {
        logger.info("Creating custom query step: {} with query: {}", stepName, customQuery);

        JdbcCursorItemReader<DataRecord> customReader = new DatabaseItemReader()
                .createReader(sourceDataSource, "(" + customQuery + ") AS custom_query", null, batchProperties.getChunkSize());

        return new StepBuilder(stepName, jobRepository)
                .<DataRecord, DataRecord>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(customReader)
                .processor(dataTransformProcessor)
                .writer(databaseItemWriter)
                .faultTolerant()
                .skipLimit(batchProperties.getSkipLimit())
                .skip(Exception.class)
                .retryLimit(batchProperties.getRetryLimit())
                .retry(Exception.class)
                .listener(new MigrationStepListener(stepName))
                .build();
    }
}
