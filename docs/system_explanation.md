# Eduverse Backend - Beginner-Friendly System Explanation Guide

Welcome! If you are new to backend engineering, database architectures, or Spring Boot, this guide is written specifically for you. We will explain how the entire **Eduverse** system works end-to-end using plain English, clear step-by-step descriptions, and friendly real-world analogies.

---

## 1. How API Requests Flow: The Journey of a Request

When you click a button on a web page or mobile app—like clicking "Enroll in Course"—it triggers an **API Request** (an HTTP message) that travels over the internet to our backend server. 

Here is the exact journey that request takes inside the Eduverse system:

```
[ Client Click ] 
       │
       ▼ (1)
[ Tomcat Server ] ──► (Allocates an HTTP Thread to handle your request)
       │
       ▼ (2)
[ Spring Security ] ──► (Asks: "Who are you? Do you have a valid JWT passport?")
       │
       ▼ (3)
[ REST Controller ] ──► (Validates your input DTOs. e.g., "Is the course ID valid?")
       │
       ▼ (4)
[ Service Layer ] ──► (Applies business rules. e.g., "Has this student already enrolled?")
       │
       ▼ (5)
[ Repository Layer ] ──► (Talks to PostgreSQL Database using optimized queries)
       │
       ▼ (6)
[ PostgreSQL DB ] ──► (Saves enrollment, returns success confirmation)
       │
       ▼ (7)
[ Client Screen ] ◄── (HTTP 200 OK Response wrapped in a clean JSON envelope)
```

1. **The Arrival (Tomcat Server)**: The request hits our embedded **Tomcat Web Server**. Tomcat acts like a traffic controller, allocating a single worker thread to guide your request.
2. **The Security Guard (Spring Security)**: The request passes through our security gates. The guard checks if you have a valid security passport (a **JWT Token**). If you are attempting an restricted action (like creating a course), the guard verifies your role (e.g., making sure you are an `INSTRUCTOR` and not just a `STUDENT`).
3. **The Reception Desk (Controller Layer)**: The request arrives at the REST Controller. The controller reads your input data (which we pack into a secure data envelope called a **DTO**) and verifies it. If you forgot a field, it sends back a polite error immediately.
4. **The Brain (Service Layer)**: The request moves into the Service Layer. This is where all the decision-making happens. It applies business logic, validates permissions, and initiates database transactions.
5. **The Translator (Repository Layer)**: The service layer asks the Repository Layer to write to the database. The repository acts as a translator, converting Java commands into SQL database queries.
6. **The Filing Cabinet (PostgreSQL Database)**: The database securely writes the new enrollment record to the physical disk.
7. **The Return Journey**: The database confirms the write, the service layer packages the confirmation into a clean Response DTO, and the controller wraps it in a standard success envelope, sending it back to your device screen.

---

## 2. How JWT Works: The Secure Digital Passport Analogy

Imagine you go to an exclusive amusement park. Instead of having a security guard follow you to every ride to verify your ticket, the front gate checks your ID once, prints a secure **wristband (JWT)**, and hands it to you. 

Every time you want to board a ride (secured API endpoints), you simply show your wristband. The operator doesn't call the front gate to check their books; they simply inspect the cryptographic seal on your wristband to verify it is authentic.

```
                  ┌────────────────────────────────────────┐
                  │              JWT PASSPORT              │
                  ├────────────────────────────────────────┤
                  │ Header   : {"alg": "HS256", ...}      │
                  │ Payload  : {"sub": "student@abc.com",  │
                  │             "role": "ROLE_STUDENT"}    │
                  │ Signature: [Cryptographic Seal]        │
                  └────────────────────────────────────────┘
```

A **JSON Web Token (JWT)** consists of three distinct parts separated by dots (`.`):
1. **Header (Red)**: Declares the type of token (JWT) and the hashing algorithm used (e.g., HMAC-SHA256).
2. **Payload (Claims - Blue)**: The actual data inside the passport. It contains information like:
   * `sub` (Subject): The user's unique identifier (e.g., `student@eduverse.com`).
   * `role`: The user's role (e.g., `ROLE_STUDENT`).
   * `exp`: The exact expiration timestamp (24 hours from creation).
3. **Signature (Green)**: The cryptographic seal. The server takes the Header and Payload, combines them with a secret key known *only* to the server, and hashes it. If an attacker tries to modify the payload (e.g., changing their role from `STUDENT` to `ADMIN`), the signature check fails immediately, and the server rejects the request.

---

## 3. How Spring Security Works: Castle Guards & Keys

