# Leave Management System

A comprehensive leave management system for IST, built with Spring Boot and Google Authentication.

## Features

- User Management with Google Authentication
- Leave Request Management
- Role-based Access Control
- RESTful API
- Swagger Documentation

## Tech Stack

- Java 17
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT Authentication
- Google OAuth2
- Swagger/OpenAPI

## Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- Google Cloud Account (for authentication)

## Environment Variables

```env
# Database Configuration
POSTGRES_URL=jdbc:postgresql://localhost:5432/leave_management
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password_here

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION=86400000 # 24 hours

# Google OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
```

## Running the Application

1. Clone the repository
2. Configure environment variables
3. Run `mvn spring-boot:run`
4. Access the application at `http://localhost:8080`

## API Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html`

## Project Structure

```
leave-management-system/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── ist/
│       │           ├── config/
│       │           ├── common/
│       │           ├── user_management/
│       │           └── leave_management/
│       └── resources/
└── pom.xml
```

## Contributing

Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the IST License. 