package com.example.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

/**
 * MSSQL에서 MariaDB로 데이터 이관을 위한 스프링 배치 애플리케이션
 * 
 * 실행 방법:
 * - 개발환경: java -jar batch-migration.jar --spring.profiles.active=dev
 * - 운영환경: java -jar batch-migration.jar --spring.profiles.active=prod
 * - 청크 사이즈 조절: --batch.chunk-size=500
 */
@SpringBootApplication
@EnableBatchProcessing
public class BatchApplication {

    public static void main(String[] args) {
        System.setProperty("spring.batch.job.enabled", "false");
        SpringApplication.run(BatchApplication.class, args);
    }
}
