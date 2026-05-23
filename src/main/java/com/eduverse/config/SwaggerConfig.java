package com.eduverse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * SWAGGER / OPENAPI 3 CONFIGURATION
 * ============================================================================
 * 
 * This class configures OpenAPI 3 settings for auto-generating our backend API
 * documentation and the interactive Swagger UI testing panel.
 * 
 * To test JWT-protected APIs, we declare a "Bearer Token" security scheme.
 * This adds an "Authorize" button to the Swagger UI.
 * 
 * Once built, you can access the interactive visual interface in your browser at:
 * http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configures the OpenAPI system layout.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Define the name of our security scheme (can be any unique key name)
        final String securitySchemeName = "BearerTokenAuth";

        return new OpenAPI()
                // 1. Define API Metadata (Title, Description, version)
                .info(new Info()
                        .title("Eduverse API Documentation")
                        .version("1.0.0")
                        .description("Production-grade RESTful APIs for the Eduverse online course marketplace platform. " +
                                "Contains user management, course creations, enrollments, reviews, and S3 file uploading.")
                        .contact(new Contact()
                                .name("Eduverse Developer Team")
                                .email("support@eduverse.com")))
                
                // 2. Wire in security requirements globally.
                //    This tells Swagger to apply our security scheme to all documented API routes.
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                
                // 3. Define the JWT Bearer Security Scheme structure in Swagger
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Please paste your JWT Token (obtained from /api/auth/login) in the format: <JWT_STRING>")));
    }
}
