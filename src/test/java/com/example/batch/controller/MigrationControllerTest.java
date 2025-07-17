package com.example.batch.controller;

import com.example.batch.model.BatchConfiguration;
import com.example.batch.model.MigrationResult;
import com.example.batch.service.MigrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MigrationController.class)
@DisplayName("Migration Controller 단위 테스트")
class MigrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MigrationService migrationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("전체 이관 시작 API - 성공")
    void startFullMigration_Success() throws Exception {
        // Given
        MigrationResult expectedResult = MigrationResult.builder()
                .jobExecutionId(1L)
                .jobInstanceId(1L)
                .status("STARTED")
                .startTime("2024-07-16T10:30:00")
                .message("Migration job started successfully")
                .build();

        when(migrationService.startFullMigration(any(BatchConfiguration.class)))
                .thenReturn(expectedResult);

        // When & Then
        mockMvc.perform(post("/api/migration/start")
                        .param("chunkSize", "1000")
                        .param("skipLimit", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobExecutionId").value(1L))
                .andExpect(jsonPath("$.data.status").value("STARTED"))
                .andExpect(jsonPath("$.message").value("Migration job started successfully"));

        verify(migrationService).startFullMigration(any(BatchConfiguration.class));
    }

    @Test
    @DisplayName("전체 이관 시작 API - 잘못된 파라미터")
    void startFullMigration_InvalidParameters() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/migration/start")
                        .param("chunkSize", "-1")
                        .param("skipLimit", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETERS"));
    }

    @Test
    @DisplayName("특정 테이블 이관 시작 API - 성공")
    void startTableMigration_Success() throws Exception {
        // Given
        String tableName = "사용자";
        MigrationResult expectedResult = MigrationResult.builder()
                .jobExecutionId(2L)
                .status("STARTED")
                .tableName(tableName)
                .message("Table migration started successfully")
                .build();

        when(migrationService.startTableMigration(eq(tableName), any(BatchConfiguration.class)))
                .thenReturn(expectedResult);

        // When & Then
        mockMvc.perform(post("/api/migration/table/{tableName}", tableName)
                        .param("chunkSize", "500")
                        .param("whereClause", "사용자ID > 100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobExecutionId").value(2L))
                .andExpect(jsonPath("$.data.tableName").value(tableName));

        verify(migrationService).startTableMigration(eq(tableName), any(BatchConfiguration.class));
    }

    @Test
    @DisplayName("특정 테이블 이관 시작 API - 존재하지 않는 테이블")
    void startTableMigration_TableNotFound() throws Exception {
        // Given
        String invalidTableName = "존재하지않는테이블";
        when(migrationService.startTableMigration(eq(invalidTableName), any(BatchConfiguration.class)))
                .thenThrow(new IllegalArgumentException("Table not found: " + invalidTableName));

        // When & Then
        mockMvc.perform(post("/api/migration/table/{tableName}", invalidTableName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_TABLE_NAME"));
    }

    @Test
    @DisplayName("배치 중지 API - 성공")
    void stopMigration_Success() throws Exception {
        // Given
        Long jobExecutionId = 1L;
        MigrationResult expectedResult = MigrationResult.builder()
                .jobExecutionId(jobExecutionId)
                .status("STOPPED")
                .message("Migration job stopped successfully")
                .build();

        when(migrationService.stopMigration(jobExecutionId))
                .thenReturn(expectedResult);

        // When & Then
        mockMvc.perform(post("/api/migration/stop/{jobExecutionId}", jobExecutionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));

        verify(migrationService).stopMigration(jobExecutionId);
    }

    @Test
    @DisplayName("배치 재시작 API - 성공")
    void restartMigration_Success() throws Exception {
        // Given
        Long jobExecutionId = 1L;
        String fromStep = "userMigrationStep";
        MigrationResult expectedResult = MigrationResult.builder()
                .jobExecutionId(jobExecutionId)
                .status("RESTARTED")
                .message("Migration job restarted successfully")
                .build();

        when(migrationService.restartMigration(jobExecutionId, fromStep))
                .thenReturn(expectedResult);

        // When & Then
        mockMvc.perform(post("/api/migration/restart/{jobExecutionId}", jobExecutionId)
                        .param("fromStep", fromStep)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESTARTED"));

        verify(migrationService).restartMigration(jobExecutionId, fromStep);
    }

    @Test
    @DisplayName("모든 Job 목록 조회 API - 성공")
    void getAllJobs_Success() throws Exception {
        // Given
        // 목록 데이터는 실제 구현에 따라 조정

        // When & Then
        mockMvc.perform(get("/api/migration/jobs")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(migrationService).getAllJobs(0, 10);
    }

    @Test
    @DisplayName("도메인별 이관 시작 API - 성공")
    void startDomainMigration_Success() throws Exception {
        // Given
        String domainName = "USER";
        MigrationResult expectedResult = MigrationResult.builder()
                .jobExecutionId(3L)
                .status("STARTED")
                .domainName(domainName)
                .message("Domain migration started successfully")
                .build();

        when(migrationService.startDomainMigration(eq(domainName), any(BatchConfiguration.class)))
                .thenReturn(expectedResult);

        // When & Then
        mockMvc.perform(post("/api/migration/domain/{domainName}", domainName)
                        .param("chunkSize", "2000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.domainName").value(domainName));

        verify(migrationService).startDomainMigration(eq(domainName), any(BatchConfiguration.class));
    }

    @Test
    @DisplayName("이미 실행 중인 Job이 있을 때 - 충돌 오류")
    void startMigration_JobAlreadyRunning() throws Exception {
        // Given
        when(migrationService.startFullMigration(any(BatchConfiguration.class)))
                .thenThrow(new IllegalStateException("Job is already running"));

        // When & Then
        mockMvc.perform(post("/api/migration/start")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("JOB_ALREADY_RUNNING"));
    }
}
