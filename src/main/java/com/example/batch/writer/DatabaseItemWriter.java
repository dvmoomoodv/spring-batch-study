package com.example.batch.writer;

import com.example.batch.model.DataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MariaDB 데이터베이스에 데이터를 쓰는 ItemWriter
 * 배치 INSERT를 통한 성능 최적화
 */
@Component
public class DatabaseItemWriter implements ItemWriter<DataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseItemWriter.class);
    
    private final JdbcTemplate targetJdbcTemplate;
    private long writtenCount = 0;
    private long errorCount = 0;

    public DatabaseItemWriter(JdbcTemplate targetJdbcTemplate) {
        this.targetJdbcTemplate = targetJdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends DataRecord> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        // 테이블별로 그룹화
        Map<String, List<DataRecord>> recordsByTable = chunk.getItems().stream()
                .collect(Collectors.groupingBy(DataRecord::getTableName));

        // 각 테이블별로 배치 INSERT 수행
        for (Map.Entry<String, List<DataRecord>> entry : recordsByTable.entrySet()) {
            String tableName = entry.getKey();
            List<DataRecord> records = entry.getValue();
            
            try {
                writeRecordsToTable(tableName, records);
                writtenCount += records.size();
                
                logger.info("Successfully wrote {} records to table: {}, Total written: {}", 
                    records.size(), tableName, writtenCount);
                    
            } catch (Exception e) {
                errorCount += records.size();
                logger.error("Failed to write {} records to table: {}, Error: {}", 
                    records.size(), tableName, e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * 특정 테이블에 레코드들을 배치 INSERT
     */
    private void writeRecordsToTable(String tableName, List<DataRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        // 첫 번째 레코드를 기준으로 컬럼 정보 추출
        DataRecord firstRecord = records.get(0);
        Map<String, Object> firstData = firstRecord.getData();
        
        if (firstData == null || firstData.isEmpty()) {
            logger.warn("No data to write for table: {}", tableName);
            return;
        }

        List<String> columns = new ArrayList<>(firstData.keySet());
        
        // INSERT 쿼리 생성
        String insertSql = buildInsertQuery(tableName, columns);
        logger.debug("Insert SQL for table {}: {}", tableName, insertSql);

        // 배치 파라미터 준비
        List<Object[]> batchArgs = new ArrayList<>();
        
        for (DataRecord record : records) {
            Object[] args = new Object[columns.size()];
            Map<String, Object> data = record.getData();
            
            for (int i = 0; i < columns.size(); i++) {
                args[i] = data.get(columns.get(i));
            }
            batchArgs.add(args);
        }

        try {
            // 배치 INSERT 실행
            int[] updateCounts = targetJdbcTemplate.batchUpdate(insertSql, batchArgs);
            
            // 결과 검증
            int successCount = 0;
            for (int count : updateCounts) {
                if (count > 0) {
                    successCount++;
                }
            }
            
            if (successCount != records.size()) {
                logger.warn("Expected {} inserts but {} succeeded for table: {}", 
                    records.size(), successCount, tableName);
            }
            
        } catch (DataAccessException e) {
            logger.error("Database error writing to table: {}, SQL: {}, Error: {}", 
                tableName, insertSql, e.getMessage());
            
            // 개별 INSERT 시도 (배치 실패 시 복구 로직)
            attemptIndividualInserts(tableName, insertSql, batchArgs);
        }
    }

    /**
     * INSERT 쿼리 생성
     */
    private String buildInsertQuery(String tableName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(columns.stream().map(c -> "?").collect(Collectors.joining(", ")));
        sql.append(")");
        
        return sql.toString();
    }

    /**
     * 배치 INSERT 실패 시 개별 INSERT 시도
     */
    private void attemptIndividualInserts(String tableName, String insertSql, List<Object[]> batchArgs) {
        logger.info("Attempting individual inserts for table: {} ({} records)", tableName, batchArgs.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Object[] args : batchArgs) {
            try {
                int result = targetJdbcTemplate.update(insertSql, args);
                if (result > 0) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (DataAccessException e) {
                failCount++;
                logger.warn("Failed to insert individual record to table: {}, args: {}, error: {}", 
                    tableName, java.util.Arrays.toString(args), e.getMessage());
            }
        }
        
        logger.info("Individual insert results for table: {} - Success: {}, Failed: {}", 
            tableName, successCount, failCount);
    }

    /**
     * 쓰기 통계 정보 반환
     */
    public String getWritingStats() {
        return String.format("Written: %d, Errors: %d, Success Rate: %.2f%%", 
            writtenCount, errorCount, 
            (writtenCount + errorCount) > 0 ? ((double)writtenCount / (writtenCount + errorCount) * 100) : 0.0);
    }

    /**
     * 통계 초기화
     */
    public void resetStats() {
        writtenCount = 0;
        errorCount = 0;
    }
}
