package com.project.freecruting.config.AWS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/*
 현재 미사용 중
 */
@Configuration
public class S3Config {

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Bean
    @ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }


}
