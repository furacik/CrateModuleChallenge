A Spring Boot app for managing customers, loans, and installments with role-based authorization.

## 📦 Features

- Admin can:
    - View all customers
    - Create loans for any customer
    - See all installments
- Customer can:
    - View their own loans
    - Pay their own installments
- Admin & customer roles with HTTP Basic Auth
- H2 in-memory database
- Swagger API documentation

How to Run

### tech 

Java 17
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- H2 Database
- Swagger / OpenAPI
- Maven
### ✅ Prerequisites

- Java 17+
- Maven
- IntelliJ IDEA (used)

### Run the Project
http://localhost:8080/app

mvn spring-boot:run

All endpoints except POST /api/customers/create require authentication.

Role is assigned during customer creation (ADMIN or CUSTOMER).

H2 Database Console
URL: http://localhost:8080/app/h2-console

JDBC URL: jdbc:h2:mem:loandb

User: root

Password: root

Swagger UI: http://localhost:8080/app/swagger-ui.html


### without auth
curl -X POST http://localhost:8080/app/api/customers/create \
-H 'Content-Type: application/json' \
-d '{
"name": "John",
"surname": "Doe",
"creditLimit": 5000.0,
"username": "johndoe",
"password": "password123",
"role": "CUSTOMER"
}'

### with auth 
curl -X POST http://localhost:8080/app/api/loans/create \
-u admin:admin123 \
-H 'Content-Type: application/json' \
-d '{
"customerId": 1,
"loanAmount": 10000,
"interestRate": 0.4,
"numberOfInstallment": 9
}'