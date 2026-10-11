# Online Bookstore

**BNP Paribas Fortis — KATA assessment**

A full-stack bookstore application built with a Spring Boot REST API and a
React + TypeScript frontend. Browse books, manage a cart, and check out with
stock-safe order processing and idempotent requests.

## Contents

- [Technology](#technology)
- [Prerequisites](#prerequisites)
- [Run locally](#run-locally)
- [Demo account](#demo-account)
- [Build and test](#build-and-test)
- [Project structure](#project-structure)
- [Local development](#local-development)

## Technology

| Area | Stack |
| --- | --- |
| Backend | Java 17, Spring Boot, Spring Security, Spring Data JPA |
| Database | H2 in-memory |
| Frontend | React, TypeScript, Vite, Radix UI |
| Backend build | Maven Wrapper |
| Frontend tooling | Node.js and npm |

## Prerequisites

- Java 17 or later
- Node.js 20.19+ or 22.12+ and npm
- Git (optional, for cloning)

The backend includes the Maven Wrapper, so a separate Maven installation is
not required.

## Run locally

Clone the repository and open two terminals from its root directory.

### Start the backend

```bash
cd bookstore-be
./mvnw spring-boot:run
```

The API starts at <http://localhost:8080>. The default `local` profile creates
an in-memory H2 database and loads sample books, a demo user, cart, and order.

### Start the frontend

```bash
cd bookstore-fe
npm install
npm run dev
```

Open <http://localhost:8081>. Vite proxies `/api` requests to the backend on
port `8080`.

## Demo account

| Username | Password |
| --- | --- |
| `demo` | `DemoPassword123` |

This account is for local development only. Do not use these credentials in a
deployed environment.

## Build and test

### Backend

```bash
cd bookstore-be
./mvnw test
./mvnw clean verify
```

### Frontend

```bash
cd bookstore-fe
npm run test:run
npm run build
npm run lint
```

To generate frontend test coverage, run `npm run test:coverage`.

## Project structure

```text
.
├── bookstore-be/    Spring Boot API, persistence, and backend tests
└── bookstore-fe/    React + TypeScript frontend
```

## Local development

- H2 data is in-memory and is recreated when the backend restarts.
- The H2 console is available at <http://localhost:8080/h2-console>. The JDBC
  URL and credentials are configured in
  `bookstore-be/src/main/resources/application-local.properties`.
- Swagger UI is available at <http://localhost:8080/swagger-ui/index.html>;
  the OpenAPI document is at <http://localhost:8080/v3/api-docs>.
- CORS is configured for the frontend origin `http://localhost:8081`.
- Backend logs are written to the console and configured rolling log file.

For frontend-specific setup, scripts, and testing notes, see
[`bookstore-fe/README.md`](bookstore-fe/README.md).
