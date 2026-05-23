# Eduverse Backend System Architecture Documentation

Welcome to the **Eduverse Production-Grade Architecture Documentation**. This document contains a comprehensive breakdown of the architectural designs, database mappings, and process lifecycles of the Eduverse platform. It features **10 detailed Mermaid diagrams** mapping every corner of the system.

---

## 1. System Architecture Overview

Eduverse follows a clean, modular **layered monolithic architecture** built on top of the **Spring Boot** framework. The system is designed to be highly secure, resilient, and horizontally scalable. It balances ease of development with high-performance operational components such as multi-strategy cloud storage, thread-pool offloading, and decoupled event multicasting.

### Layered Structure
* **API Controller Layer**: Exposes secure REST endpoints, consumes/validates DTOs, and handles HTTP response wrapping.
* **Service/Business Logic Layer**: Enforces transactional constraints (`@Transactional`) and implements business rules.
* **Data Access (Repository) Layer**: Interfaces with PostgreSQL using Spring Data JPA, optimized with custom fetch joins to prevent the N+1 select problem.
* **Infrastructure/Security Layer**: Intercepts requests using Spring Security filters, validates JWT tokens, rates limits uploads, and hosts async task execution thread pools.

---

## 2. All 10 Production-Grade System Diagrams

### Diagram 1: Complete System Architecture (High-Level overview)
This diagram maps how clients traverse firewalls, auth layers, HTTP thread managers, and delegate slow operations to background workers.

```mermaid
graph TD
    Client[Web / Mobile Clients] -->|HTTPS REST Request| GW[Tomcat HTTP Thread Pool]
    
    subgraph Spring Boot Application Layer
        GW -->|JWT Filter Auth| Security[Spring Security Filter Chain]
        Security -->|Allow / Deny| Controller[REST Controller Layer]
        Controller -->|DTO Validation| Service[Service / Business Layer]
        
        Service -->|Data Mapping| DB_Access[JPA Repository Layer]
        Service -->|Event Publish| Events[Application Event Publisher]
    end
    
    subgraph Data & Storage Layer
        DB_Access -->|Optimized Queries| PostgreSQL[(PostgreSQL Database)]
        Service -->|Storage Strategy| Storage{Active Storage Strategy}
        Storage -->|LOCAL| Disk[(Local Server Disk /uploads)]
        Storage -->|S3| S3Bucket[(AWS S3 Bucket)]
    end
    
    subgraph Async & Notification Workers
        Events -->|Decoupled Event| Listener[Async Event Listeners]
        Listener -->|ThreadPoolTaskExecutor| ExecutorPool[Worker Thread Pool]
        ExecutorPool -->|Send Async Email| SMTP[External SMTP Server]
        ExecutorPool -->|Write DB Alert| PostgreSQL
    end
```

---

