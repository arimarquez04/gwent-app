package com.arimar.gwent.bff.config.docs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Gwent BFF")
                        .description("Service to BFF operations in Gwent")
                        .version("1.0.0")
                        .contact(new Contact().name("Ariel Marquez")
                                .email("arimarquez2444@gmail.com")
                                .url("https://github.com/arimarquez2444")
                        )
                        .license(new License().name("Apache 2.0")
                                .url("http://springdoc.org")
                        )
                );
    }
}