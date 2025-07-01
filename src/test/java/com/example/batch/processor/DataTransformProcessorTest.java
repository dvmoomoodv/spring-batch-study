package com.example.batch.processor;

import com.example.batch.model.DataRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DataTransformProcessor 테스트
 * 한글-영어 변환 로직 및 데이터 변환 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("데이터 변환 프로세서 테스트")
class DataTransformProcessorTest {

    @InjectMocks
    private DataTransformProcessor processor;

    private DataRecord testRecord;
    private Map<String, Object> testData;

    @BeforeEach
    void setUp() {
        processor.resetStats();
        testData = new HashMap<>();
        testRecord = new DataRecord();
    }

    @Test
    @DisplayName("사용자 테이블 한글-영어 변환 테스트")
    void testUserTableTransformation() throws Exception {
        // Given
        testData.put("사용자ID", 1);
        testData.put("이름", "김철수");
        testData.put("이메일", "kim.cs@example.com");
        testData.put("전화번호", "010-1234-5678");
        testData.put("주소", "서울시 강남구");
        testData.put("성별", "남성");
        testData.put("직업", "개발자");
        testData.put("활성여부", true);
        
        testRecord.setTableName("사용자");
        testRecord.setData(testData);

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTableName()).isEqualTo("users");
        
        Map<String, Object> resultData = result.getData();
        assertThat(resultData.get("user_id")).isEqualTo(1);
        assertThat(resultData.get("name")).isEqualTo("김철수");
        assertThat(resultData.get("email")).isEqualTo("kim.cs@example.com");
        assertThat(resultData.get("phone_number")).isEqualTo("010-1234-5678");
        assertThat(resultData.get("address")).isEqualTo("서울시 강남구");
        assertThat(resultData.get("gender")).isEqualTo("MALE");
        assertThat(resultData.get("occupation")).isEqualTo("개발자");
        assertThat(resultData.get("is_active")).isEqualTo(true);
        assertThat(resultData.get("migrated_at")).isInstanceOf(LocalDateTime.class);
    }

    @Test
    @DisplayName("상품 테이블 한글-영어 변환 테스트")
    void testProductTableTransformation() throws Exception {
        // Given
        testData.put("상품ID", 100);
        testData.put("상품명", "갤럭시 스마트폰");
        testData.put("상품설명", "최신 안드로이드 스마트폰");
        testData.put("카테고리", "전자제품");
        testData.put("가격", 899000.00);
        testData.put("재고수량", 50);
        testData.put("제조사", "삼성전자");
        testData.put("원산지", "대한민국");
        testData.put("판매상태", "판매중");
        
        testRecord.setTableName("상품");
        testRecord.setData(testData);

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTableName()).isEqualTo("products");
        
        Map<String, Object> resultData = result.getData();
        assertThat(resultData.get("product_id")).isEqualTo(100);
        assertThat(resultData.get("product_name")).isEqualTo("갤럭시 스마트폰");
        assertThat(resultData.get("product_description")).isEqualTo("최신 안드로이드 스마트폰");
        assertThat(resultData.get("category")).isEqualTo("전자제품");
        assertThat(resultData.get("price")).isEqualTo(899000.00);
        assertThat(resultData.get("stock_quantity")).isEqualTo(50);
        assertThat(resultData.get("manufacturer")).isEqualTo("삼성전자");
        assertThat(resultData.get("origin_country")).isEqualTo("대한민국");
        assertThat(resultData.get("sales_status")).isEqualTo("ON_SALE");
    }

    @Test
    @DisplayName("주문 테이블 상태값 변환 테스트")
    void testOrderStatusTransformation() throws Exception {
        // Given
        testData.put("주문ID", 1001);
        testData.put("사용자ID", 1);
        testData.put("주문번호", "ORD-2024-001");
        testData.put("총금액", 934000.00);
        testData.put("주문상태", "배송완료");
        testData.put("결제방법", "신용카드");
        
        testRecord.setTableName("주문");
        testRecord.setData(testData);

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTableName()).isEqualTo("orders");
        
        Map<String, Object> resultData = result.getData();
        assertThat(resultData.get("order_id")).isEqualTo(1001);
        assertThat(resultData.get("user_id")).isEqualTo(1);
        assertThat(resultData.get("order_number")).isEqualTo("ORD-2024-001");
        assertThat(resultData.get("total_amount")).isEqualTo(934000.00);
        assertThat(resultData.get("order_status")).isEqualTo("DELIVERED");
        assertThat(resultData.get("payment_method")).isEqualTo("신용카드");
    }

    @Test
    @DisplayName("NULL 값 처리 테스트")
    void testNullValueHandling() throws Exception {
        // Given
        testData.put("사용자ID", 1);
        testData.put("이름", "김철수");
        testData.put("이메일", null);
        testData.put("전화번호", "");
        testData.put("주소", "   ");
        testData.put("성별", "남성");
        
        testRecord.setTableName("사용자");
        testRecord.setData(testData);

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNotNull();
        Map<String, Object> resultData = result.getData();
        assertThat(resultData.get("user_id")).isEqualTo(1);
        assertThat(resultData.get("name")).isEqualTo("김철수");
        assertThat(resultData.get("email")).isNull();
        assertThat(resultData.get("phone_number")).isNull(); // 빈 문자열은 null로 변환
        assertThat(resultData.get("address")).isNull(); // 공백만 있는 문자열은 null로 변환
        assertThat(resultData.get("gender")).isEqualTo("MALE");
    }

    @Test
    @DisplayName("매핑되지 않은 테이블 처리 테스트")
    void testUnmappedTableHandling() throws Exception {
        // Given
        testData.put("id", 1);
        testData.put("name", "테스트");
        testData.put("status", "활성");
        
        testRecord.setTableName("알수없는테이블");
        testRecord.setData(testData);

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTableName()).isEqualTo("알수없는테이블"); // 테이블명 변환 안됨
        
        Map<String, Object> resultData = result.getData();
        assertThat(resultData.get("id")).isEqualTo(1);
        assertThat(resultData.get("name")).isEqualTo("테스트");
        assertThat(resultData.get("status")).isEqualTo("활성"); // 값 변환 안됨
        assertThat(resultData.get("migrated_at")).isInstanceOf(LocalDateTime.class);
    }

    @Test
    @DisplayName("데이터 검증 실패 테스트")
    void testDataValidationFailure() throws Exception {
        // Given - 빈 데이터
        testRecord.setTableName("사용자");
        testRecord.setData(new HashMap<>());

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNull(); // 검증 실패 시 null 반환
    }

    @Test
    @DisplayName("Boolean 타입 변환 테스트")
    void testBooleanTypeConversion() throws Exception {
        // Given
        testData.put("사용자ID", 1);
        testData.put("이름", "테스트사용자");
        testData.put("활성여부", Boolean.TRUE);
        
        testRecord.setTableName("사용자");
        testRecord.setData(testData);

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNotNull();
        Map<String, Object> resultData = result.getData();
        assertThat(resultData.get("is_active")).isEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("처리 통계 테스트")
    void testProcessingStats() throws Exception {
        // Given
        testData.put("사용자ID", 1);
        testData.put("이름", "테스트");
        testRecord.setTableName("사용자");
        testRecord.setData(testData);

        // When
        processor.process(testRecord);
        processor.process(testRecord);
        
        // 실패 케이스
        DataRecord failRecord = new DataRecord();
        failRecord.setTableName("사용자");
        failRecord.setData(new HashMap<>());
        processor.process(failRecord);

        // Then
        String stats = processor.getProcessingStats();
        assertThat(stats).contains("Processed: 3");
        assertThat(stats).contains("Errors: 1");
    }

    @Test
    @DisplayName("여러 상태값 변환 테스트")
    void testMultipleStatusValueTransformation() throws Exception {
        // Given - 문의 테이블
        testData.put("문의ID", 1);
        testData.put("사용자ID", 1);
        testData.put("문의유형", "상품문의");
        testData.put("제목", "상품 문의드립니다");
        testData.put("처리상태", "접수");
        
        testRecord.setTableName("문의");
        testRecord.setData(testData);

        // When
        DataRecord result = processor.process(testRecord);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTableName()).isEqualTo("inquiries");
        
        Map<String, Object> resultData = result.getData();
        assertThat(resultData.get("inquiry_type")).isEqualTo("PRODUCT_INQUIRY");
        assertThat(resultData.get("status")).isEqualTo("RECEIVED");
    }
}
