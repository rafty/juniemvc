# junie mvc

## REST API Endpoints

- Beer
  - POST /api/v1/beer
  - GET /api/v1/beer/{id}
  - GET /api/v1/beer
  - PUT /api/v1/beer/{id}
  - DELETE /api/v1/beer/{id}

- Beer Orders
  - POST /api/v1/beer-orders
  - GET /api/v1/beer-orders/{id}

- Customers
  - POST /api/v1/customers
  - GET /api/v1/customers/{id}
  - GET /api/v1/customers
  - PUT /api/v1/customers/{id}
  - DELETE /api/v1/customers/{id}

All controllers use constructor injection with package-private visibility and DTO-based request/response models per the project Spring Boot guidelines.