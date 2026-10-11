================================================================================

ONLINE BOOKSTORE KATA - PROMPTS & AI WORKFLOW HISTORY

================================================================================

PROJECT PLANNING & INITIAL SETUP

--------------------------------------------------------------------------------

User Query:

"I have a project for KATA - DISTRIBUTED DEVELOPER - Online Bookstore.

Task: Create a simple online bookstore with React frontend and Spring Boot

REST backend. Front-end in React, back-end using RESTful API, TDD workflow,

separated repositories. Requirements cover book catalog, shopping cart,

checkout, user auth, error handling, status codes, and README.

Give me a basic plan on how to start with the project."



Objective:

- Establish decoupled architecture and domain models.

- Plan API contract across frontend and backend.

- Define a phased Red-Green-Refactor TDD roadmap.


FRONTEND BOOTSTRAP & TOOLING

--------------------------------------------------------------------------------

User Query:

"How to start with project setup files for Vite, Vitest, React Testing Library, and

MSW for the bookstore frontend. Give the commands like Vite commands as well

to build a Vite project first and then later on proceed with other commands

that I can perform in vs code."



Objective:

- Scaffold Vite React TypeScript project.

- Install Vitest, jsdom, testing-library, MSW, and icon primitives.

- Configure `vite.config.ts`, `setupTests.ts`, `tsconfig.app.json`, and sanity tests.



PHASE 1: BOOK CATALOG DISPLAY 

--------------------------------------------------------------------------------

User Query:

"Lets start with Book Catalog Display

Phase 1: Catalog Component

Red: Write a test asserting that BookList renders loading states, errors,

and an array of books (title, author, price, and 'Add to Cart' button)

using MSW to intercept /api/books.

Green: Implement the BookCard and BookList components with fetching logic.

Refactor: Extract fetching logic into a custom hook (useBooks).

1. React 18 version (avoid React.FC).

2. Use Radix UI.

3. Connect BookList on app mount.

4. Minimal styling via Radix UI."



Objective:

- Write failing component test with MSW handlers (`BookList.test.tsx`).

- Implement `BookCard.tsx` and `BookList.tsx` using Radix UI primitives.

- Refactor fetching logic into reusable `useBooks.ts` custom hook.



TROUBLESHOOTING & RUNTIME CONFIGURATION

--------------------------------------------------------------------------------

User Queries:

a) "Property 'toBeInTheDocument' does not exist on type 'Assertion<void, HTMLElement>'."

b) "All declarations of 'Assertion' must have identical type parameters. Cannot find name 'expect'."

c) "This is how booklist shows. I want to show each book like a card view a thin margin to differentiate between books."

d) "On reloading blank white screen is coming."

e) "The string did not match the expected pattern."



Solutions Implemented:

- Switched to official `@testing-library/jest-dom/vitest` import and updated `tsconfig`.

- Overrode Vite default centering CSS styles in `index.css`.

- Added defensive `try/catch` around `localStorage` JSON parsing in `AuthContext`.

- Added Content-Type checks prior to `response.json()` to avoid crash on HTML proxy errors.



AUTHENTICATION & LOGIN WORKFLOW

--------------------------------------------------------------------------------

User Query:

"First we need to create a login page for user.

1. Register api takes username, email, password and calls api.

2. Login sends username and password to backend.

After successful show booklist."



Objective:

- Define auth contracts (`User`, `LoginPayload`, `RegisterPayload`, `AuthResponse`).

- Build `AuthContext` with session caching in `localStorage`.

- Implement `AuthView.tsx` with toggleable login/registration modes.

- Conditionally render auth screen vs authenticated store shell in `App.tsx`.



PHASES 2 & 3: CART STATE MANAGEMENT & CART VIEW MODAL

--------------------------------------------------------------------------------

User Query:

"Lets move to stage 2 and 3 of cart management:

- Implement a shopping cart feature that allows users to add books to their cart.
- Display the contents of the shopping cart with the ability to modify quantities

and remove items.

Adding or removing from a cart will be on the basis of book id. Backend will

add/remove on the basis of userID (configured using spring security in backend)."



Objective:

- Establish `CartContext` with `useReducer` to manage pure cart actions.

- Implement tests verifying quantity adjustments, removals, and subtotal math.

- Build `CartView.tsx` dialog with table layout and step controls.



Phase 4

------------------------------------------------------------------------------------------------

@folder:bookstore-fe
@file:spec.md implement phase 4 using TDD @file:CartController.java has api contracts


Phase 5

----------------------------------------------------------------------------------------------

@folder:bookstore-fe
@file:spec.md  This is spec for front end.  implement phase 5 using TDD @file:OrderController.java  has api contracts
ORder summary view should show as soon as checkout is clicked (no payment logic)
On checkout we need to send idompotency key to order api . USe UUID-v4


Error handling

------------------------------------------------------------------------------------------------

@folder:bookstore-fe
Lets make a global error handling for this .