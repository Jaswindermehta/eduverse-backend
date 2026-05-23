# Eduverse - Production-Grade AI-Powered Online Course Marketplace Backend

Eduverse is a production-grade, highly scalable, and secure Spring Boot backend engine designed for an AI-powered online course marketplace platform similar to Udemy or Coursera. Built with a focus on startup engineering principles, the system demonstrates high performance, loose coupling, robust thread management, and multi-strategy cloud storage interfaces while remaining extremely clean, readable, and developer-friendly.

---

## 🚀 Project Overview

Eduverse serves as the foundational backbone for an online educational hub. It addresses the critical requirements of modern backend engineering: high-throughput API endpoints, strict role-based access controls, robust transactional boundary constraints, real-time background workflows, resilient asynchronous notifications, and modular cloud storage interfaces.

### Key Objectives
* **Technical Excellence**: Showcasing enterprise-grade Spring Boot best practices (dependency injection, strategy patterns, event multicasting, aspect-oriented concerns).
* **High Performance**: Transitioning slow blocking network queries (such as S3 and SMTP calls) to background worker threads, improving active API throughput.
* **Security First**: Implementing stateful defenses via custom JWT filters, BCrypt hashing, strict file validations, and rate-limiting structures.
* **Developer Experience**: Pluggable storage profiles and local fallback mechanisms that allow the entire stack to run locally on a developer's machine without external cloud dependencies.

---

## 🏗️ Architectural Topology

The system uses a highly optimized **layered monolithic architecture** that decouples concerns cleanly between layers:

```
[ Web / Mobile Clients ]
          │ (HTTPS REST API Requests with Bearer JWT)
          ▼
[ Tomcat HTTP Thread Pool ]
          │
[ Spring Security Filter Chain ] ──(Invalid Token)──► [ 401 Unauthorized Response ]
          │ (Validates JWT, Sets AuthenticationContext)
          ▼
[ API Controller Layer ] ────────(Validates DTOs)──► [ Global Exception Handler ]
          │
[ Business Service Layer ] ◄───► [ Pluggable Cloud/Local Storage Strategies ]
          │ (Transaction boundaries, Business Logic)
          ├─────────────────────────────────────────┐
          ▼ (JPA Fetch Joins / Lazy Loading)        ▼ (Loose Coupling via Events)
[ JPA Repository Layer ]                 [ Application Event Publisher ]
          │                                         │
          ▼ (Indexes: Email, Title, FKs)            ▼ (Async Dispatchers)
[ PostgreSQL Database ]                  [ Background Worker Thread Pool ]
                                                    │
                                                    ▼
                                         [ SMTP / Notification Pipelines ]
```

