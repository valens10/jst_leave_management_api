# Leave Management System

A comprehensive leave management system built with Spring Boot and React, designed to streamline employee leave requests and approvals according to Rwandan Labor Law (2023).

## Features

### Core Features
- **Employee Dashboard**
  - View leave balances
  - Apply for leave
  - Track leave history
  - Upload supporting documents
  - Calendar integration with public holidays and team leaves

- **Leave Application**
  - Multiple leave types (PTO, Sick, Compassionate, Maternity)
  - Full-day/half-day options
  - Document upload support
  - Application status tracking

- **Approval Workflow**
  - Manager/Admin approval process
  - Email and in-app notifications
  - Approval/rejection with comments

- **Leave Balance Management**
  - Auto-accrual (1.66 days/month)
  - Carry-forward logic (max 5 days)
  - Manual balance adjustments

- **Admin Panel**
  - Leave type management
  - Balance adjustments
  - Team calendar views
  - Report generation and export

- **Notifications**
  - Email notifications
  - In-app notifications
  - Real-time status updates

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.x
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- SendGrid for email notifications
- Microsoft OAuth2 Authentication

### Frontend
- React
- Material-UI
- Redux Toolkit
- Axios
- React Router

## Prerequisites

- Docker and Docker Compose
- Java 17 JDK
- Node.js 18+
- npm or yarn

## Quick Start with Docker

1. Clone the repository:
```bash
git clone https://github.com/yourusername/leave-management.git
cd leave-management
```

2. Create environment files:
```bash
# Backend (.env)
cp backend/.env.example backend/.env
# Frontend (.env)
cp frontend/.env.example frontend/.env
```

3. Update environment variables:
```bash
# Backend (.env)
POSTGRES_URL=jdbc:postgresql://postgres:5432/leave_management
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000
SENDGRID_API_KEY=your_sendgrid_key
SENDGRIS_FROM_EMAIL=your_verified_email
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/oauth2/callback/google
FRONTEND_URL=http://localhost:4200

# Frontend (.env)
REACT_APP_API_URL=http://localhost:8080
REACT_APP_GOOGLE_CLIENT_ID=your_client_id
```

4. Start the application:
```bash
docker-compose up -d
```

The application will be available at:
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Manual Setup

### Backend Setup

1. Navigate to backend directory:
```bash
cd backend
```

2. Build the application:
```bash
./mvnw clean package
```

3. Run the application:
```bash
java -jar target/leave-management-0.0.1-SNAPSHOT.jar
```

### Frontend Setup

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

## API Documentation

The API documentation is available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Security Features

- JWT-based authentication
- Google OAuth2 integration
- Role-based access control
- CSRF protection
- CORS configuration

## Database Schema

The system uses PostgreSQL with the following main tables:
- users
- roles
- leave_types
- leave_applications
- leave_balances
- leave_workflows
- notifications

## Testing

### Backend Tests
```bash
cd backend
./mvnw test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## Deployment

### Docker Deployment
1. Build the images:
```bash
docker-compose build
```

2. Push to Docker Hub:
```bash
docker push yourusername/leave-management-backend:latest
docker push yourusername/leave-management-frontend:latest
```

### Production Deployment
1. Update environment variables for production
2. Use HTTPS
3. Configure proper CORS settings
4. Set up proper logging
5. Configure backup strategy

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, please contact [your-email@example.com] 