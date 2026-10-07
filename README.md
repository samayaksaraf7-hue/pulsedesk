# 🚨 PulseDesk

### Intelligent Incident Prioritization & Team Workload Management Platform

PulseDesk is a full-stack incident management platform designed to help teams prioritize operational incidents, automatically assign work based on team capacity, and track incidents through their complete lifecycle.

The project demonstrates production-oriented backend development using Spring Boot, JWT authentication, PostgreSQL, Redis caching, Kafka event-driven architecture, Flyway migrations, Docker, and a React frontend.

---

## 🌐 Live Application

**Frontend**

https://artistic-mercy-production-15d3.up.railway.app

**Backend Health Check**

https://pulsedesk-production-8369.up.railway.app/health

---

## ✨ Key Features

- Secure JWT-based authentication
- BCrypt password hashing
- Smart incident priority calculation
- Automatic workload-aware assignment
- Incident lifecycle management
- Team capacity tracking
- Redis workload caching
- Kafka-based incident events
- PostgreSQL persistence
- Flyway database migrations
- Responsive React dashboard
- RESTful backend APIs
- Docker-ready backend
- Cloud deployment using Railway

---

## 🧠 Smart Priority Engine

PulseDesk calculates incident priority using operational factors such as:

- Impact
- Urgency
- Number of affected users
- Deadline

Each incident receives a calculated priority score and priority level.

This allows important incidents to surface automatically instead of relying only on manual prioritization.

---

## 👥 Workload-Aware Auto Assignment

PulseDesk automatically assigns incidents to the least-loaded team member who has enough remaining capacity.

The assignment engine:

1. Reads current team workloads.
2. Orders users by workload.
3. Checks available capacity.
4. Selects the least-loaded eligible user.
5. Assigns the incident.
6. Updates the user's workload.
7. Invalidates cached workload data.
8. Publishes an assignment event when Kafka is enabled.

When an incident is resolved, its estimated workload is released from the assigned user.

---

## 🔄 Incident Lifecycle

```text
OPEN
  ↓
IN_PROGRESS
  ↓
RESOLVED
```

Typical workflow:

```text
Login
  ↓
Create Incident
  ↓
Priority Engine
  ↓
Auto Assignment
  ↓
Workload Updated
  ↓
Start Progress
  ↓
Resolve Incident
  ↓
Workload Released
```

---

## 🏗️ Architecture

```text
React + Vite Frontend
        │
        │ HTTPS / REST
        ▼
Spring Boot Backend
        │
        ├── Spring Security + JWT
        │
        ├── Priority Engine
        │
        ├── Assignment Engine
        │
        ├── Redis Cache
        │
        ├── Kafka Events
        │
        └── Flyway Migrations
        │
        ▼
    PostgreSQL
```

---

## 🛠️ Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- Redis
- Apache Kafka
- Flyway
- Maven
- Docker

### Frontend

- React
- Vite
- JavaScript
- CSS
- Fetch API

### Deployment

- Railway
- PostgreSQL cloud database
- Redis cloud service
- GitHub

---

## 🔐 Security

PulseDesk uses stateless JWT authentication.

Security features include:

- JWT authentication
- BCrypt password hashing
- Stateless Spring Security configuration
- Protected API endpoints
- CORS configuration
- Environment-based secrets
- Authorization headers for authenticated requests

Passwords and production secrets are not stored in the repository.

---

## ⚡ Redis Caching

Redis is used to cache workload information.

When assignment or workload-changing operations occur, relevant cache entries are invalidated so subsequent requests receive updated workload information.

---

## 📨 Kafka Integration

Kafka is used for asynchronous incident-related events such as assignment events.

For local development, Kafka is enabled by default.

The public Railway demo uses:

```text
KAFKA_ENABLED=false
```

because the demo deployment does not provision a Kafka broker.

The Kafka producer, consumer, and event-driven integration remain part of the application and can be enabled whenever a Kafka broker is available.

---

## 🗄️ Database

PulseDesk uses PostgreSQL for persistent storage.

Core domain data includes:

- Users
- Teams
- Incidents
- Issue events
- Workload information

Database schema changes are managed using Flyway migrations.

---

## 🔌 Main API Endpoints

### Authentication

```http
POST /api/auth/register
POST /api/auth/login
```

### Incidents

```http
GET   /api/issues
POST  /api/issues
GET   /api/issues/{id}
PATCH /api/issues/{id}/status
POST  /api/issues/{id}/auto-assign
```

### Workloads

```http
GET /api/workloads
```

### Health

```http
GET /health
GET /actuator/health
```

---

## 📁 Project Structure

```text
pulsedesk/
│
├── pulsedesk/
│   ├── src/main/java/com/pulsedesk/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── kafka/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   │
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   └── application.yaml
│   │
│   ├── Dockerfile
│   └── pom.xml
│
├── pulsedesk-frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
│
└── README.md
```

---

## 💻 Running Locally

### Prerequisites

Install:

- Java 21
- Node.js
- PostgreSQL
- Redis
- Apache Kafka
- Git

### Backend

Navigate to:

```bash
cd pulsedesk
```

Configure the required environment variables, including:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
```

Then run:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend runs on:

```text
http://localhost:8080
```

### Frontend

Navigate to:

```bash
cd pulsedesk-frontend
```

Install dependencies:

```bash
npm install
```

Create a `.env` file:

```text
VITE_API_URL=http://localhost:8080
```

Start the frontend:

```bash
npm run dev
```

---

## ☁️ Deployment

PulseDesk is deployed on Railway.

The deployed architecture includes:

```text
React Frontend
      │
      ▼
Spring Boot REST API
      │
      ├── PostgreSQL
      └── Redis
```

Kafka integration is feature-controlled through the `KAFKA_ENABLED` environment variable, allowing Kafka to run locally while remaining disabled on hosting environments without a Kafka broker.

---

## 🎯 What This Project Demonstrates

PulseDesk demonstrates practical implementation of:

- Full-stack application development
- REST API design
- Authentication and authorization
- Relational database design
- Database migrations
- Caching
- Event-driven architecture
- Business-rule implementation
- Workload-aware algorithms
- Environment-based configuration
- Cloud deployment
- Production debugging
- Responsive frontend development

---

## 🚀 Future Improvements

Possible future enhancements include:

- Dedicated managed Kafka deployment
- Role-based administration dashboard
- Email and Slack notifications
- Incident comments and activity timeline
- SLA monitoring
- Advanced analytics
- WebSocket real-time updates
- Kubernetes deployment
- CI/CD pipeline

---

## 👨‍💻 Author

**Samayak Saraf**

GitHub: `samayaksaraf7-hue`

---

## 📌 Project Status

**Deployed and functional**

Core production workflow verified:

```text
Authentication
→ Incident Creation
→ Priority Calculation
→ Auto Assignment
→ Workload Management
→ Start Progress
→ Incident Resolution
```