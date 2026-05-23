package com.eduverse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ============================================================================
 * ASYNCHRONOUS TASK EXECUTOR & MONITORING CONFIGURATION
 * ============================================================================
 * 
 * Enbles `@Async` background execution and `@Scheduled` system tasks.
 * 
 * ----------------------------------------------------------------------------
 * BEGINNER-FRIENDLY EXPLANATION: THREAD POOLS & ASYNC EXECUTION
 * ----------------------------------------------------------------------------
 * Think of Spring Boot as a busy restaurant:
 * - Tomcat HTTP threads are the "Front-of-House Waiters". Their only job is to
 *   greet customers (receive API requests), take orders, and quickly serve food.
 * - If a waiter has to personally cook the food, wash the dishes, and clean the
 *   kitchen (which takes minutes, like sending an email or uploading an S3 file),
 *   no other customers can be greeted. The restaurant gets clogged, and users see timeouts!
 * - **Async Execution** solves this by letting the waiter hand the slow task over to
 *   a "Kitchen Staff Member" (a background worker thread). The waiter immediately returns
 *   to serve the next customer, while the kitchen staff processes the task in the background.
 * - **Thread Pool (ThreadPoolTaskExecutor)** is like the "Kitchen Staff Pool". Instead of hiring
 *   and firing a worker every time an order comes in (which is extremely expensive!), we keep a 
 *   permanent set of workers (Core Pool) waiting for tasks. If they all get busy, incoming orders
 *   wait in a line (Queue). If the line fills up, we hire temporary workers (Max Pool) to help.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${async.executor.core-size:5}")
    private int corePoolSize;

    @Value("${async.executor.max-size:15}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.executor.thread-prefix:EduTask-}")
    private String threadNamePrefix;

    private ThreadPoolTaskExecutor taskExecutor;

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        logger.info("Initializing ThreadPoolTaskExecutor: Core Size = {}, Max Size = {}, Queue Capacity = {}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // Handles what happens when queue is full and all max threads are busy.
        // CallerRunsPolicy forces the thread that submitted the task to execute it, slowing down requests.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        this.taskExecutor = executor;
        return executor;
    }

    /**
     * ASYNC EXECUTOR MONITORING METRICS:
     * Periodically logs the active state of our worker pool to provide operational visibility.
     * Triggers every 15 seconds.
     */
    @Scheduled(fixedRate = 15000)
    public void logExecutorMetrics() {
        if (taskExecutor != null) {
            ThreadPoolExecutor tpe = taskExecutor.getThreadPoolExecutor();
            logger.info("[OBSERVABILITY - THREAD POOL METRICS] Active Threads: {}, Current Pool Size: {}, Queue Size: {}, Completed Tasks: {}",
                    tpe.getActiveCount(),
                    tpe.getPoolSize(),
                    tpe.getQueue().size(),
                    tpe.getCompletedTaskCount()
            );
        }
    }
}