Think of your Spring Boot backend as a **Medieval Castle**:
* **Public Paths** (e.g., browsing the course catalog or signing up) are like the public market outside the castle. Anyone can enter without verification.
* **Secured Paths** (e.g., enrolling in a course, posting reviews) are inside the castle walls.
* **Admin Paths** (e.g., managing database backups or system stats) are inside the inner treasury.

```
       [ Public Paths ] ──────────────────────► Allowed (Public Market)
       
       [ Incoming HTTP Request ] ────► [ JwtAuthenticationFilter ] (The Castle Gate Guard)
                                                   │
                                     (Extracts & Validates Token)
                                                   ▼
                                       [ SecurityContextHolder ] (The Guest Registry)
                                                   │
                                                   ▼
       [ Restricted Controller ] ◄─── (Allowed) ───┤ ◄─── (Checks Roles & Privileges)
```

1. **The Gate Guard (Filters)**: Our custom `JwtAuthenticationFilter` stands at the castle drawbridge. Every request is intercepted. If the request wants to access a public path, the guard waves it through. If it is a secured path, the guard demands to see your JWT passport.
2. **The Guest Registry (SecurityContext)**: If your passport is valid, the guard writes your username and roles into a temporary registry called the **`SecurityContextHolder`**. This registry is kept active for the duration of your request.
3. **The Inner Door Guards (Method Security)**: When your request attempts to enter a specific room (like the `createCourse()` controller method), Spring Security checks the guest registry: *"Is this user registered as an `INSTRUCTOR`? Yes? Welcome inside. No? Throw a 403 Access Denied error."*

---

## 4. How Asynchronous Processing Works: The Coffee Shop Analogy

To understand why **asynchronous processing** is essential for high-performance servers, let's look at a busy coffee shop:

### The Synchronous (Blocking) Coffee Shop:
1. You order an espresso.
2. The cashier takes your payment.
3. The cashier walks over to the espresso machine, grinds the beans, froths the milk, and brews the coffee (5 minutes).
4. While the cashier is brewing your coffee, the queue of customers grows out the door. No one else can order. The shop is blocked!

### The Asynchronous (Non-Blocking) Coffee Shop (Eduverse Way):
1. You order an espresso.
2. The cashier takes your payment, writes your order on a ticket, and handles it to the barista (Background Worker).
3. The cashier immediately turns to the next customer and takes their order. The cash queue keeps moving!
4. Meanwhile, the barista brews your espresso in the background. When it is ready, they call your name.

```
Synchronous (Blocking Cashier):
Customer ──► [ Cashier Takes Order ] ──► [ Cashier Brews Coffee (5m) ] ──► Customer Gets Coffee
(Next customer in line is stuck waiting!)

Asynchronous (Decoupled Barista):
Customer ──► [ Cashier Takes Order ] ──► Customer Gets Receipt (Instant!)
                         │
                         ▼ (Passes Ticket)
               [ Barista Brews Coffee in Background ] ──► Notification / Email
```

In **Eduverse**, the Tomcat request threads are the **cashiers**, and our background `ThreadPoolTaskExecutor` threads are the **baristas**. 
When a student enrolls in a course, the Tomcat thread writes the enrollment to the database and immediately returns an HTTP success code to the user. At the same time, it hands a "ticket" to the background worker pool to send out the welcome email. The student doesn't wait a single second for external email servers to respond.

---

## 5. How Events Work: Decoupling and Broadcasting

In many backend systems, when a specific event occurs—like a student submitting a review—the `ReviewService` has to do multiple things:
1. Save the review to the database.
2. Update the course's overall rating.
3. Send an email notification to the instructor.
4. Send an alert to the admin dashboard.

If you write all this code inside a single method, the code becomes bloated and tightly coupled. If the email system breaks, the entire review submission fails.

**The Eduverse Event System Solution**:
We use a **Broadcasting (Publisher-Subscriber) Model**.

```
[ Review Submitted ] ──► ReviewService saves review to DB
                                 │
                                 ▼ (Publishes Event)
                     [ ReviewCreatedEvent ]
                                 │
                 ┌───────────────┼───────────────┐
                 ▼ (Multicast)   ▼ (Multicast)   ▼ (Multicast)
             Listener A      Listener B      Listener C
             (Update Rating) (Email Instructor) (Log System Audit)
```