> [!NOTE]
> For a full suite of **10 detailed system architecture diagrams** (covering ER schemas, token lifecycles, file validation chains, thread metrics schedulers, and more), please refer to the comprehensive [System Architecture Document](file:///Users/yogeshberwal/.gemini/antigravity/scratch/eduverse/docs/architecture.md).

---

## 🛠️ Technology Stack

Eduverse is built on a modern, robust, and highly popular enterprise stack:

* **Core Language**: Java 17 (LTS)
* **Framework**: Spring Boot 3.2.x (Spring MVC, Spring Security, Spring Mail, Async)
* **Security & Auth**: JWT (JSON Web Tokens - `io.jsonwebtoken` v0.11.5), BCrypt Hashing
* **Database & Persistence**: PostgreSQL (v14+), Hibernate / JPA (Spring Data JPA)
* **Build & Dependency Management**: Apache Maven (v3.8+)
* **API Documentation**: Swagger / OpenAPI 3 (SpringDoc OpenApi UI v2.5.0)
* **Cloud Storage**: AWS S3 SDK for Java 2.x (`software.amazon.awssdk:s3` v2.25.27)
* **Testing Suite**: JUnit 5, Mockito, Spring Security Test
* **Utilities**: Lombok (v1.18.36)

---

## ⚡ Core Features

### 1. Robust Security & Identity Engine
* **JWT Stateless Auth**: Intercepts and parses authorization headers via a custom `JwtFilter`. Tokens contain distinct claims (Subject, Email, Custom Role Claim) with a configurable 24-hour expiration.
* **BCrypt Hashing**: Raw user passwords are salted and hashed using standard BCrypt before storage.
* **Role-Based Access Controls (RBAC)**: Custom method security rules restrict actions using roles (`ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMIN`).

### 2. Advanced Course & Enrollment System
* **Optimized Queries**: Solves Hibernate's infamous $N+1$ select problem using custom JPQL `JOIN FETCH` queries.
* **Soft Delete Support**: Protects course integrity and historical student enrollments using soft-deletes (`active` flag).
* **Database Indexing**: Tailored performance optimization via target indexes on columns `email`, `instructor_id`, `category_id`, `student_id`, and course `title`.
* **Rich Aggregate Statistics**: Real-time aggregation of reviews, dynamic computing of average ratings, and total student counts mapped directly to clean Response DTOs.

### 3. Asynchronous & Resilient Notification Infrastructure
* **Blocking Call Offloading**: Dispatches SMTP email and alert dispatches to a dedicated `ThreadPoolTaskExecutor`.
* **Observer Strategy**: Uses Spring's `ApplicationEvent` framework (`EnrollmentCreatedEvent`, `ReviewCreatedEvent`) to loosely couple services.
* **Resilient Fallback**: Automatically reverts to rich console logging if SMTP servers go down or credentials are unconfigured.

### 4. Pluggable Cloud Storage Pipeline
* **Strategy Design Pattern**: Exposes a common `FileStorageService` interface implemented by `S3StorageService` (AWS S3) and `LocalStorageService` (local server disk directory fallback).
* **Dynamic S3 Presigned URLs**: Securely generates expirable links directly from S3 without exposing bucket credentials.
* **File Protection Guard**: Validates file structures (file sizes, allowed extensions, MIME-type spoofing defenses) and incorporates rate-limiting protections.

---

## ⚙️ Step-by-Step Setup & Configuration

### Prerequisites
Before starting, ensure you have the following installed:
* **Java Development Kit (JDK) 17**
* **Apache Maven 3.8+**
* **PostgreSQL 14+**
* An IDE (IntelliJ IDEA, VS Code, or Eclipse)

---

### 1. PostgreSQL Database Configuration

1. Connect to your local PostgreSQL instance:
   ```bash
   psql -U postgres
   ```
2. Create the target database:
   ```sql
   CREATE DATABASE eduverse;
   ```
3. (Optional) Verify the database exists:
   ```sql
   \l
   ```
   
*Note: Hibernate will automatically scan the project entities on startup and construct the entire relational schema (tables, foreign keys, indexes) because `spring.jpa.hibernate.ddl-auto=update` is configured in `application.properties`.*

---

### 2. Environment Configurations

Navigate to `src/main/resources/application.properties`. Review and modify settings as required:

```properties
# PostgreSQL Settings
spring.datasource.url=jdbc:postgresql://localhost:5432/eduverse
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT Configurations (Base64 Encoded Secret Key)
eduverse.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
eduverse.jwt.expiration=86400000

# AWS S3 Integration
aws.s3.enabled=false
aws.s3.bucket=eduverse-marketplace-bucket
aws.s3.region=us-east-1
aws.accessKeyId=YOUR_ACCESS_KEY
aws.secretKey=YOUR_SECRET_KEY

# SMTP Mail Settings
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=noreply.eduverse@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
```

---

### 3. AWS S3 Integration Setup (Cloud Strategy)

To enable production cloud storage:
1. Create a private bucket in your AWS Console (e.g., `eduverse-marketplace-bucket`).
2. Create an IAM User with `AmazonS3FullAccess` and generate Access/Secret keys.
3. Update `application.properties`:
   * Set `aws.s3.enabled=true`
   * Populate `aws.accessKeyId`, `aws.secretKey`, and `aws.s3.bucket` keys.
4. If `aws.s3.enabled` is `false`, the backend immediately creates an `/uploads` directory in your project root and processes local writes securely.

---

### 4. SMTP Configuration (Asynchronous Emails)

Eduverse sends real-time registration confirmations and purchase receipts:
1. We use standard SMTP. For Gmail, navigate to Google Accounts -> Security -> Enable 2-Step Verification -> App Passwords.
2. Generate an app password for "Mail" and "Other".
3. Update `application.properties` by injecting the username and the generated 16-character app password.
4. *Dev Fallback*: If the SMTP credentials are left at default values (`YOUR_APP_PASSWORD`), the async listener intercepts the `MailException` gracefully and outputs a formatted HTML preview to the console log, maintaining local developer testing efficiency.

---

## 🚀 Running the Application

Run the application using Maven from the project root:

```bash
mvn spring-boot:run
```

Once started successfully, you should see the following console line:
```text
[INFO] com.eduverse.EduverseApplication - Started EduverseApplication in X.XXX seconds
```

### Automatic Seed Data
On initial boot, the database is auto-seeded via `com.eduverse.config.DatabaseSeeder` with:
* **Roles**: `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMIN`
* **Default Users**:
  * Admin: `admin@eduverse.com` (Password: `admin123`)
  * Instructor: `instructor@eduverse.com` (Password: `instructor123`)
  * Student: `student@eduverse.com` (Password: `student123`)
* **Default Course Categories**: `Software Development`, `Data Science`, `Business`, `Design`.

---

## 📖 Swagger API Playground

SpringDoc OpenAPI is integrated natively, allowing interactive endpoint testing from your web browser.

* **Swagger UI URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **API Documentation JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### How to use Swagger with JWT Authorization:
1. Open the **Swagger UI** in your browser.
2. Scroll to the authentication endpoint: `/api/auth/login`.
3. Submit a POST request using the seeded credentials:
   ```json
   {
     "email": "student@eduverse.com",
     "password": "student123"
   }
   ```
4. Copy the JWT token string returned inside the response body DTO:
   ```json
   {
     "success": true,
     "message": "Login successful",
     "data": {
       "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdHVkZW50QGVkdXZlcnNlLmNvbSI..."
     }
   }
   ```
5. Scroll to the top of the Swagger page and click the green **Authorize** button.
6. Enter `Bearer <YOUR_COPIED_TOKEN>` in the dialog input field (include the space between `Bearer` and the token value).
7. Click **Authorize** and then **Close**. All secured endpoints (like enrollment, adding reviews, or uploading thumbnails) can now be invoked directly through the browser.

---

## 🧪 Postman Integration Guide

To import and execute API collections in Postman:

1. **Create an Environment**: Create a new environment in Postman and add a variable named `base_url` set to `http://localhost:8080`.
2. **Handle Dynamic Auth**:
   * Add a `token` variable in your environment.
   * Under your Login Request's **Tests** tab, add a script to capture the token dynamically:
     ```javascript
     const response = pm.response.json();
     if (response.success && response.data && response.data.token) {
         pm.environment.set("token", response.data.token);
     }
     ```
   * Set your collections or individual secure requests to use **Bearer Token** auth, referencing your environment variable: `{{token}}`.

---

## 💻 REST API Direct curl Examples

### 1. Register a New Student (Public Endpoint)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newstudent@gmail.com",
    "password": "SecurePassword123",
    "fullName": "John Doe",
    "role": "STUDENT"
  }'
