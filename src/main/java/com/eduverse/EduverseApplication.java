package com.eduverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ============================================================================
 * EDUVERSE APPLICATION MAIN ENTRY POINT
 * ============================================================================
 * 
 * This is the starting point of our entire backend application. When we run this
 * class, Spring Boot will boot up, start its internal Tomcat web server, scan our
 * codebase for annotations (like controllers and services), set up our PostgreSQL
 * database connection, and make our REST API ready to receive web traffic.
 * 
 * We use two key class-level annotations here:
 * 
 * 1. @SpringBootApplication: Tells the framework that this is a Spring Boot
 *    application. It automatically reads settings, performs package-scanning, 
 *    and runs auto-configurations.
 * 
 * 2. @EnableAsync: Instructs Spring to enable background/asynchronous processing.
 *    Any service methods we label with @Async will run in the background without
 *    blocking our main web threads (extremely useful for sending emails!).
 */
@SpringBootApplication
@EnableAsync // Enables asynchronous processing across our application
public class EduverseApplication {

    public static void main(String[] args) {
        // Starts the Spring Boot framework container
        SpringApplication.run(EduverseApplication.class, args);
    }
}
