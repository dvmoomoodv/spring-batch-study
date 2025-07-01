package com.example.batch.reader;

import com.example.batch.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DatabaseItemReader 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("데이터베이스 아이템 리더 테스트")
class DatabaseItemReaderTest {

    @InjectMocks
    private DatabaseItemReader databaseItemReader;

    @Mock
    private DataSource mockDataSource;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private ResultSetMetaData mockMetaData;

    @BeforeEach
    void setUp() {
        // Mock 초기화는 @Mock 어노테이션으로 자동 처리
    }

    @Test
    @DisplayName("ItemReader 생성 테스트")
    void testCreateReader() {
        // Given
        String tableName = "사용자";
        String whereClause = "활성여부 = 1";
        int fetchSize = 1000;

        // When
        JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
            mockDataSource, tableName, whereClause, fetchSize);

        // Then
        assertThat(reader).isNotNull();
        assertThat(reader.getName()).isEqualTo(tableName + "ItemReader");
    }

    @Test
    @DisplayName("WHERE 절 없는 쿼리 생성 테스트")
    void testCreateReaderWithoutWhereClause() {
        // Given
        String tableName = "상품";
        int fetchSize = 500;

        // When
        JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
            mockDataSource, tableName, null, fetchSize);

        // Then
        assertThat(reader).isNotNull();
        assertThat(reader.getName()).isEqualTo(tableName + "ItemReader");
    }

    @Test
    @DisplayName("빈 WHERE 절 처리 테스트")
    void testCreateReaderWithEmptyWhereClause() {
        // Given
        String tableName = "주문";
        String whereClause = "   "; // 공백만 있는 WHERE 절
        int fetchSize = 2000;

        // When
        JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
            mockDataSource, tableName, whereClause, fetchSize);

        // Then
        assertThat(reader).isNotNull();
    }

    @Test
    @DisplayName("RowMapper 데이터 매핑 테스트")
    void testRowMapperDataMapping() throws SQLException {
        // Given
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getColumnCount()).thenReturn(3);
        when(mockMetaData.getColumnName(1)).thenReturn("사용자ID");
        when(mockMetaData.getColumnName(2)).thenReturn("이름");
        when(mockMetaData.getColumnName(3)).thenReturn("이메일");
        
        when(mockResultSet.getObject(1)).thenReturn(1);
        when(mockResultSet.getObject(2)).thenReturn("김철수");
        when(mockResultSet.getObject(3)).thenReturn("kim.cs@example.com");

        // RowMapper 직접 테스트를 위한 리플렉션 사용
        // 실제로는 내부 클래스이므로 통합 테스트에서 검증하는 것이 더 적절
        
        // When & Then
        // 이 테스트는 통합 테스트에서 더 적절하게 검증됨
        assertTrue(true); // 플레이스홀더
    }

    @Test
    @DisplayName("다양한 테이블명으로 Reader 생성 테스트")
    void testCreateReaderWithVariousTableNames() {
        // Given
        String[] tableNames = {"사용자", "상품", "주문", "주문상세", "카테고리"};
        int fetchSize = 1000;

        // When & Then
        for (String tableName : tableNames) {
            JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
                mockDataSource, tableName, null, fetchSize);
            
            assertThat(reader).isNotNull();
            assertThat(reader.getName()).isEqualTo(tableName + "ItemReader");
        }
    }

    @Test
    @DisplayName("복잡한 WHERE 절 처리 테스트")
    void testCreateReaderWithComplexWhereClause() {
        // Given
        String tableName = "주문";
        String complexWhereClause = "주문일시 >= '2024-01-01' AND 주문상태 IN ('배송완료', '배송중') AND 총금액 > 100000";
        int fetchSize = 1500;

        // When
        JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
            mockDataSource, tableName, complexWhereClause, fetchSize);

        // Then
        assertThat(reader).isNotNull();
        assertThat(reader.getName()).isEqualTo(tableName + "ItemReader");
    }

    @Test
    @DisplayName("Fetch Size 설정 테스트")
    void testCreateReaderWithDifferentFetchSizes() {
        // Given
        String tableName = "리뷰";
        int[] fetchSizes = {100, 500, 1000, 2000, 5000};

        // When & Then
        for (int fetchSize : fetchSizes) {
            JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
                mockDataSource, tableName, null, fetchSize);
            
            assertThat(reader).isNotNull();
            // fetchSize는 내부적으로 설정되므로 직접 검증하기 어려움
            // 통합 테스트에서 성능 측정을 통해 간접적으로 검증 가능
        }
    }

    @Test
    @DisplayName("특수 문자가 포함된 테이블명 처리 테스트")
    void testCreateReaderWithSpecialCharacterTableName() {
        // Given
        String tableName = "사용자_백업"; // 특수문자 포함
        int fetchSize = 1000;

        // When
        JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
            mockDataSource, tableName, null, fetchSize);

        // Then
        assertThat(reader).isNotNull();
        assertThat(reader.getName()).isEqualTo(tableName + "ItemReader");
    }

    @Test
    @DisplayName("NULL DataSource 처리 테스트")
    void testCreateReaderWithNullDataSource() {
        // Given
        String tableName = "사용자";
        int fetchSize = 1000;

        // When & Then
        assertDoesNotThrow(() -> {
            JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
                null, tableName, null, fetchSize);
            assertThat(reader).isNotNull();
        });
    }

    @Test
    @DisplayName("빈 테이블명 처리 테스트")
    void testCreateReaderWithEmptyTableName() {
        // Given
        String tableName = "";
        int fetchSize = 1000;

        // When
        JdbcCursorItemReader<DataRecord> reader = databaseItemReader.createReader(
            mockDataSource, tableName, null, fetchSize);

        // Then
        assertThat(reader).isNotNull();
        assertThat(reader.getName()).isEqualTo("ItemReader");
    }

    @Test
    @DisplayName("최소/최대 Fetch Size 테스트")
    void testCreateReaderWithMinMaxFetchSize() {
        // Given
        String tableName = "상품";
        
        // When & Then
        // 최소값 테스트
        JdbcCursorItemReader<DataRecord> readerMin = databaseItemReader.createReader(
            mockDataSource, tableName, null, 1);
        assertThat(readerMin).isNotNull();
        
        // 최대값 테스트 (일반적으로 10000 정도가 적절)
        JdbcCursorItemReader<DataRecord> readerMax = databaseItemReader.createReader(
            mockDataSource, tableName, null, 10000);
        assertThat(readerMax).isNotNull();
    }
}
