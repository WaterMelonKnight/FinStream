package io.finstream;

import io.finstream.config.FinStreamProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FinStreamProperties.class)
public class FinStreamApplication {
    public static void main(String[] args) { SpringApplication.run(FinStreamApplication.class, args); }
}
