package com.example.batch.service;

import com.example.batch.exception.MigrationException;
import com.example.batch.model.BatchConfiguration;
import com.example.batch.model.MigrationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Migration Service 단위 테스트")
class MigrationServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job dataMigrationJob;

    @Mock
    private Job singleTableMigrationJob;

    @Mock
    private JobFactory jobFactory;

    @InjectMocks
    private MigrationService migrationService;

    private BatchConfiguration defaultConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = BatchConfiguration.builder()
                .chunkSize(1000)
                .skipLimit(100)
                .retryLimit(3)
                .build();
    }

    @Test
    @DisplayName("전체 이관 시작 - 성공")
    void startFullMigration_Success() throws Exception {
        // Given
        JobExecution jobExecution = createJobExecution(1L, BatchStatus.STARTED);
        
        when(jobLauncher.run(eq(dataMigrationJob), any(JobParameters.class)))
                .thenReturn(jobExecution);

        // When
        MigrationResult result = migrationService.startFullMigration(defaultConfig);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("STARTED");
        assertThat(result.getMessage()).contains("started successfully");

        verify(jobLauncher).run(eq(dataMigrationJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("전체 이관 시작 - Job 이미 실행 중")
    void startFullMigration_JobAlreadyRunning() throws Exception {
        // Given
        when(jobLauncher.run(eq(dataMigrationJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("Job is already running"));

        // When & Then
        assertThatThrownBy(() -> migrationService.startFullMigration(defaultConfig))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("already running");

        verify(jobLauncher).run(eq(dataMigrationJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("테이블 이관 시작 - 성공")
    void startTableMigration_Success() throws Exception {
        // Given
        String tableName = "사용자";
        JobExecution jobExecution = createJobExecution(2L, BatchStatus.STARTED);
        
        when(jobFactory.createSingleTableMigrationJob(tableName, null))
                .thenReturn(singleTableMigrationJob);
        when(jobLauncher.run(eq(singleTableMigrationJob), any(JobParameters.class)))
                .thenReturn(jobExecution);

        // When
        MigrationResult result = migrationService.startTableMigration(tableName, defaultConfig);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(2L);
        assertThat(result.getTableName()).isEqualTo(tableName);
        assertThat(result.getStatus()).isEqualTo("STARTED");

        verify(jobFactory).createSingleTableMigrationJob(tableName, null);
        verify(jobLauncher).run(eq(singleTableMigrationJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("테이블 이관 시작 - 존재하지 않는 테이블")
    void startTableMigration_TableNotFound() {
        // Given
        String invalidTableName = "존재하지않는테이블";
        when(jobFactory.createSingleTableMigrationJob(invalidTableName, null))
                .thenThrow(new IllegalArgumentException("Table not found: " + invalidTableName));

        // When & Then
        assertThatThrownBy(() -> migrationService.startTableMigration(invalidTableName, defaultConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Table not found");

        verify(jobFactory).createSingleTableMigrationJob(invalidTableName, null);
        verifyNoInteractions(jobLauncher);
    }

    @Test
    @DisplayName("도메인 이관 시작 - 성공")
    void startDomainMigration_Success() throws Exception {
        // Given
        String domainName = "USER";
        JobExecution jobExecution = createJobExecution(3L, BatchStatus.STARTED);
        Job domainMigrationJob = mock(Job.class);
        
        when(jobFactory.createDomainMigrationJob(domainName))
                .thenReturn(domainMigrationJob);
        when(jobLauncher.run(eq(domainMigrationJob), any(JobParameters.class)))
                .thenReturn(jobExecution);

        // When
        MigrationResult result = migrationService.startDomainMigration(domainName, defaultConfig);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(3L);
        assertThat(result.getDomainName()).isEqualTo(domainName);
        assertThat(result.getStatus()).isEqualTo("STARTED");

        verify(jobFactory).createDomainMigrationJob(domainName);
        verify(jobLauncher).run(eq(domainMigrationJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("배치 중지 - 성공")
    void stopMigration_Success() {
        // Given
        Long jobExecutionId = 1L;
        JobExecution jobExecution = createJobExecution(jobExecutionId, BatchStatus.STOPPED);
        
        when(jobRepository.getLastJobExecution(any(), any()))
                .thenReturn(jobExecution);

        // When
        MigrationResult result = migrationService.stopMigration(jobExecutionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(result.getStatus()).isEqualTo("STOPPED");
    }

    @Test
    @DisplayName("배치 재시작 - 성공")
    void restartMigration_Success() throws Exception {
        // Given
        Long jobExecutionId = 1L;
        String fromStep = "userMigrationStep";
        JobExecution jobExecution = createJobExecution(jobExecutionId, BatchStatus.STARTED);
        
        when(jobRepository.getLastJobExecution(any(), any()))
                .thenReturn(jobExecution);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(jobExecution);

        // When
        MigrationResult result = migrationService.restartMigration(jobExecutionId, fromStep);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(result.getStatus()).isEqualTo("STARTED");

        verify(jobLauncher).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("Job 상태 조회 - 성공")
    void getJobStatus_Success() {
        // Given
        Long jobExecutionId = 1L;
        JobExecution jobExecution = createJobExecution(jobExecutionId, BatchStatus.COMPLETED);
        
        when(jobRepository.getLastJobExecution(any(), any()))
                .thenReturn(jobExecution);

        // When
        MigrationResult result = migrationService.getJobStatus(jobExecutionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getJobExecutionId()).isEqualTo(jobExecutionId);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Job 파라미터 검증 - 잘못된 청크 크기")
    void validateJobParameters_InvalidChunkSize() {
        // Given
        BatchConfiguration invalidConfig = BatchConfiguration.builder()
                .chunkSize(-1)
                .skipLimit(100)
                .build();

        // When & Then
        assertThatThrownBy(() -> migrationService.startFullMigration(invalidConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chunk size must be positive");
    }

    @Test
    @DisplayName("Job 파라미터 검증 - 잘못된 스킵 제한")
    void validateJobParameters_InvalidSkipLimit() {
        // Given
        BatchConfiguration invalidConfig = BatchConfiguration.builder()
                .chunkSize(1000)
                .skipLimit(-1)
                .build();

        // When & Then
        assertThatThrownBy(() -> migrationService.startFullMigration(invalidConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skip limit must be non-negative");
    }

    @Test
    @DisplayName("동시 실행 제한 - 같은 테이블 중복 실행")
    void preventConcurrentExecution_SameTable() throws Exception {
        // Given
        String tableName = "사용자";
        when(jobFactory.createSingleTableMigrationJob(tableName, null))
                .thenReturn(singleTableMigrationJob);
        when(jobLauncher.run(eq(singleTableMigrationJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("Job is already running"));

        // When & Then
        assertThatThrownBy(() -> migrationService.startTableMigration(tableName, defaultConfig))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("already running");
    }

    private JobExecution createJobExecution(Long executionId, BatchStatus status) {
        JobInstance jobInstance = new JobInstance(1L, "testJob");
        JobExecution jobExecution = new JobExecution(jobInstance, executionId, new JobParameters());
        jobExecution.setStatus(status);
        jobExecution.setStartTime(LocalDateTime.now());
        
        if (status == BatchStatus.COMPLETED || status == BatchStatus.STOPPED) {
            jobExecution.setEndTime(LocalDateTime.now());
        }
        
        return jobExecution;
    }
}
