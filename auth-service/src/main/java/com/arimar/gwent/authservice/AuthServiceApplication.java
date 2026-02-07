package com.arimar.gwent.authservice;

import com.arimar.gwent.authservice.config.security.JWTConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties(JWTConfigurationProperties.class)
@EnableJpaRepositories(basePackages = "com.arimar.gwent.authservice.repository")
@EntityScan(basePackages = "com.arimar.gwent.domain")

public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
