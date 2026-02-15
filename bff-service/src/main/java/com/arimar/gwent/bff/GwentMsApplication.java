package com.arimar.gwent.bff;

import com.arimar.gwent.communication.config.CommonsCommunicationAutoConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportAutoConfiguration(CommonsCommunicationAutoConfig.class)
public class GwentMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GwentMsApplication.class, args);
	}

}
