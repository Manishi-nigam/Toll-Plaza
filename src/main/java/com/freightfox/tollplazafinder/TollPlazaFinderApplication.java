package com.freightfox.tollplazafinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TollPlazaFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TollPlazaFinderApplication.class, args);
    }
}
