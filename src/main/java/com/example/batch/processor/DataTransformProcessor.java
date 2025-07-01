package com.example.batch.processor;

import com.example.batch.model.DataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 데이터 변환 및 검증을 수행하는 ItemProcessor
 * MSSQL에서 MariaDB로 이관 시 필요한 데이터 변환 로직 포함
 * 한글 컬럼명/값을 영어로 변환하는 로직 포함
 */
@Component
public class DataTransformProcessor implements ItemProcessor<DataRecord, DataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(DataTransformProcessor.class);

    private long processedCount = 0;
    private long errorCount = 0;

    // 한글 -> 영어 테이블명 매핑
    private static final Map<String, String> TABLE_NAME_MAPPING = new HashMap<>();

    // 한글 -> 영어 컬럼명 매핑
    private static final Map<String, Map<String, String>> COLUMN_NAME_MAPPING = new HashMap<>();

    // 한글 -> 영어 값 매핑
    private static final Map<String, Map<String, String>> VALUE_MAPPING = new HashMap<>();

    static {
        initializeTableMapping();
        initializeColumnMapping();
        initializeValueMapping();
    }

    /**
     * 테이블명 매핑 초기화
     */
    private static void initializeTableMapping() {
        TABLE_NAME_MAPPING.put("사용자", "users");
        TABLE_NAME_MAPPING.put("상품", "products");
        TABLE_NAME_MAPPING.put("주문", "orders");
        TABLE_NAME_MAPPING.put("주문상세", "order_details");
        TABLE_NAME_MAPPING.put("카테고리", "categories");
        TABLE_NAME_MAPPING.put("리뷰", "reviews");
        TABLE_NAME_MAPPING.put("공지사항", "notices");
        TABLE_NAME_MAPPING.put("쿠폰", "coupons");
        TABLE_NAME_MAPPING.put("배송", "deliveries");
        TABLE_NAME_MAPPING.put("문의", "inquiries");
    }

    /**
     * 컬럼명 매핑 초기화
     */
    private static void initializeColumnMapping() {
        // 사용자 테이블 컬럼 매핑
        Map<String, String> userColumns = new HashMap<>();
        userColumns.put("사용자ID", "user_id");
        userColumns.put("이름", "name");
        userColumns.put("이메일", "email");
        userColumns.put("전화번호", "phone_number");
        userColumns.put("주소", "address");
        userColumns.put("생년월일", "birth_date");
        userColumns.put("성별", "gender");
        userColumns.put("직업", "occupation");
        userColumns.put("등록일시", "created_at");
        userColumns.put("수정일시", "updated_at");
        userColumns.put("활성여부", "is_active");
        COLUMN_NAME_MAPPING.put("사용자", userColumns);

        // 상품 테이블 컬럼 매핑
        Map<String, String> productColumns = new HashMap<>();
        productColumns.put("상품ID", "product_id");
        productColumns.put("상품명", "product_name");
        productColumns.put("상품설명", "product_description");
        productColumns.put("카테고리", "category");
        productColumns.put("가격", "price");
        productColumns.put("재고수량", "stock_quantity");
        productColumns.put("제조사", "manufacturer");
        productColumns.put("원산지", "origin_country");
        productColumns.put("등록일시", "created_at");
        productColumns.put("수정일시", "updated_at");
        productColumns.put("판매상태", "sales_status");
        COLUMN_NAME_MAPPING.put("상품", productColumns);

        // 주문 테이블 컬럼 매핑
        Map<String, String> orderColumns = new HashMap<>();
        orderColumns.put("주문ID", "order_id");
        orderColumns.put("사용자ID", "user_id");
        orderColumns.put("주문번호", "order_number");
        orderColumns.put("주문일시", "order_date");
        orderColumns.put("총금액", "total_amount");
        orderColumns.put("배송주소", "delivery_address");
        orderColumns.put("주문상태", "order_status");
        orderColumns.put("결제방법", "payment_method");
        orderColumns.put("배송메모", "delivery_memo");
        COLUMN_NAME_MAPPING.put("주문", orderColumns);

        // 주문상세 테이블 컬럼 매핑
        Map<String, String> orderDetailColumns = new HashMap<>();
        orderDetailColumns.put("주문상세ID", "order_detail_id");
        orderDetailColumns.put("주문ID", "order_id");
        orderDetailColumns.put("상품ID", "product_id");
        orderDetailColumns.put("수량", "quantity");
        orderDetailColumns.put("단가", "unit_price");
        orderDetailColumns.put("소계", "subtotal");
        COLUMN_NAME_MAPPING.put("주문상세", orderDetailColumns);

        // 카테고리 테이블 컬럼 매핑
        Map<String, String> categoryColumns = new HashMap<>();
        categoryColumns.put("카테고리ID", "category_id");
        categoryColumns.put("카테고리명", "category_name");
        categoryColumns.put("상위카테고리ID", "parent_category_id");
        categoryColumns.put("카테고리설명", "category_description");
        categoryColumns.put("정렬순서", "sort_order");
        categoryColumns.put("사용여부", "is_active");
        categoryColumns.put("등록일시", "created_at");
        COLUMN_NAME_MAPPING.put("카테고리", categoryColumns);
    }

    /**
     * 값 매핑 초기화 (한글 값을 영어로 변환)
     */
    private static void initializeValueMapping() {
        // 성별 매핑
        Map<String, String> genderMapping = new HashMap<>();
        genderMapping.put("남성", "MALE");
        genderMapping.put("여성", "FEMALE");
        VALUE_MAPPING.put("gender", genderMapping);

        // 주문상태 매핑
        Map<String, String> orderStatusMapping = new HashMap<>();
        orderStatusMapping.put("주문접수", "ORDER_RECEIVED");
        orderStatusMapping.put("결제완료", "PAYMENT_COMPLETED");
        orderStatusMapping.put("배송준비", "PREPARING");
        orderStatusMapping.put("배송중", "SHIPPING");
        orderStatusMapping.put("배송완료", "DELIVERED");
        orderStatusMapping.put("주문취소", "CANCELLED");
        VALUE_MAPPING.put("order_status", orderStatusMapping);

        // 판매상태 매핑
        Map<String, String> salesStatusMapping = new HashMap<>();
        salesStatusMapping.put("판매중", "ON_SALE");
        salesStatusMapping.put("품절", "OUT_OF_STOCK");
        salesStatusMapping.put("판매중단", "DISCONTINUED");
        VALUE_MAPPING.put("sales_status", salesStatusMapping);

        // 배송상태 매핑
        Map<String, String> deliveryStatusMapping = new HashMap<>();
        deliveryStatusMapping.put("배송준비", "PREPARING");
        deliveryStatusMapping.put("배송중", "SHIPPING");
        deliveryStatusMapping.put("배송완료", "DELIVERED");
        deliveryStatusMapping.put("배송실패", "FAILED");
        VALUE_MAPPING.put("delivery_status", deliveryStatusMapping);

        // 문의유형 매핑
        Map<String, String> inquiryTypeMapping = new HashMap<>();
        inquiryTypeMapping.put("상품문의", "PRODUCT_INQUIRY");
        inquiryTypeMapping.put("배송문의", "DELIVERY_INQUIRY");
        inquiryTypeMapping.put("기타문의", "OTHER_INQUIRY");
        VALUE_MAPPING.put("inquiry_type", inquiryTypeMapping);

        // 처리상태 매핑
        Map<String, String> statusMapping = new HashMap<>();
        statusMapping.put("접수", "RECEIVED");
        statusMapping.put("처리중", "PROCESSING");
        statusMapping.put("완료", "COMPLETED");
        statusMapping.put("취소", "CANCELLED");
        VALUE_MAPPING.put("status", statusMapping);

        // 할인타입 매핑
        Map<String, String> discountTypeMapping = new HashMap<>();
        discountTypeMapping.put("정액할인", "FIXED_AMOUNT");
        discountTypeMapping.put("정률할인", "PERCENTAGE");
        VALUE_MAPPING.put("discount_type", discountTypeMapping);
    }

    @Override
    public DataRecord process(DataRecord item) throws Exception {
        try {
            processedCount++;
            
            // 데이터 변환 로직
            DataRecord transformedRecord = transformData(item);
            
            // 데이터 검증
            if (!validateData(transformedRecord)) {
                logger.warn("Data validation failed for record from table: {}, data: {}", 
                    item.getTableName(), item.getData());
                errorCount++;
                return null; // null 반환 시 해당 레코드는 Writer로 전달되지 않음
            }
            
            // 진행 상황 로깅
            if (processedCount % 1000 == 0) {
                logger.info("Processed {} records, errors: {} for table: {}", 
                    processedCount, errorCount, item.getTableName());
            }
            
            return transformedRecord;
            
        } catch (Exception e) {
            errorCount++;
            logger.error("Error processing record from table: {}, error: {}", 
                item.getTableName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 데이터 변환 로직
     * MSSQL과 MariaDB 간의 데이터 타입 차이 등을 처리
     * 한글 테이블명/컬럼명/값을 영어로 변환
     */
    private DataRecord transformData(DataRecord record) {
        String originalTableName = record.getTableName();
        Map<String, Object> originalData = record.getData();

        // 1. 테이블명 변환
        String englishTableName = TABLE_NAME_MAPPING.getOrDefault(originalTableName, originalTableName);
        record.setTableName(englishTableName);

        // 2. 컬럼명 및 값 변환
        Map<String, Object> transformedData = new HashMap<>();
        Map<String, String> columnMapping = COLUMN_NAME_MAPPING.get(originalTableName);

        if (columnMapping != null) {
            for (Map.Entry<String, Object> entry : originalData.entrySet()) {
                String koreanColumnName = entry.getKey();
                Object value = entry.getValue();

                // 컬럼명 변환
                String englishColumnName = columnMapping.getOrDefault(koreanColumnName, koreanColumnName);

                // 값 변환 및 처리
                Object transformedValue = transformValue(englishColumnName, value);

                transformedData.put(englishColumnName, transformedValue);
            }
        } else {
            // 매핑이 없는 경우 원본 데이터 사용 (기본 변환만 적용)
            logger.warn("No column mapping found for table: {}", originalTableName);
            for (Map.Entry<String, Object> entry : originalData.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();
                Object transformedValue = transformValue(columnName, value);
                transformedData.put(columnName, transformedValue);
            }
        }

        // 3. 이관 시점 정보 추가
        transformedData.put("migrated_at", LocalDateTime.now());

        record.setData(transformedData);

        logger.debug("Transformed table: {} -> {}, columns: {}",
            originalTableName, englishTableName, transformedData.keySet());

        return record;
    }

    /**
     * 개별 값 변환 로직
     */
    private Object transformValue(String columnName, Object value) {
        if (value == null) {
            return null;
        }

        // 1. 문자열 데이터 트림 처리
        if (value instanceof String) {
            String stringValue = ((String) value).trim();
            if (stringValue.isEmpty()) {
                return null;
            }

            // 2. 한글 값을 영어로 변환
            Map<String, String> valueMapping = VALUE_MAPPING.get(columnName);
            if (valueMapping != null && valueMapping.containsKey(stringValue)) {
                String mappedValue = valueMapping.get(stringValue);
                logger.debug("Value mapping: {} -> {} for column: {}", stringValue, mappedValue, columnName);
                return mappedValue;
            }

            return stringValue;
        }

        // 3. MSSQL의 bit 타입을 MariaDB의 boolean으로 변환
        if (value instanceof Boolean) {
            return value;
        }

        // 4. 숫자 타입 처리
        if (value instanceof Number) {
            return value;
        }

        // 5. 날짜/시간 데이터 처리
        if (value instanceof java.sql.Timestamp) {
            return value;
        }

        if (value instanceof java.sql.Date) {
            return value;
        }

        // 6. 기타 타입은 그대로 반환
        return value;
    }

    /**
     * 데이터 검증 로직
     */
    private boolean validateData(DataRecord record) {
        Map<String, Object> data = record.getData();
        
        // 기본 검증: 데이터가 존재하는지 확인
        if (data == null || data.isEmpty()) {
            logger.warn("Empty data record for table: {}", record.getTableName());
            return false;
        }
        
        // 필수 필드 검증 (테이블별로 커스터마이징 필요)
        // 예: ID 필드가 존재하는지 확인
        if (!data.containsKey("id") && !data.containsKey("ID")) {
            logger.warn("Missing ID field in record for table: {}", record.getTableName());
            // return false; // 필요에 따라 활성화
        }
        
        // 추가 비즈니스 로직 검증
        // 예: 특정 필드의 값 범위 검증, 형식 검증 등
        
        return true;
    }

    /**
     * 처리 통계 정보 반환
     */
    public String getProcessingStats() {
        return String.format("Processed: %d, Errors: %d, Success Rate: %.2f%%", 
            processedCount, errorCount, 
            processedCount > 0 ? ((double)(processedCount - errorCount) / processedCount * 100) : 0.0);
    }

    /**
     * 통계 초기화
     */
    public void resetStats() {
        processedCount = 0;
        errorCount = 0;
    }
}
