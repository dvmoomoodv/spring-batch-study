package com.example.batch.writer;

import com.example.batch.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DatabaseItemWriter 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("데이터베이스 아이템 라이터 테스트")
class DatabaseItemWriterTest {

    @InjectMocks
    private DatabaseItemWriter databaseItemWriter;

    @Mock
    private JdbcTemplate mockJdbcTemplate;

    private List<DataRecord> testRecords;
    private Chunk<DataRecord> testChunk;

    @BeforeEach
    void setUp() {
        databaseItemWriter.resetStats();
        testRecords = new ArrayList<>();
        createTestData();
        testChunk = new Chunk<>(testRecords);
    }

    private void createTestData() {
        // 사용자 데이터
        Map<String, Object> userData = new HashMap<>();
        userData.put("user_id", 1);
        userData.put("name", "김철수");
        userData.put("email", "kim.cs@example.com");
        userData.put("phone_number", "010-1234-5678");
        userData.put("gender", "MALE");
        userData.put("is_active", true);
        userData.put("migrated_at", LocalDateTime.now());

        DataRecord userRecord = new DataRecord("users", userData);
        testRecords.add(userRecord);

        // 상품 데이터
        Map<String, Object> productData = new HashMap<>();
        productData.put("product_id", 100);
        productData.put("product_name", "갤럭시 스마트폰");
        productData.put("category", "전자제품");
        productData.put("price", 899000.00);
        productData.put("stock_quantity", 50);
        productData.put("sales_status", "ON_SALE");
        productData.put("migrated_at", LocalDateTime.now());

        DataRecord productRecord = new DataRecord("products", productData);
        testRecords.add(productRecord);
    }

