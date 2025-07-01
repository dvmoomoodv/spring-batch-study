package com.example.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 배치 설정 프로퍼티
 * application.yml의 batch 설정을 바인딩
 */
@Component
@ConfigurationProperties(prefix = "batch")
public class BatchProperties {

    private int chunkSize = 1000;
    private int skipLimit = 100;
    private int retryLimit = 3;

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getSkipLimit() {
        return skipLimit;
    }

    public void setSkipLimit(int skipLimit) {
        this.skipLimit = skipLimit;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    @Override
    public String toString() {
        return "BatchProperties{" +
                "chunkSize=" + chunkSize +
                ", skipLimit=" + skipLimit +
                ", retryLimit=" + retryLimit +
                '}';
    }
}
