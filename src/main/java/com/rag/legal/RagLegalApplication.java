package com.rag.legal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.rag.legal"})
public class RagLegalApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagLegalApplication.class, args);
    }
}
