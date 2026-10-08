# Project Specification & Development Blueprint: Online Bookstore (Backend)

## 1. Overview
This project is an enterprise-grade backend service for an Online Bookstore code kata, built using Spring Boot (Java 17). It exposes a RESTful API to manage book catalogs, user authentication, shopping carts, and concurrent order processing with strict adherence to Test-Driven Development (TDD) and clean architecture principles.

---

## 2. Technical Stack & Dependencies
* **Core Framework:** Spring Boot (Parent Version 4.1.1)
* **Language:** Java 17
* **Persistence & Database:** Spring Data JPA, H2 In-Memory Database (with local console support)
* **Validation & Utilities:** Spring Boot Starter Validation, Project Lombok
* **Testing:** Spring Boot Test Starters (JPA, Validation, WebMVC Test)
* **Build Tool:** Maven

---

## 3. Architecture & Design Principles
* **Separation of Concerns:** Strict layer isolation across Controllers, Services, Repositories, and Domain Models.
* **Domain-Driven Models:** Entities (`Book`, `User`, `Cart`, `CartItem`, `Order`, `OrderItem`) encapsulate business rules to avoid logic leaking into DTOs or services.
* **Design Patterns:** Application of Builder, Strategy, and transactional boundary management (`@Transactional`).
* **Concurrency & Safety:** Pessimistic/Optimistic locking mechanisms on stock inventories to prevent race conditions and overselling.
* **Idempotency:** Guaranteeing safe, duplicate-resistant order placement and checkout flows.

---

## 4. Step-by-Step Development Plan

### Phase 1: Project Foundation & Configuration
* **Step 1.1:** Baseline verification of Java 17, Maven dependencies, and profile-based configuration (`application.properties` and `application-local.properties`).
* **Step 1.2:** Project scaffolding and structure initialization.

### Phase 2: Domain Model & Book Management (Read-Only Feature)
* **Step 2.1:** Implement the `Book` entity with encapsulation, bean validations (`@NotBlank`, `@DecimalMin`, `@Min`), and `BookRepository`.
* **Step 2.2:** Write failing integration tests (TDD) for catalog retrieval and implement `BookService` and `BookController` endpoints (`GET /api/books`) and (`GET /api/books\{id}`).

### Phase 3: User Authentication & Management
* **Step 3.1:** Create `User` entity and `UserRepository` for handling custom registration and login flows.
* **Step 3.2:** Write failing tests (TDD) for user signup and validation rules, followed by `UserService` and `UserController` implementations.

### Phase 4: Shopping Cart Domain & Operations
* **Step 4.1:** Model `Cart` and `CartItem` domain entities mapping user relations and item collections.
* **Step 4.2:** Write failing TDD tests for adding items, updating quantities, removing items, and empty cart edge cases. Implement `CartService` with `@Transactional` boundaries and expose corresponding REST endpoints.

### Phase 5: Order Processing, Concurrency & Idempotency
* **Step 5.1:** Design `Order` and `OrderItem` models with status Enums and pricing snapshots.
* **Step 5.2:** Implement TDD tests for checkout validations (e.g., handling insufficient stock or empty carts). Add stock locking mechanisms (`@Lock`) to prevent concurrent overselling and enforce checkout idempotency.

### Phase 6: Global Error Handling, Logging & Polish
* **Step 6.1:** Implement global exception handling (`@ControllerAdvice`) to map custom domain exceptions to standardized error responses and proper HTTP status codes (200, 201, 400, 404).
* **Step 6.2:** Configure structured logging via SLF4J/Logback for critical checkpoints (add-to-cart, sign-in, checkout).
* **Step 6.3:** Author a comprehensive `README.md` with clear setup, run, and test documentation.
* **Step 6.4:** Final code review and repository wrap-up.