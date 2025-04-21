# 🛡️ Leave Management System - Backend

A robust and scalable backend for a **Leave Management System**, built using **Spring Boot** and aligned with **Rwandan Labor Law (2023)**. The system handles leave requests, balances, approvals, and HR workflows.

---

## ✨ Key Features

- ✅ **Leave Application**
  - leave types (Annual, Sick, Maternity, Compassionate, etc)
  - Full-day and half-day support
  - Document uploads

- 🔁 **Approval Workflow**
  - Manager/admin approvals with comments
  - Notifications

- 📊 **Dashboard & Reports**
  - Employee leave summaries
  - Admin calendar view
  - Exportable leave reports

- ⚙️ **Leave Balance Management**
  - Auto-accrual (1.66 days/month)
  - Max carry forward (5 days)
  - Manual adjustment via Admin Panel

- 🔔 **Notifications**
  - Email via SendGrid
  - In-app notifications

- 🔐 **Authentication & Security**
  - JWT authentication
  - Google OAuth2 (can be replaced with Microsoft Auth in future(Production))
  - Role-based access control

---

## 🛠️ Tech Stack

- **Java**
- **Spring Boot**
- **Spring Security (JWT + OAuth2)**
- **Spring Data JPA**
- **PostgreSQL**
- **SendGrid API**
- **Docker & Docker Compose**

---

## 📁 Project Structure

```bash
├── src/
│   ├── main/java/com/valens/lms/
│   │   ├── leave_manment/
│   │   ├── user_manament/
│   │   └── config/
│   └── resources/
│       ├── application.yml
│       └── static/
├── .env.example
├── Dockerfile.backend
└── docker-compose.yml

-- XX
```

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/valens10/jst_leave_management_api.git
cd jst_leave_management_api
```

### 2. Create Environment File

```bash
cp .env.example .env
```

Edit `.env` with your local or Docker configuration:

```env
POSTGRES_URL=jdbc:postgresql://postgres:5432/leave_management
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000
SENDGRID_API_KEY=your_sendgrid_key
SENDGRID_FROM_EMAIL=your_verified_email
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/oauth2/callback/google
FRONTEND_URL=http://localhost:4200
```

---

## 🐳 Run with Docker Compose

### 🔧 Build & Start All Services

```bash
docker-compose up --build -d
```

- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 📚 API Documentation

Access detailed interactive documentation:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🔐 Security Overview

- ✅ JWT Authentication
- ✅ OAuth2 via Google
- ✅ Role-Based Access (Admin, Staff, Manager)
- ✅ CORS whitelist setup

---

## 📦 Manual Build (Without Docker)

```bash
# Build Spring Boot app
.\mvnw.cmd spring-boot:run
Ensure PostgreSQL is running and configured to match the `.env` or `application.yml`.

---

## 📂 Database Schema Overview

Main tables include:

- `users`
- `roles`
- `leave_applications`
- `leave_balances`
- `leave_types`
- `notifications`
- `approval_workflows`
- etc

---

## 🚢 Deployment & Production Tips

1. Set environment variables for production
3. Harden your `application.yml`
5. Set up automatic backups (PostgreSQL)

---

## 🤝 Contributing

1. Fork this repository
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

---