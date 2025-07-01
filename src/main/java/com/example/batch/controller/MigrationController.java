package com.example.batch.controller;

import com.example.batch.config.BatchProperties;
import com.example.batch.job.DataMigrationJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 데이터 이관 Job을 실행하고 모니터링하는 REST 컨트롤러
 */
@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);

    private final JobLauncher jobLauncher;
    private final Job dataMigrationJob;
    private final DataMigrationJobConfig jobConfig;
    private final BatchProperties batchProperties;

    public MigrationController(
            JobLauncher jobLauncher,
            Job dataMigrationJob,
            DataMigrationJobConfig jobConfig,
            BatchProperties batchProperties) {
        this.jobLauncher = jobLauncher;
        this.dataMigrationJob = dataMigrationJob;
        this.jobConfig = jobConfig;
        this.batchProperties = batchProperties;
    }

    /**
     * 전체 데이터 이관 Job 실행
     */
    @PostMapping("/start")
    public Map<String, Object> startMigration(
            @RequestParam(required = false) Integer chunkSize,
            @RequestParam(required = false) Integer skipLimit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Job Parameters 설정
            JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                    .addLocalDateTime("startTime", LocalDateTime.now())
                    .addString("triggeredBy", "REST_API");
            
            // 동적 파라미터 설정
            if (chunkSize != null) {
                parametersBuilder.addLong("chunkSize", chunkSize.longValue());
                logger.info("Using custom chunk size: {}", chunkSize);
            }
            
            if (skipLimit != null) {
                parametersBuilder.addLong("skipLimit", skipLimit.longValue());
                logger.info("Using custom skip limit: {}", skipLimit);
            }
            
            JobParameters jobParameters = parametersBuilder.toJobParameters();
            
            logger.info("Starting full data migration job with parameters: {}", jobParameters);
            JobExecution jobExecution = jobLauncher.run(dataMigrationJob, jobParameters);
            
            response.put("success", true);
            response.put("jobExecutionId", jobExecution.getId());
            response.put("jobInstanceId", jobExecution.getJobInstance().getId());
            response.put("status", jobExecution.getStatus().toString());
            response.put("startTime", jobExecution.getStartTime());
            response.put("message", "Migration job started successfully");
            
        } catch (JobExecutionAlreadyRunningException e) {
            logger.warn("Job is already running: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "Job is already running");
            response.put("message", e.getMessage());
            
        } catch (JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            logger.error("Failed to start migration job: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Job execution failed");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    /**
     * 특정 테이블만 이관
     */
    @PostMapping("/table/{tableName}")
    public Map<String, Object> migrateTable(
            @PathVariable String tableName,
            @RequestParam(required = false) String whereClause,
            @RequestParam(required = false) Integer chunkSize) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 단일 테이블 Job 생성
            Job singleTableJob = jobConfig.createSingleTableMigrationJob(tableName, whereClause);
            
            // Job Parameters 설정
            JobParametersBuilder parametersBuilder = new JobParametersBuilder()
                    .addLocalDateTime("startTime", LocalDateTime.now())
                    .addString("tableName", tableName)
                    .addString("triggeredBy", "REST_API_TABLE");
            
            if (whereClause != null) {
                parametersBuilder.addString("whereClause", whereClause);
            }
            
            if (chunkSize != null) {
                parametersBuilder.addLong("chunkSize", chunkSize.longValue());
            }
            
            JobParameters jobParameters = parametersBuilder.toJobParameters();
            
            logger.info("Starting table migration for: {} with parameters: {}", tableName, jobParameters);
            JobExecution jobExecution = jobLauncher.run(singleTableJob, jobParameters);
            
            response.put("success", true);
            response.put("tableName", tableName);
            response.put("jobExecutionId", jobExecution.getId());
            response.put("status", jobExecution.getStatus().toString());
            response.put("message", "Table migration started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start table migration for {}: {}", tableName, e.getMessage(), e);
            response.put("success", false);
            response.put("tableName", tableName);
            response.put("error", "Table migration failed");
            response.put("message", e.getMessage());
        }
        
        return response;
    }

    /**
     * 현재 배치 설정 정보 조회
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("chunkSize", batchProperties.getChunkSize());
        config.put("skipLimit", batchProperties.getSkipLimit());
        config.put("retryLimit", batchProperties.getRetryLimit());
        return config;
    }

    /**
     * 배치 설정 동적 변경
     */
    @PutMapping("/config")
    public Map<String, Object> updateConfig(
            @RequestParam(required = false) Integer chunkSize,
            @RequestParam(required = false) Integer skipLimit,
            @RequestParam(required = false) Integer retryLimit) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (chunkSize != null) {
            batchProperties.setChunkSize(chunkSize);
            logger.info("Updated chunk size to: {}", chunkSize);
        }
        
        if (skipLimit != null) {
            batchProperties.setSkipLimit(skipLimit);
            logger.info("Updated skip limit to: {}", skipLimit);
        }
        
        if (retryLimit != null) {
            batchProperties.setRetryLimit(retryLimit);
            logger.info("Updated retry limit to: {}", retryLimit);
        }
        
        response.put("success", true);
        response.put("message", "Configuration updated successfully");
        response.put("currentConfig", getConfig());
        
        return response;
    }
}
