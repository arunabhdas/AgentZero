# AgentZero

Agentic AI Powered CRM built in Spring Boot with Kotlin

## 🚀 Quick Start

1. **Setup & Run Locally**: Follow [STEPS.md](STEPS.md) for step-by-step instructions
2. **Test the API**: Import [AgentZero-Postman-Collection.json](AgentZero-Postman-Collection.json) into Postman

## 📚 Documentation

- **[STEPS.md](STEPS.md)** - Local setup and testing guide (start here!)
- **[TOKENS.md](TOKENS.md)** - JWT token management and authentication flow
- **[POSTMAN-GUIDE.md](POSTMAN-GUIDE.md)** - How to use the Postman collection
- **[USAGE.md](USAGE.md)** - Complete API usage documentation with examples
- **[DOCUMENTATION.md](DOCUMENTATION.md)** - Developer documentation and architecture
- **[DEPLOY.md](DEPLOY.md)** - Production deployment guide

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.4.5 + Kotlin 1.9.25
- **Database**: MongoDB (Atlas or self-hosted)
- **Security**: JWT authentication with BCrypt password hashing
- **Build Tool**: Gradle with Kotlin DSL
- **Java Version**: 17

## ✨ Features

- 🔐 Secure JWT-based authentication (access + refresh tokens)
- 📝 Personal notes management (CRUD operations)
- 🔄 Token refresh and rotation
- 👤 User registration and login
- 🌐 RESTful API design
- 🗄️ MongoDB integration with TTL indexes

## 🔑 Generate JWT Secret

You can generate a secure 32-byte JWT secret using:

```bash
openssl rand -base64 32
```
