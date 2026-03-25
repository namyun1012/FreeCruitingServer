package com.project.freecruting.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * @Async 어노테이션 활성화
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 이벤트 처리용 Executor
     */
    @Bean(name = "taskExecutor") // ⭐ "taskExecutor"로 이름 변경
    @Primary
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);         // 기본 스레드 수
        executor.setMaxPoolSize(10);         // 최대 스레드 수
        executor.setQueueCapacity(100);      // 큐 용량
        executor.setThreadNamePrefix("event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

}
