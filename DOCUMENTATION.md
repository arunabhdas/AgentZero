# AgentZero - Developer Documentation

## Overview

AgentZero is an Agentic AI Powered CRM built with Spring Boot and Kotlin. The current implementation provides a secure REST API backend with JWT-based authentication and a notes management system. The application uses MongoDB for data persistence and implements modern security best practices.

## Technology Stack

### Core Technologies
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.4.5
- **Build Tool**: Gradle with Kotlin DSL
- **JVM Version**: Java 17

### Spring Boot Modules
- `spring-boot-starter-web` - RESTful web services
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-boot-starter-data-mongodb-reactive` - Reactive MongoDB support
- `spring-boot-starter-security` - Security framework
- `spring-boot-starter-validation` - Request validation

### Security & Authentication
- **JWT**: JSON Web Tokens using `io.jsonwebtoken:jjwt` (v0.12.6)
- **Password Hashing**: BCrypt via Spring Security Crypto
- **Token Types**: Access tokens (15 min) and Refresh tokens (30 days)

### Testing
- JUnit 5 Platform
- Spring Boot Test
- Reactor Test (for reactive components)
- Spring Security Test
- Kotlin Coroutines Test

## Project Structure

```
agentzeroapp/
├── src/
│   ├── main/
│   │   ├── kotlin/app/agentzero/agentzeroapp/
│   │   │   ├── AgentZeroAppApplication.kt    # Application entry point
│   │   │   ├── controllers/                  # REST controllers
│   │   │   │   ├── AuthController.kt         # Authentication endpoints
│   │   │   │   └── NoteController.kt         # Note CRUD endpoints
│   │   │   ├── data/
│   │   │   │   ├── model/                    # Data models
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── Note.kt
│   │   │   │   │   └── RefreshToken.kt
│   │   │   │   └── repository/               # MongoDB repositories
│   │   │   │       ├── UserRepository.kt
│   │   │   │       ├── NoteRepository.kt
│   │   │   │       └── RefreshTokenRepository.kt
│   │   │   └── security/                     # Security components
│   │   │       ├── SecurityConfig.kt         # Security configuration
│   │   │       ├── JwtAuthFilter.kt          # JWT authentication filter
│   │   │       ├── JwtService.kt             # JWT token management
│   │   │       ├── AuthService.kt            # Authentication business logic
│   │   │       └── HashEncoder.kt            # Password hashing utility
│   │   └── resources/
│   │       └── application.properties        # Application configuration
│   └── test/                                 # Test files
├── build.gradle.kts                          # Gradle build configuration
└── settings.gradle.kts                       # Gradle settings
```

## Architecture

### Layer Architecture

The application follows a standard layered architecture:

1. **Controller Layer** (`controllers/`)
   - REST endpoints
   - Request/response DTOs
   - Input validation
   - HTTP status mapping

2. **Service Layer** (`security/` for business logic)
   - Business logic implementation
   - Transaction management
   - Authentication and authorization logic

3. **Repository Layer** (`data/repository/`)
   - Data access abstraction
   - MongoDB query methods
   - Spring Data repositories

4. **Model Layer** (`data/model/`)
   - Domain entities
   - MongoDB document mappings

### Security Architecture

#### JWT Token Flow
1. **Access Token**: Short-lived (15 minutes), used for API authentication
2. **Refresh Token**: Long-lived (30 days), used to obtain new access tokens
3. **Token Storage**: Refresh tokens stored in MongoDB with TTL indexes for automatic cleanup

**📚 For detailed token management, usage examples, and best practices, see [TOKENS.md](TOKENS.md)**

#### Authentication Filter Chain
```
Request → JwtAuthFilter → UsernamePasswordAuthenticationFilter → Controller
```

The `JwtAuthFilter` intercepts all requests, validates JWT tokens, and sets the security context.

## Key Components

### 1. Authentication System (`AuthController` + `AuthService`)

**Endpoints**:
- `POST /auth/register` - Create new user account
- `POST /auth/login` - Authenticate and receive tokens
- `POST /auth/refresh` - Exchange refresh token for new token pair
- `POST /auth/logout` - Invalidate refresh token

**Features**:
- BCrypt password hashing
- JWT token generation and validation
- Refresh token rotation (one-time use)
- Automatic token cleanup via MongoDB TTL indexes

### 2. Notes Management (`NoteController`)

**Endpoints**:
- `POST /notes` - Create or update note (upsert)
- `GET /notes` - Get all notes for authenticated user
- `DELETE /notes/{id}` - Delete note by ID

**Features**:
- User-scoped data (users can only access their own notes)
- Automatic owner validation
- Color-coded notes
- Timestamp tracking

### 3. Security Configuration (`SecurityConfig`)

**Configuration**:
- Stateless session management (JWT-based)
- CSRF protection disabled (appropriate for stateless REST APIs)
- Public endpoints: `/auth/**`
- Protected endpoints: Everything else
- Custom JWT filter before default authentication

### 4. JWT Service (`JwtService`)

**Responsibilities**:
- Generate access and refresh tokens
- Validate token format and expiration
- Extract user ID from tokens
- Token type differentiation (access vs refresh)

**Token Claims**:
- `sub`: User ID (ObjectId as hex string)
- `type`: Token type ("access" or "refresh")
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp

### 5. Data Models

#### User
```kotlin
@Document("users")
data class User(
    val email: String,
    val hashedPassword: String,
    @Id val id: ObjectId = ObjectId()
)
```

#### Note
```kotlin
@Document("notes")
data class Note(
    @Id val id: ObjectId = ObjectId.get(),
    val title: String,
    val content: String,
    val color: String,
    val createdAt: Instant,
    val ownerId: ObjectId
)
```

#### RefreshToken
```kotlin
@Document("refresh_tokens")
data class RefreshToken(
    val userId: ObjectId,
    @Indexed(expireAfter = "0s") val expiresAt: Instant,
    val token: String,
    val createdAt: Instant = Instant.now()
)
```

## Security Implementation

### Password Security
- **Algorithm**: BCrypt with default strength (10 rounds)
- **Salt**: Automatically generated per password
- **Storage**: Only hashed passwords stored, never plaintext

### Token Security
- **Algorithm**: HMAC-SHA256 (HS256)
- **Secret**: Base64-encoded secret from environment variable
- **Rotation**: Refresh tokens are one-time use (deleted after refresh)
- **Cleanup**: Expired tokens automatically deleted via MongoDB TTL

### API Security
- All endpoints except `/auth/**` require valid JWT access token
- Tokens passed in `Authorization: Bearer <token>` header
- User context extracted from token claims
- Resource ownership validated (users can only access their own data)

## Database Schema

### Collections

**users**
- `_id`: ObjectId (primary key)
- `email`: String (unique, used for login)
- `hashedPassword`: String (BCrypt hash)

**notes**
- `_id`: ObjectId (primary key)
- `title`: String
- `content`: String
- `color`: String (hex color code)
- `createdAt`: Instant
- `ownerId`: ObjectId (foreign key to users)

**refresh_tokens**
- `_id`: ObjectId (primary key)
- `userId`: ObjectId (foreign key to users)
- `token`: String (JWT refresh token)
- `expiresAt`: Instant (TTL index for auto-cleanup)
- `createdAt`: Instant

### Indexes
- `refresh_tokens.expiresAt`: TTL index with `expireAfter = "0s"` for automatic cleanup
- Implicit index on `_id` for all collections

## Development Setup

### Prerequisites
- JDK 17 or higher
- MongoDB instance (local or remote)
- Gradle (included via wrapper)

### Environment Variables
Create a `.env` file or set these environment variables:

```bash
MONGODB_CONNECTION_STRING=mongodb://localhost:27017/agentzero
JWT_SECRET_BASE64=<base64-encoded-32-byte-secret>
```

Generate JWT secret:
```bash
openssl rand -base64 32
```

### Building the Application

```bash
# Build the application
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew bootRun
```

### IDE Setup

**IntelliJ IDEA** (Recommended):
1. Open the `agentzeroapp` directory
2. IntelliJ will auto-detect Gradle project
3. Install Kotlin plugin if not present
4. Set JDK 17 in Project Structure
5. Enable annotation processing

**VS Code**:
1. Install "Kotlin Language" extension
2. Install "Gradle for Java" extension
3. Open the `agentzeroapp` directory

## Testing

### Test Structure
```
src/test/kotlin/app/agentzero/agentzeroapp/
└── AgentZeroAppApplicationTests.kt
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests AgentZeroAppApplicationTests

# Run with coverage
./gradlew test jacocoTestReport
```

### Test Categories
- **Integration Tests**: Test with Spring context and MongoDB
- **Unit Tests**: Test individual components in isolation
- **Security Tests**: Test authentication and authorization

## Configuration

### Application Properties
Located at `src/main/resources/application.properties`:

```properties
spring.application.name=Agent Zero App
server.port=8090
spring.data.mongodb.uri=${MONGODB_CONNECTION_STRING}
jwt.secret=${JWT_SECRET_BASE64}
```

### JWT Configuration
- Access token validity: 15 minutes (900,000 ms)
- Refresh token validity: 30 days (2,592,000,000 ms)
- Algorithm: HS256 (HMAC-SHA256)

### Server Configuration
- Default port: 8090
- Session: Stateless (no HTTP sessions)
- CSRF: Disabled (JWT-based authentication)

## Common Development Tasks

### Adding a New Endpoint
1. Create/update controller in `controllers/`
2. Add request/response DTOs as data classes
3. Implement business logic in service layer
4. Add tests for new endpoint
5. Update this documentation

### Adding a New Data Model
1. Create model in `data/model/`
2. Add `@Document` annotation with collection name
3. Create repository interface in `data/repository/`
4. Define custom query methods if needed
5. Update database schema documentation

### Modifying Security Rules
1. Update `SecurityConfig.kt` for endpoint permissions
2. Modify `JwtAuthFilter.kt` for token validation logic
3. Update `JwtService.kt` for token generation/validation
4. Test security changes thoroughly

## Code Style & Conventions

### Kotlin Conventions
- Use data classes for DTOs and models
- Prefer immutability (val over var)
- Use nullable types appropriately (`?`)
- Leverage Kotlin's null-safety features
- Use extension functions for clarity

### Naming Conventions
- **Classes**: PascalCase (e.g., `AuthController`)
- **Functions**: camelCase (e.g., `generateToken`)
- **Properties**: camelCase (e.g., `userId`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `ACCESS_TOKEN_VALIDITY`)

### Package Organization
```
app.agentzero.agentzeroapp
├── controllers      # REST endpoints
├── data
│   ├── model       # Domain models
│   └── repository  # Data access
├── security        # Security components
└── config          # Configuration classes (future)
```

## Troubleshooting

### Common Issues

**MongoDB Connection Failed**
- Verify `MONGODB_CONNECTION_STRING` is set correctly
- Check MongoDB service is running
- Verify network connectivity and firewall rules

**JWT Token Validation Failed**
- Ensure `JWT_SECRET_BASE64` matches the secret used to generate tokens
- Verify token hasn't expired
- Check "Bearer " prefix in Authorization header

**Access Denied to Resources**
- Verify access token is valid and not expired
- Check user ID in token matches resource owner
- Ensure proper Authorization header format

**Build Failures**
- Clean build directory: `./gradlew clean`
- Refresh Gradle dependencies: `./gradlew --refresh-dependencies`
- Verify JDK 17 is being used

## Contributing

### Development Workflow
1. Create feature branch from `main`
2. Implement changes with tests
3. Run tests locally: `./gradlew test`
4. Commit with descriptive messages
5. Create pull request for review

### Code Review Checklist
- [ ] All tests pass
- [ ] New code has test coverage
- [ ] No security vulnerabilities introduced
- [ ] Documentation updated
- [ ] Code follows project conventions
- [ ] No commented-out code or TODOs

## Future Enhancements

### Planned Features
- Role-based access control (RBAC)
- Note sharing between users
- Note categories and tags
- Full-text search for notes
- File attachments for notes
- User profile management
- Email verification
- Password reset functionality
- Rate limiting
- API versioning
- Comprehensive audit logging

### Technical Improvements
- Implement caching (Redis)
- Add API documentation (OpenAPI/Swagger)
- Implement request rate limiting
- Add comprehensive logging (ELK stack)
- Set up CI/CD pipeline
- Add monitoring and alerting
- Implement database migrations (Liquibase/Flyway)
- Add integration with AI services for CRM features

## Resources

### Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Spring Security](https://spring.io/projects/spring-security)
- [MongoDB Documentation](https://docs.mongodb.com/)
- [JWT.io](https://jwt.io/)

### Tools
- [Gradle Build Tool](https://gradle.org/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [MongoDB Compass](https://www.mongodb.com/products/compass)
- [Postman](https://www.postman.com/) - API testing

## License

This project is part of the AgentZero CRM system. Refer to the main repository for licensing information.

## Support

For issues, questions, or contributions, please refer to the main AgentZero repository or contact the development team.