```

### 2. Authenticate / Login (Public Endpoint)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "instructor@eduverse.com",
    "password": "instructor123"
  }'
```

### 3. Create a Course (Secured - Requires ROLE_INSTRUCTOR)
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer <INJECT_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Mastering Advanced Spring Boot 3.x",
    "description": "Learn event driven architecture, async worker pools, and advanced JPA.",
    "price": 99.99,
    "categoryId": 1
  }'
```

### 4. Fetch All Active Courses (Public Endpoint - paginated & sorted)
```bash
curl -X GET "http://localhost:8080/api/courses?page=0&size=5&sortBy=title&direction=ASC"
```

---

## 🎨 Visual Assets & Screenshots

*Below are structural layout references showing where to capture visual proof points for portfolio presentations:*

#### 1. Interactive Swagger UI Documentation Dashboard
![Swagger Playground Documentation Placeholder](https://raw.githubusercontent.com/antigravity-ai/placeholders/main/eduverse-swagger.png)
*An illustration of the complete, self-documenting OpenAPI schemas and role-based test fields available at `/swagger-ui.html`.*

#### 2. Clean Database Entity Relational Model (ERD) Schema
![Database ERD Schema Layout](https://raw.githubusercontent.com/antigravity-ai/placeholders/main/eduverse-erd.png)
*Detailed visual layout of relational foreign key connections mapping users, roles, categories, courses, enrollments, reviews, and logs in PostgreSQL.*

#### 3. High-Throughput Thread Pool Observability Console Logs
```text
2026-05-22 15:30:15 [EduTask-Metrics-1] INFO  c.e.c.AsyncMetricsConfig - [ASYNC MONITOR] Pool Active Threads: 3, Queue Tasks: 12, Pool Size: 5, Max Size: 15
2026-05-22 15:30:18 [EduTask-2] DEBUG c.e.s.AsyncEmailService - [MAIL DISPATCH] Sending rich HTML receipt to student@eduverse.com
```

---

## 🧪 Testing Instructions

Eduverse includes a comprehensive unit and integration test suite targeting critical paths like authentication, token lifecycle validation, role guarding, and core course enrollments.

### Running All Tests
To trigger the testing suite via Maven:
```bash
mvn test
```

### Key Test Components
* **JUnit 5**: Manages assertions, test execution lifecycle, and mock contexts.
* **Mockito**: Isolates and mocks service logic, database records, and third-party network APIs (S3/SMTP) to avoid side effects during test suites.
* **MockMvc**: Simulates HTTP calls inside a mock Spring MVC environment, validating JWT filter behaviors and controller response payloads without booting the full Tomcat server.

---

## 🌐 Cloud Deployment Architecture

For professional production deployments, we recommend a decoupled, cloud-native strategy:

1. **Application Containerization**: Pack the application using a multi-stage Dockerfile containing a minimal Eclipse Temurin JRE footprint.
2. **Orchestration**: Deploy to **AWS ECS (Fargate)** or a **Kubernetes (EKS)** cluster. Set task execution limits to scale container instances horizontally behind an **AWS Application Load Balancer (ALB)**.
3. **Database Layer**: Provision an **AWS RDS PostgreSQL** multi-AZ (Availability Zone) instance to ensure database high availability and automated failovers.
4. **Caching Subsystem**: Integrate **Amazon ElastiCache (Redis)** to cache course catalogue results and throttle malicious request streams.
5. **Static Assets & Uploads**: Point storage structures strictly to **AWS S3** and serve resources through **Amazon CloudFront** to edge-cache thumbnails and course resources globally.

---

## 🛡️ Key Security Measures

* **Custom Token Filters**: Employs an exact token filtering mechanism using the `OncePerRequestFilter` paradigm to prevent token parsing overheads on non-secured paths.
* **MIME-Type Spoofing Defenses**: File validator inspects the actual magic byte header of files via input streams instead of trusting user-supplied extensions.
* **SQL Injection Prevention**: Built entirely on Spring Data JPA repositories that use parameterized prepared statements under the hood.
* **Cross-Site Scripting (XSS) Defenses**: All string fields are disinfected, and API responses return clean, uninterpreted DTO payloads.

---

## 🎓 Key Engineering Learning Outcomes

Building or studying the Eduverse backend repository provides mastery over the following concepts:

1. **Strategic Thread Allocation**: Structuring a core queue system that prevents long network tasks (S3 uploads, email dispatches) from starving standard Tomcat web threads.
2. **JPA Query Optimization**: Solving the $N+1$ select problem using custom `JOIN FETCH` queries, and managing lazy initialization boundaries correctly.
3. **Stateful Security Architecture**: Building robust custom filter chains, understanding token generation lifecycles, and implementing granular role-based authorization rules.
4. **Decoupled Event Design (EDA)**: Structuring loose service bounds using transactional application events, making the system highly modular and scalable.
5. **Strategy Design Pattern**: Creating multi-strategy abstractions that decouple physical file operations from the business controllers.

---

## 📄 License & Showcase Information

Eduverse is licensed under the MIT License. It serves as a flagship, production-grade project designed to showcase advanced Spring Boot backend engineering, API security, and systems design expertise in professional portfolios, technical interviews, and engineering resume displays.

*Designed and engineered with care by the Eduverse Development Team.*
