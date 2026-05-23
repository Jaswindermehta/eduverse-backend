package com.eduverse.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * ASYNCHRONOUS RESILIENT EMAIL SERVICE
 * ============================================================================
 * 
 * Offloads long-running SMTP connection overhead entirely to the background 
 * worker thread pool (`@Async("taskExecutor")`), ensuring instant API response times.
 * 
 * RESILIENCY tactics:
 * - **Exponential Backoff Retries**: Attempts email delivery multiple times with
 *   exponentially increasing delays to survive transient network or SMTP disruptions.
 * - **Developer-Friendly Log Fallback**: If SMTP credentials are dummy, offline, or
 *   the mail server goes down, the HTML email is formatted and dumped directly to 
 *   the console using our logger. The transaction does NOT crash!
 */
@Service
public class AsyncEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply.eduverse@gmail.com}")
    private String fromEmail;

    public AsyncEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Dispatches rich HTML emails on a background thread.
     * Implements thread-safe exponential backoff retry algorithms.
     */
    @Async("taskExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        logger.info("[ASYNC EMAIL] Initiating email task to: '{}', Subject: '{}' on Thread: '{}'", 
                to, subject, Thread.currentThread().getName());

        int maxAttempts = 3;
        long initialBackoffMs = 500;
        double multiplier = 2.0;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // If developer did not configure a real mail password, skip sending and force console fallback
                if (fromEmail.equals("YOUR_APP_PASSWORD") || fromEmail.contains("YOUR_")) {
                    throw new RuntimeException("Developer has not configured real SMTP credentials.");
                }

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true); // true indicates HTML content format

                mailSender.send(message);
                
                logger.info("[ASYNC EMAIL] SUCCESS: Email successfully delivered to '{}' on attempt #{}", to, attempt);
                return; // Return immediately on success!
            } catch (Exception ex) {
                lastException = ex;
                logger.warn("[ASYNC EMAIL] WARNING: Attempt #{} failed to deliver email to '{}': {}. Retrying in background...", 
                        attempt, to, ex.getMessage());
                
                if (attempt < maxAttempts) {
                    long backoffMs = (long) (initialBackoffMs * Math.pow(multiplier, attempt - 1));
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("[ASYNC EMAIL] Thread interrupted during backoff sleep!", e);
                        return;
                    }
                }
            }
        }

        // ============================================================================
        // DEVELOPER LOG FALLBACK (SMTP OFFLINE/DUMMY FALLBACK)
        // ============================================================================
        logger.error("[ASYNC EMAIL] CRITICAL: SMTP delivery failed to '{}' after {} attempts. Triggering dev fallback console dump.", 
                to, maxAttempts, lastException);
        
        System.out.println("====================================================================================================");
        System.out.println("                     [DEVELOPER OFFLINE EMAIL FALLBACK SIMULATOR]                                  ");
        System.out.println("====================================================================================================");
        System.out.println("To:      " + to);
        System.out.println("From:    " + fromEmail);
        System.out.println("Subject: " + subject);
        System.out.println("---------------------------------------- HTML CONTENT ----------------------------------------------");
        System.out.println(htmlContent);
        System.out.println("====================================================================================================");
    }
}
