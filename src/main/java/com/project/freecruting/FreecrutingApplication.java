package com.project.freecruting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
@EnableRedisHttpSession
public class FreecrutingApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreecrutingApplication.class, args);
	}
	//h2-console 에서 일단 설정 스킵함
}
