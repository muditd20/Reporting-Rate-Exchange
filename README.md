# Reporting Rate Exchange

This is a production-ready Java Spring Boot REST API application designed to store purchase transactions in USD and retrieve them converted into various target currencies supported by the US Treasury Reporting Rates of Exchange API.

## Features & Requirements Satisfied
* **Store a Purchase Transaction**: Accepts a transaction (description, date, amount), validates all fields (e.g. description <= 50 characters, amount > 0, valid dates), and safely persists it into H2 Database with an auto-generated unique ID.
* **Retrieve Converted Transaction**: Retrieves a previously saved transaction and seamlessly converts the purchase amount to a target currency (e.g., `Canada`, `India`, `Australia`).
* **Treasury API Integration**: Dynamically queries the US Treasury API to find the most recent exchange rate within **6 months** prior to the purchase transaction date.
* **Robust Error Handling**: Gracefully maps validation issues and unconvertible currency scenarios to user-friendly HTTP 400 Bad Request responses.

## Technology Stack
- **Java 17**
- **Spring Boot 3.3.0**
- **Spring Data JPA & Hibernate ORM**
- **H2 Database**
- **Maven**
- **Testcontainers & WireMock** (for robust integration testing)

## Prerequisites
- **Java 17+**
- **Maven** (A Maven wrapper `mvnw` is included)
- **Docker / Docker Desktop** (Required to spin up H2 Database and run Testcontainers for automated testing)

---

## Getting Started

### 1. Database Setup
A `docker-compose.yml` file is provided inside the `Reporting Rate Exchange/purchase-transaction/` directory.

Start the local H2 Database database:
```bash
cd "Reporting Rate Exchange/purchase-transaction"
docker-compose up -d
```
*(Ensure you have updated your `<YOUR USER>` and `<YOUR PASSWORD>` in `application.yml` and `docker-compose.yml` if you modified them).*

### 2. Build and Test
The project includes fully automated functional integration tests using MockMvc, Testcontainers, and WireMock. 

Run the test suite:
```bash
.\mvnw.cmd test
```

### 3. Run the Application
Start the Spring Boot server:
```bash
.\mvnw.cmd spring-boot:run
```
The application will boot up and run on `http://localhost:8080`.

---

## API Usage

### 1. Store a Purchase Transaction
**Endpoint:** `POST /api/purchases`

**Payload:**
```json
{
  "description": "Office Supplies",
  "transactionDate": "2023-10-01",
  "purchaseAmount": 150.75
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/purchases \
-H "Content-Type: application/json" \
-d '{"description": "Office Supplies", "transactionDate": "2023-10-01", "purchaseAmount": 150.75}'
```

### 2. Retrieve a Converted Transaction
**Endpoint:** `GET /api/purchases/{id}?targetCountry={country}`

Pass the unique ID returned from the POST request, and exactly match the country name as indexed by the Treasury API (e.g. `India`, `Canada`, `Japan`).

**CURL Example:**
```bash
curl -X GET "http://localhost:8080/api/purchases/1?targetCountry=India"
```

**Successful Response:**
```json
{
  "id": 1,
  "description": "Office Supplies",
  "transactionDate": "2023-10-01",
  "originalUsdAmount": 150.75,
  "exchangeRate": 83.15,
  "convertedAmount": 12534.86
}
```

## Clean Code & Best Practices
- **KISS & DRY:** The architecture strictly isolates responsibilities (Controller layer, Service Layer, External Client, and JPA Repository). 
- **Modern Java:** Utilizes Java 17 `record`s for concise, immutable Data Transfer Objects (DTOs) without getter/setter boilerplate.
- **Resilience:** The HTTP Client is securely configured to seamlessly bypass strict internal proxies for local testing while keeping production patterns intact.
