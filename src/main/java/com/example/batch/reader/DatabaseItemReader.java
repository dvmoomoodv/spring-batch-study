package com.example.batch.reader;

import com.example.batch.model.DataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MSSQL 데이터베이스에서 데이터를 읽어오는 ItemReader
 * 커서 기반으로 메모리 효율적인 데이터 읽기 수행
 */
@Component
public class DatabaseItemReader {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseItemReader.class);

    /**
     * 지정된 테이블의 데이터를 읽어오는 ItemReader 생성
     * 
     * @param dataSource 소스 데이터베이스
     * @param tableName 읽어올 테이블명
     * @param whereClause WHERE 조건 (선택사항)
     * @param fetchSize 한 번에 가져올 레코드 수
     * @return JdbcCursorItemReader
     */
    public JdbcCursorItemReader<DataRecord> createReader(
            DataSource dataSource, 
            String tableName, 
            String whereClause, 
            int fetchSize) {
        
        String sql = buildSelectQuery(tableName, whereClause);
        logger.info("Creating ItemReader for table: {} with SQL: {}", tableName, sql);
        
        return new JdbcCursorItemReaderBuilder<DataRecord>()
                .name(tableName + "ItemReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper(new DataRecordRowMapper(tableName))
                .fetchSize(fetchSize)
                .build();
    }

    /**
     * SELECT 쿼리 생성
     */
    private String buildSelectQuery(String tableName, String whereClause) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(tableName);
        
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        return sql.toString();
    }

    /**
     * ResultSet을 DataRecord로 매핑하는 RowMapper
     */
    private static class DataRecordRowMapper implements RowMapper<DataRecord> {
        
        private final String tableName;
        private final Logger logger = LoggerFactory.getLogger(DataRecordRowMapper.class);
        
        public DataRecordRowMapper(String tableName) {
            this.tableName = tableName;
        }
        
        @Override
        public DataRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            Map<String, Object> data = new HashMap<>();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                data.put(columnName, value);
            }
            
            DataRecord record = new DataRecord(tableName, data);
            
            if (rowNum % 10000 == 0) {
                logger.debug("Read {} records from table: {}", rowNum + 1, tableName);
            }
            
            return record;
        }
    }
}
