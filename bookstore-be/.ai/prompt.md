================================================================================

ONLINE BOOKSTORE BACKEND - PROMPTS & AI WORKFLOW HISTORY

================================================================================

PROJECT SPECIFICATION

--------------------------------------------------------------------------------

User Query:

"Based on .ai/description.md, let's make spec.md first."


PHASE 2: BOOK MANAGEMENT (READ-ONLY)

--------------------------------------------------------------------------------

User Queries:

"Spec is final, let's implement Phase 2."

"Write a failing test first: Create an integration test (e.g., BookControllerTest
or a repository/service test) that attempts to fetch available books from the
database.

Based on spec.md, implement Phase 2 (Book Management Read-Only Feature) for our
Spring Boot application.

Please generate:

1. BookService
2. BookController
3. BookEntity"


PHASE 3: USER AUTHENTICATION & MANAGEMENT

--------------------------------------------------------------------------------

User Query:

"Based on spec.md, implement Phase 3 using TDD unit/integration testing
principles.

Use basic Spring Security, filters, and setup config for authentication.

Generate:

1. UserEntity with fields like id, username, email, and password.
2. UserRepository for UserEntity extending JpaRepository<User, Long>.
3. UserService and UserServiceImpl implementing custom registration and login
   logic (handling user creation and validation).
4. UserController exposing POST /api/users/register and POST /api/users/login
   endpoints with proper status codes.

Exclude Swagger and the H2 console from default authentication.
Set up CORS for localhost port 8081."


PHASE 4: SHOPPING CART

--------------------------------------------------------------------------------

User Query:

"Based on spec.md, implement Phase 4.

Generate:

1. Cart and CartItem entities.
2. CartRepository for Cart extending JpaRepository.
3. CartService and CartServiceImpl implementing core cart logic like adding
   items, updating quantities, removing items, and fetching cart details, using
   @Transactional.
4. CartController REST endpoints for the shopping cart like POST
   /api/cart/items, GET /api/cart, and DELETE /api/cart/items/{id} with status
   codes (200, 201, 400)."



5. PHASE 5: ORDER PROCESSING, CONCURRENCY & IDEMPOTENCY

--------------------------------------------------------------------------------

User Query:

"Based on spec.md, implement Phase 5.

Please generate:

1. Order and OrderItem entities with status enums like PENDING, COMPLETED, and
   CANCELLED.
2. OrderRepository.
3. OrderService and OrderServiceImpl implementing core checkout logic.
4. OrderController exposing POST /api/orders/checkout.

Ensure:

1. Concurrency and idempotency.
2. Use PESSIMISTIC_WRITE locks on Book inventory to prevent race conditions
   and concurrent overselling.
3. Validate empty carts and insufficient stock, use @Transactional, and add
   idempotency checks to prevent duplicate order processing.
4. Return appropriate status codes (201 Created, 400 Bad Request)."


PHASE 6: GLOBAL ERROR HANDLING, LOGGING & POLISH

--------------------------------------------------------------------------------

User Query:

"Based on spec.md, implement Phase 6 (Global Error Handling, Logging & Polish)
for our Spring Boot backend.

Please generate:

1. A Global Exception Handler (@ControllerAdvice) catching custom business
   exceptions (e.g., ResourceNotFoundException, InsufficientStockException,
   CartEmptyException) and returning standardized, user-friendly error
   response payloads with proper HTTP status codes (400, 404, etc.).
2. Structured logging using SLF4J/Logback across critical checkpoints such as
   add-to-cart operations, user sign-ins, and order checkouts.
3. SETUP.md documentation detailing application setup, build instructions,
   and how to execute the test suite.

Ensure clean coding practices, standard error DTO structures, and professional
enterprise standards."



LOGGING FOLLOW-UPS

--------------------------------------------------------------------------------

"Added logs to all controllers/services?"



SEED DATA

--------------------------------------------------------------------------------

User Queries:

"Update DataInitializer.java and initial-data.json."

"Add orders, cart, etc."



SERVICE LAYER TESTS

--------------------------------------------------------------------------------

User Query:

"Write comprehensive JUnit 5 and Mockito unit test classes for our service
layer (e.g., BookServiceTest, UserServiceTest, CartServiceTest, and
OrderServiceTest) targeting 100% code coverage.

Ensure:

1. Cover all possible edge cases, happy paths, boundary conditions, and
   failure/exception scenarios, such as resource not found, empty cart
   checkouts, insufficient stock exceptions, race conditions, and idempotency
   checks.
2. If tests identify missing validations, null checks, edge case handling, or
   exception management in the underlying service/entity code, proactively
   implement the necessary code changes.
3. Use @ExtendWith(MockitoExtension.class), @Mock, and @InjectMocks following
   Given-When-Then structure with precise, professional assertions."


================================================================================
