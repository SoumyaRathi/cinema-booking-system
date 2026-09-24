package com.movie.movieticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.movie.movieticket")
@EnableJpaRepositories(basePackages = "com.movie.movieticket.repository")
@EntityScan(basePackages = "com.movie.movieticket.model")
@EnableScheduling
// @EnableJdbcHttpSession - REMOVED FOR SAFETY (using default HTTP sessions)
public class MovieticketApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MovieticketApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(MovieticketApplication.class, args);
    }
}