    @Test
    @DisplayName("정상적인 데이터 쓰기 테스트")
    void testSuccessfulWrite() throws Exception {
        // Given
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(new int[]{1, 1}); // 각 레코드당 1개씩 성공

        // When
        assertDoesNotThrow(() -> databaseItemWriter.write(testChunk));

        // Then
        verify(mockJdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
        
        String stats = databaseItemWriter.getWritingStats();
        assertThat(stats).contains("Written: 2");
        assertThat(stats).contains("Errors: 0");
    }

    @Test
    @DisplayName("빈 청크 처리 테스트")
    void testWriteEmptyChunk() throws Exception {
        // Given
        Chunk<DataRecord> emptyChunk = new Chunk<>();

        // When & Then
        assertDoesNotThrow(() -> databaseItemWriter.write(emptyChunk));
        
        // JdbcTemplate 호출되지 않아야 함
        verify(mockJdbcTemplate, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    @DisplayName("같은 테이블 여러 레코드 처리 테스트")
    void testWriteMultipleRecordsToSameTable() throws Exception {
        // Given
        List<DataRecord> sameTableRecords = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", i);
            userData.put("name", "사용자" + i);
            userData.put("email", "user" + i + "@example.com");
            userData.put("migrated_at", LocalDateTime.now());
            
            sameTableRecords.add(new DataRecord("users", userData));
        }
        
        Chunk<DataRecord> sameTableChunk = new Chunk<>(sameTableRecords);
        
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(new int[]{1, 1, 1}); // 3개 모두 성공

        // When
        assertDoesNotThrow(() -> databaseItemWriter.write(sameTableChunk));

        // Then
        verify(mockJdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
        
        String stats = databaseItemWriter.getWritingStats();
        assertThat(stats).contains("Written: 3");
    }

    @Test
    @DisplayName("배치 INSERT 실패 시 개별 INSERT 시도 테스트")
    void testBatchInsertFailureWithIndividualRetry() throws Exception {
        // Given
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenThrow(new DataAccessException("Batch insert failed") {});
        
        when(mockJdbcTemplate.update(anyString(), any(Object[].class)))
            .thenReturn(1); // 개별 INSERT는 성공

        // When & Then
        assertDoesNotThrow(() -> databaseItemWriter.write(testChunk));
        
        // 배치 INSERT 1회 + 개별 INSERT 2회 호출되어야 함
        verify(mockJdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
        verify(mockJdbcTemplate, times(2)).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("NULL 데이터 처리 테스트")
    void testWriteRecordWithNullData() throws Exception {
        // Given
        List<DataRecord> nullDataRecords = new ArrayList<>();
        
        DataRecord recordWithNullData = new DataRecord("users", null);
        nullDataRecords.add(recordWithNullData);
        
        Chunk<DataRecord> nullDataChunk = new Chunk<>(nullDataRecords);

        // When & Then
        assertDoesNotThrow(() -> databaseItemWriter.write(nullDataChunk));
        
        // NULL 데이터는 처리되지 않아야 함
        verify(mockJdbcTemplate, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    @DisplayName("빈 데이터 처리 테스트")
    void testWriteRecordWithEmptyData() throws Exception {
        // Given
        List<DataRecord> emptyDataRecords = new ArrayList<>();
        
        DataRecord recordWithEmptyData = new DataRecord("users", new HashMap<>());
        emptyDataRecords.add(recordWithEmptyData);
        
        Chunk<DataRecord> emptyDataChunk = new Chunk<>(emptyDataRecords);

        // When & Then
        assertDoesNotThrow(() -> databaseItemWriter.write(emptyDataChunk));
        
        // 빈 데이터는 처리되지 않아야 함
        verify(mockJdbcTemplate, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    @DisplayName("다양한 데이터 타입 처리 테스트")
    void testWriteRecordWithVariousDataTypes() throws Exception {
        // Given
        Map<String, Object> mixedData = new HashMap<>();
        mixedData.put("id", 1);                           // Integer
        mixedData.put("name", "테스트");                   // String
        mixedData.put("price", 99.99);                    // Double
        mixedData.put("is_active", true);                 // Boolean
        mixedData.put("created_at", LocalDateTime.now()); // LocalDateTime
        mixedData.put("description", null);               // NULL
        
        List<DataRecord> mixedDataRecords = new ArrayList<>();
        mixedDataRecords.add(new DataRecord("test_table", mixedData));
        
        Chunk<DataRecord> mixedDataChunk = new Chunk<>(mixedDataRecords);
        
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(new int[]{1});

        // When
        assertDoesNotThrow(() -> databaseItemWriter.write(mixedDataChunk));

        // Then
        verify(mockJdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    @Test
    @DisplayName("부분 성공 시나리오 테스트")
    void testPartialSuccessScenario() throws Exception {
        // Given
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(new int[]{1, 0}); // 첫 번째는 성공, 두 번째는 실패

        // When
        assertDoesNotThrow(() -> databaseItemWriter.write(testChunk));

        // Then
        verify(mockJdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
        
        // 부분 성공에 대한 경고 로그가 출력되어야 함 (로그 검증은 별도 테스트에서)
    }

    @Test
    @DisplayName("개별 INSERT도 실패하는 경우 테스트")
    void testIndividualInsertFailure() throws Exception {
        // Given
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenThrow(new DataAccessException("Batch insert failed") {});
        
        when(mockJdbcTemplate.update(anyString(), any(Object[].class)))
            .thenThrow(new DataAccessException("Individual insert failed") {});

        // When & Then
        assertDoesNotThrow(() -> databaseItemWriter.write(testChunk));
        
        // 배치 INSERT 시도 + 개별 INSERT 시도가 모두 이루어져야 함
        verify(mockJdbcTemplate, times(2)).batchUpdate(anyString(), anyList());
        verify(mockJdbcTemplate, times(2)).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("통계 정보 정확성 테스트")
    void testStatisticsAccuracy() throws Exception {
        // Given
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(new int[]{1, 1});

        // When
        databaseItemWriter.write(testChunk);
        databaseItemWriter.write(testChunk); // 두 번 실행

        // Then
        String stats = databaseItemWriter.getWritingStats();
        assertThat(stats).contains("Written: 4"); // 2 * 2 = 4
        assertThat(stats).contains("Errors: 0");
        assertThat(stats).contains("Success Rate: 100.00%");
    }

    @Test
    @DisplayName("통계 초기화 테스트")
    void testStatisticsReset() throws Exception {
        // Given
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(new int[]{1, 1});

        // When
        databaseItemWriter.write(testChunk);
        databaseItemWriter.resetStats();

        // Then
        String stats = databaseItemWriter.getWritingStats();
        assertThat(stats).contains("Written: 0");
        assertThat(stats).contains("Errors: 0");
    }

    @Test
    @DisplayName("대용량 데이터 처리 시뮬레이션 테스트")
    void testLargeDataProcessingSimulation() throws Exception {
        // Given
        List<DataRecord> largeDataSet = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", i);
            data.put("name", "User" + i);
            data.put("migrated_at", LocalDateTime.now());
            
            largeDataSet.add(new DataRecord("users", data));
        }
        
        Chunk<DataRecord> largeChunk = new Chunk<>(largeDataSet);
        
        int[] successResults = new int[1000];
        for (int i = 0; i < 1000; i++) {
            successResults[i] = 1;
        }
        
        when(mockJdbcTemplate.batchUpdate(anyString(), anyList()))
            .thenReturn(successResults);

        // When
        assertDoesNotThrow(() -> databaseItemWriter.write(largeChunk));

        // Then
        verify(mockJdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
        
        String stats = databaseItemWriter.getWritingStats();
        assertThat(stats).contains("Written: 1000");
    }
}