### Diagram 2: Database ER Diagram (PostgreSQL Schema)
Maps all primary tables, fields, data types, indexes, and relational cardinalities.

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password
        VARCHAR full_name
        BIGINT role_id FK
        BOOLEAN enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    roles {
        BIGINT id PK
        VARCHAR name UK
    }
    categories {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR description
    }
    courses {
        BIGINT id PK
        VARCHAR title
        VARCHAR description
        BIGINT instructor_id FK
        BIGINT category_id FK
        BOOLEAN active
        DOUBLE_PRECISION price
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    course_contents {
        BIGINT id PK
        VARCHAR title
        VARCHAR video_url
        VARCHAR resource_url
        BIGINT course_id FK
        INTEGER sequence_order
    }
    enrollments {
        BIGINT id PK
        BIGINT student_id FK
        BIGINT course_id FK
        TIMESTAMP enrolled_at
    }
    reviews {
        BIGINT id PK
        BIGINT student_id FK
        BIGINT course_id FK
        INTEGER rating
        VARCHAR comment
        TIMESTAMP created_at
    }
    notifications {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR title
        VARCHAR message
        BOOLEAN is_read
        TIMESTAMP created_at
    }
    upload_metadata {
        BIGINT id PK
        VARCHAR file_url
        VARCHAR original_file_name
        VARCHAR content_type
        BIGINT file_size
        VARCHAR storage_strategy
        BIGINT uploaded_by_id FK
        TIMESTAMP created_at
    }

    users ||--o| roles : "has role"
    courses ||--o| users : "taught by instructor"
    courses ||--o| categories : "belongs to"
    course_contents ||--o| courses : "belongs to"
    enrollments ||--o| users : "enrolled student"
    enrollments ||--o| courses : "course link"
    reviews ||--o| users : "written by student"
    reviews ||--o| courses : "reviewed course"
    notifications ||--o| users : "directed to recipient"
    upload_metadata ||--o| users : "uploaded by user"
```

---

### Diagram 3: Full Backend HTTP Request-Response Flow
Shows the lifecycle of a request entering the system, passing filters, executing services, and returning a DTO-only API Response.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Tomcat as Tomcat Thread Pool
    participant JwtFilter as JwtFilter & Spring Security
    participant Controller as REST Controller
    participant Service as Service Layer
    participant Repos as JPA Repository
    participant DB as PostgreSQL

    Client->>Tomcat: Send HTTP request (e.g. POST /api/courses) with Bearer JWT
    Tomcat->>JwtFilter: Intercept request
    JwtFilter->>JwtFilter: Extract token & validate signature/expiration
    Note over JwtFilter: Set Authentication in SecurityContext
    JwtFilter->>Controller: Forward request
    
    Controller->>Controller: Run DTO @Valid constraints
    Controller->>Service: Pass DTO (e.g. CourseCreateRequest)
    
    Service->>Service: Verify permissions (Instructor/Admin)
    Service->>Repos: Invoke query methods
    Repos->>DB: Execute SELECT/INSERT
    DB-->>Repos: Return database records
    Repos-->>Service: Return entity objects
    
    Service->>Service: Map Entities to clean Response DTOs
    Service-->>Controller: Return Response DTO
    Controller-->>Tomcat: Wrap in ApiResponse.success()
    Tomcat-->>Client: HTTP 200/201 OK JSON Payload
```

---

### Diagram 4: Security Architecture Topology
Illustrates how endpoints are split between public access bypass pathways and role-protected JWT filters.

```mermaid
graph TD
    Request[Incoming API Request] --> RouteCheck{Matches Path Rules?}
    
    RouteCheck -->|"/api/auth/**"<br>"/uploads/**"<br>"/swagger-ui.html"| Public[Bypass JWT Authentication]
    RouteCheck -->|"/api/courses/**" GET<br>"/api/categories/**" GET| Public
    
    RouteCheck -->|"/api/courses" POST<br>"/api/notifications/**"<br>"/api/uploads"| Secured[JwtFilter Interception]
    
    Public --> ExecuteEndpoint[Execute Controller Endpoint]
    
    Secured --> TokenCheck{Valid JWT Header?}
    TokenCheck -->|No / Expired| AuthError[Throw 401 Unauthorized]
    TokenCheck -->|Yes| LoadSecurity[Load User Roles into Context]
    
    LoadSecurity --> RoleCheck{Has Required Role?}
    RoleCheck -->|No| AccessError[Throw 403 Forbidden]
    RoleCheck -->|Yes| ExecuteEndpoint
```

---

### Diagram 5: JWT Authentication Lifecycle
Maps the step-by-step token generation during registration/login and downstream verification on subsequent client requests.

```mermaid
graph TD
    subgraph Step 1: Token Issuance (Login)
        A[Client POST /api/auth/login] --> B[AuthenticationManager verifies BCrypt hash]
        B -->|Success| C[JwtService generates token claims]
        C -->|Payload| D[Inject sub: Username, role: RoleName, exp: 24h]
        D -->|Sign| E[Secure sign using HMAC-SHA256 & JWT Secret]
        E -->|Return| F[HTTP 200 with JWT Token DTO]
    end
    
    subgraph Step 2: Client Storage
        F -->|Client saves| G[Session/Local Storage]
    end
    
    subgraph Step 3: Token Verification (Authorized Request)
        G -->|Subsequent call| H[Inject Authorization: Bearer TOKEN header]
        H --> I[JwtFilter intercepts request]
        I --> J{Valid Signature & Expiration?}
        J -->|Invalid| K[Return 401 Unauthorized Response]
        J -->|Valid| L[Extract sub & roles]
        L --> M[Populate UsernamePasswordAuthenticationToken in SecurityContext]
        M --> N[Forward to downstream REST controller]
    end
```

---

### Diagram 6: Async Processing Architecture
Details how long-running blocking processes are decoupled using thread allocation.

```mermaid
graph TD
    A[Tomcat Web Thread] -->|Invokes @Async method| B{Task Queue Capacity?}
    
    subgraph ThreadPoolTaskExecutor
        B -->|Under 100 queued items| C[Accept task and park in Queue]
        B -->|Queue full but under 15 threads| D[Spawn temporary worker thread]
        B -->|Saturated Core & Queue & Max| E[Trigger RejectedExecutionHandler CallerRunsPolicy]
        
        C --> F[Core Background Threads 1..5 execute task]
        D --> G[Max Background Threads 6..15 execute task]
    end
    
    E -->|Slow down| H[Tomcat Web Thread executes task itself]
    F --> I[Job completes successfully]
    G --> I
```

---

### Diagram 7: Event-Driven Architecture (EDA) Lifecycle
Maps how business actions publish immutable events, allowing listeners to execute detached background dispatches.

```mermaid
graph TD
    subgraph Publisher Context
        A[Student purchases course] --> B[EnrollmentService registers enrollment in DB]
        B --> C[Publish EnrollmentCreatedEvent to ApplicationEventPublisher]
        C --> D[Return instant HTTP 200 EnrollmentResponse DTO to student]
    end
    
    subgraph Event Multicaster Engine
        C --> E[Spring Event Multicaster]
    end
    
    subgraph Background Listener Context
        E -->|Multicast| F[EmailEventListener]
        E -->|Multicast| G[NotificationEventListener]
        
        F -->|@Async taskExecutor| H[Render HTML Template & Trigger AsyncEmailService]
        G -->|@Async taskExecutor| I[Persist DB-backed alert Notification entity]
    end
    
    subgraph External Infrastructure
        H -->|SMTP| J[Student Welcome Inbox]
        H -->|SMTP Fallback| K[Dump rich HTML body to System SLF4J Logs]
        I -->|JPA Save| L[(PostgreSQL Notifications Table)]
    end
```

---

### Diagram 8: File Upload Security Pipeline
Details the multi-stage defense gates that incoming multipart streams pass through before storage execution is allowed.

```mermaid
graph TD
    A[Client multipart/form-data payload] --> B[RateLimiter Check]
    B -->|Exceeded limit| C[Throw 429 Too Many Requests]
    B -->|Allowed| D[FileSecurityScanner Stream Check]
    
    D -->|Blocked filenames / EICAR signature| E[Throw SecurityException -> 400 Bad Request]
    D -->|Clean| F[FileValidator Rules Check]
    
    F -->|Spoofed MIME / oversized / bad extension| G[Throw IllegalArgumentException -> 400 Bad Request]
    F -->|Validation Passed| H[Storage Strategy Selection]
```

---

### Diagram 9: Thread Pool & Async Execution Lifecycle
Visualizes thread lifespans, task queues, and pool monitoring scheduler configurations.

```mermaid
stateDiagram-v2
    [*] --> Idle: Thread Pool Initialized (Core Size = 5)
    
    state ThreadPoolTaskExecutor {
        Idle --> TaskSubmitted: Task arrives
        TaskSubmitted --> CoreWorkerAllocated: Active threads < 5
        TaskSubmitted --> QueueParked: Active threads >= 5
        
        QueueParked --> MaxWorkerSpawned: Queue hits 100 items & threads < 15
        QueueParked --> CoreWorkerAllocated: Previous task finishes
        
        MaxWorkerSpawned --> ActiveRunning: Background Thread runs task
        CoreWorkerAllocated --> ActiveRunning: Background Thread runs task
        
        ActiveRunning --> Idle: Task completes successfully
        ActiveRunning --> ActiveRunning: Retry loop on S3/SMTP failures
    }
    
    state ObservabilityMetricsScheduler {
        [*] --> TimerFired: Every 15 seconds
        TimerFired --> LogMetrics: Query ThreadPoolExecutor metrics
        LogMetrics --> [*]: Print active threads/queue to SLF4J logs
    }
```

---

### Diagram 10: Cloud Storage Strategy Design Pattern
Maps the abstract strategy pattern allowing pluggable local storage directory fallback alongside AWS S3 buckets.

```mermaid
classDiagram
    class FileStorageService {
        <<interface>>
        +storeFile(MultipartFile file, String folder) String
        +deleteFile(String fileUrl) void
        +generatePresignedUrl(String fileKey, int expiration) String
    }
    
    class LocalStorageService {
        -Path fileStorageLocation
        +storeFile(MultipartFile file, String folder) String
        +deleteFile(String fileUrl) void
        +generatePresignedUrl(String fileKey, int expiration) String
    }
    
    class S3StorageService {
        -S3Client s3Client
        -S3Presigner s3Presigner
        -String bucketName
        +storeFile(MultipartFile file, String folder) String
        +deleteFile(String fileUrl) void
        +generatePresignedUrl(String fileKey, int expiration) String
    }
    
    class S3Config {
        +s3Client() S3Client
        +s3Presigner() S3Presigner
        +fileStorageService() FileStorageService
    }
    
    class UploadController {
        -FileStorageService fileStorageService
        +uploadFile(MultipartFile file, FileType type, String folder) ResponseEntity
    }

    FileStorageService <|.. LocalStorageService : implements
    FileStorageService <|.. S3StorageService : implements
    S3Config --> FileStorageService : resolves @Primary Strategy
    UploadController --> FileStorageService : injects active strategy
```
