================================================================================

FRONTEND TECHNICAL SPECIFICATION (spec.txt)

Online Bookstore Kata - Frontend Client

================================================================================



1. SCOPE & OBJECTIVE

--------------------------------------------------------------------------------

This specification outlines the technical design, component hierarchy, state

management, and phased Test-Driven Development (TDD) workflow for the Online

Bookstore frontend client.



The application is built using React 18, TypeScript, and Radix UI Themes. It

functions as an independent Single Page Application (SPA) communicating with

a Spring Boot backend protected by Spring Security. All features follow strict

Red-Green-Refactor testing standards using Vitest, React Testing Library, and

Mock Service Worker (MSW).





2. ARCHITECTURE & TECH STACK

--------------------------------------------------------------------------------

- Framework: React 18 (standard functional components without React.FC)

- Language: TypeScript (strict mode enabled)

- UI System: Radix UI Themes (@radix-ui/themes) for accessible layout & inputs

- Test Runners: Vitest and React Testing Library (JSDOM environment)

- Network Isolation: Mock Service Worker (MSW) for API contract simulation

- State Architecture: Context API with useReducer for cart, useState for auth

- Client Storage: Browser localStorage with defensive JSON parsing





3. APPLICATION STRUCTURE & COMPONENT HIERARCHY

--------------------------------------------------------------------------------

App (Root Provider: Theme, Auth, Cart)

│

├── Authentication Guard

│   └── AuthView (Login & Registration forms with validation)

│

└── Authenticated Bookstore Shell

    ├── Header Bar

    │   ├── App Title & User Profile info

    │   ├── Cart Trigger Badge (displays real-time total count)

    │   └── Logout Trigger Button

    │

    ├── Main Catalog View

    │   ├── BookList (Catalog orchestrator handling fetch/load/error states)

    │   └── BookCard (Product display with title, author, price, action CTA)

    │

    ├── Shopping Cart Dialog

    │   ├── EmptyCartState

    │   ├── CartTable (Line items with +/- step controls and removal action)

    │   └── CartFooter (Calculated subtotal, close action, checkout CTA)

    │

    └── Checkout Modal

        ├── OrderSummaryView (Itemized list and price breakdown)

        └── OrderConfirmation (Order ID and status feedback)





4. COMPLETE PHASES OF DEVELOPMENT (TDD ROADMAP)

--------------------------------------------------------------------------------



PHASE 0: TOOLING & TEST RUNNER BOOTSTRAP

- Setup Vite with React and TypeScript templates.

- Configure Vitest, Testing Library, and JSDOM test runner.

- Initialize Mock Service Worker (MSW) handlers and server for headless tests.

- Resolve TypeScript typing integrations for DOM assertions.

- Deliverable: Working test runner running a green sanity test suite.



PHASE 1: AUTHENTICATION & USER SESSION (STAGE 1)

- Red: Write unit and integration tests for login/registration forms:

  * Switching between Login and Register views.

  * Form inputs for credentials (username, email, password).

  * Successful authentication submission and error callout handling.

- Green: Implement AuthView using Radix UI primitives and create AuthContext

  for persistent session management via localStorage.

- Refactor: Standardize input sanitization and add defensive parsing for

  non-JSON server responses (e.g., 404/504 gateway errors).



PHASE 2: BOOK CATALOG DISPLAY (STAGE 2)

- Red: Write component tests for BookList verifying:

  * Initial loading state (accessible role="status").

  * Display of books with titles, authors, and formatted prices.

  * Resilient error state (accessible role="alert") on 500 server errors.

- Green: Implement BookCard and BookList components with layout styling.

- Refactor: Extract asynchronous network fetching into custom hook `useBooks`.



PHASE 3: SHOPPING CART STATE MANAGEMENT (STAGE 3)

- Red: Write unit tests for CartContext:

  * Adding a book (initial quantity: 1).

  * Adding an existing book increments quantity.

  * Modifying quantities via step controls.

  * Reducing item quantity to 0 removes the item.

  * Deleting an item completely by book identifier.

  * Accurate computation of aggregate total items and total price.

- Green: Implement CartProvider and reducer to manage cart state and sync

  mutations optimistically with backend endpoints.

- Refactor: Ensure state mutations remain pure and immutable.



PHASE 4: SHOPPING CART MODAL & CONTROLS (STAGE 4)

- Red: Write component tests for CartView:

  * Rendering empty cart message when items are absent.

  * Rendering line-item tables with title, unit price, and subtotal.

  * Triggering increase, decrease, and delete actions.

- Green: Implement CartView dialog modal using Radix UI Dialog and Table.

- Refactor: Connect cart trigger badge in top navigation to open/close drawer.



PHASE 5: CHECKOUT PROCESS & ORDER SUMMARY (STAGE 5)

- Red: Write integration tests simulating the checkout pipeline:

  * Transition from shopping cart to order summary view.

  * Verify item breakdown, subtotal, and total calculation.

  * Submit checkout action and verify cart resets upon success.

- Green: Implement CheckoutView/CheckoutSummary modal and submission flow.

- Refactor: Display order confirmation with unique order ID and success screen.





5. CORE SYSTEM INVARIANTS & BEHAVIORS

--------------------------------------------------------------------------------

- User Identification: Handled transparently by Spring Security session or

  bearer tokens. Frontend communicates operations strictly by book identifier.

- Cart Math Invariants:

  * Total items count equals the sum of individual item quantities.

  * Total price equals the sum of (unit price * item quantity).

  * Decrementing quantity below 1 triggers item removal.

- Network Resilience: Any failed network request displays an actionable,

  non-blocking alert message to the user without breaking existing state.

- Test Coverage Expectation: Every state mutation, API branch, and UI view

  must have a corresponding failing test prior to implementation code.

================================================================================