1. **The Broadcast**: The `ReviewService` saves the review, and immediately publishes an **`ReviewCreatedEvent`** to the system. It then says: *"I'm done!"* and returns success.
2. **The Listeners**: We have separate, independent components called **EventListeners** listening for this specific event.
3. **Isolation**: The `EmailListener` receives the event and handles the email delivery in a background thread. If it fails, the review remains saved, the course rating is still updated, and the user's experience is flawless.

---

## 6. How Uploads Work: Multi-Strategy & Security Pipelines

Uploading files (like course thumbnails or resource PDF slides) requires high security to prevent malicious users from uploading viruses or overwhelming the server's storage capacity.

Eduverse uses a multi-gate security pipeline before storing a file:

```
[ Client Upload File ] 
         │
         ▼ (Gate 1: Rate Limiter)
[ Rate Limiter ] ────► Blocks spam uploads from overwhelming the server
         │
         ▼ (Gate 2: File Validator)
[ File Validator ] ──► Checks size (<10MB) & allowed extensions (.jpg, .pdf)
         │
         ▼ (Gate 3: Magic Bytes Check)
[ MIME Scanner ] ────► Reads the first few bytes (magic bytes) to verify the file signature
         │
         ▼ (Gate 4: Active Strategy Selection)
[ Storage Strategy ] ─┬─► (aws.s3.enabled = false) ──► [ Local Storage ] (Saves to local /uploads folder)
                      └─► (aws.s3.enabled = true)   ──► [ AWS S3 Storage ] (Saves to secure S3 cloud bucket)
```

1. **Gate 1: Rate Limiter**: Ensures users cannot spam-upload hundreds of files per minute, protecting bandwidth and CPU.
2. **Gate 2: File Validator**: Verifies basic constraints. E.g., is the file size within limits (e.g., under 10MB)? Is the file extension allowed?
3. **Gate 3: MIME Magic Bytes Verification**: Attackers sometimes change the extension of a malicious script (like `virus.exe` to `photo.jpg`). To prevent this, our validator reads the actual **magic bytes** (the first few bytes of the file stream) to confirm the file signature matches its claimed type.
4. **Gate 4: Storage strategy**: Once validated, the file is processed by the active strategy:
   * **Local Strategy**: Saves the file securely to a folder inside the project workspace directory (`/uploads`).
   * **AWS S3 Cloud Strategy**: Uploads the file to an **AWS S3 bucket** and generates an expirable URL to view the file securely.

---

## 7. How Database Relations Work: Entity Mapping in PostgreSQL

Eduverse uses a relational database model in PostgreSQL, mapping primary keys (`id`) and foreign keys to link entities together:

```
      ┌──────────┐              ┌───────────────┐
      │  ROLES   │              │  CATEGORIES   │
      └────┬─────┘              └───────┬───────┘
           │ (1)                        │ (1)
           │                            │
           │ (N)                        │ (N)
      ┌────▼─────┐ (1)         (N) ┌────▼─────┐ (1)         (N) ┌───────────────────┐
      │  USERS   │◄────────────────┤ COURSES  │◄────────────────┤  COURSE_CONTENTS  │
      └────┬─────┘                 └────▲─────┘                 └───────────────────┘
           │ (1)                        │ (1)
           ├──────────────┬─────────────┤
           │ (N)          │             │ (N)
      ┌────▼─────┐        │        ┌────▼─────┐
      │ REVIEWS  │        └───────►│ENROLLMENTS│
      └──────────┘ (N)         (N) └──────────┘
```

* **Roles to Users (One-to-Many / `1:N`)**: A single role (like `ROLE_STUDENT`) can belong to *many* users, but each user has exactly *one* role. We track this by putting the `role_id` inside the `users` table.
* **Users (Instructors) to Courses (One-to-Many / `1:N`)**: An instructor can teach *many* courses, but each course is owned by *one* primary instructor. We link them using `instructor_id` in the `courses` table.
* **Categories to Courses (One-to-Many / `1:N`)**: A category (like `Software Development`) can have *many* courses, linked via `category_id` in the `courses` table.
* **Courses to Course Contents (One-to-Many / `1:N`)**: A course contains *many* lecture modules or resources, mapped via `course_id` inside the `course_contents` table.
* **Enrollments (Many-to-Many Connection / `N:M`)**: A student can enroll in *many* courses, and a course can have *many* enrolled students. We represent this relationship using an **Enrollment Join Entity**, which links `student_id` and `course_id` together.
* **Reviews (Many-to-Many Connection / `N:M`)**: Similarly, a student can write *many* reviews, and a course can receive *many* reviews, linked via `student_id` and `course_id` in the `reviews` table.
