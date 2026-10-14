package com.freightfox.tollplazafinder.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI freightFoxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Freight Fox Toll Plaza Finder API")
                        .version("1.0.0")
                        .description("REST API for finding toll plazas along driving routes between Indian postal codes (pincodes).")
                        .contact(new Contact().name("Freight Fox")));
    }
}
