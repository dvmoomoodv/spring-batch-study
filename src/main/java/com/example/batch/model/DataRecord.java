package com.example.batch.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 데이터 이관을 위한 범용 데이터 레코드 클래스
 * 다양한 테이블 구조에 대응할 수 있도록 Map 기반으로 설계
 */
public class DataRecord {
    
    private String tableName;
    private Map<String, Object> data;
    private LocalDateTime processedAt;
    private String sourceQuery;
    
    public DataRecord() {
        this.processedAt = LocalDateTime.now();
    }
    
    public DataRecord(String tableName, Map<String, Object> data) {
        this.tableName = tableName;
        this.data = data;
        this.processedAt = LocalDateTime.now();
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getSourceQuery() {
        return sourceQuery;
    }
    
    public void setSourceQuery(String sourceQuery) {
        this.sourceQuery = sourceQuery;
    }
    
    public Object getValue(String columnName) {
        return data != null ? data.get(columnName) : null;
    }
    
    public void setValue(String columnName, Object value) {
        if (data != null) {
            data.put(columnName, value);
        }
    }
    
    @Override
    public String toString() {
        return "DataRecord{" +
                "tableName='" + tableName + '\'' +
                ", dataSize=" + (data != null ? data.size() : 0) +
                ", processedAt=" + processedAt +
                '}';
    }
}
