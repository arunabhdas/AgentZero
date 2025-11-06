# AgentZero - Deployment Guide

## Overview

This guide covers deploying the AgentZero backend API service to various environments.

The application is built as a Spring Boot REST API that can be deployed as a standalone JAR, Docker container, or to cloud platforms.

## Prerequisites

### System Requirements
- **JDK**: Java 17 or higher
- **Memory**: Minimum 512MB RAM, recommended 1GB+
- **Storage**: 100MB for application, additional space for logs
- **Network**: Port 8090 (default, configurable)

### External Dependencies
- **MongoDB**: Version 4.4 or higher
  - Can be self-hosted, MongoDB Atlas, or other managed service
  - Requires network access from application server
- **Environment Variables**: Secure storage for secrets

## Configuration

### Required Environment Variables

```bash
# MongoDB connection string
MONGODB_CONNECTION_STRING=mongodb://username:password@host:port/database

# JWT secret (base64-encoded 32-byte secret)
JWT_SECRET_BASE64=<your-base64-secret>

# Optional: Server port (defaults to 8090)
SERVER_PORT=8090
```

### Generating JWT Secret

Generate a secure JWT secret:

```bash
# Using OpenSSL (recommended)
openssl rand -base64 32

# Using Python
python3 -c "import secrets; import base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"

# Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

**Important**: Store this secret securely. Changing it will invalidate all existing tokens.

### MongoDB Setup

#### Option 1: MongoDB Atlas (Cloud - Recommended for Production)

1. Create account at [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Create a new cluster
3. Create database user with read/write permissions
4. Whitelist application server IP addresses
5. Get connection string from Atlas dashboard
6. Format: `mongodb+srv://username:password@cluster.mongodb.net/agentzero?retryWrites=true&w=majority`

#### Option 2: Self-Hosted MongoDB

```bash
# Install MongoDB
# Ubuntu/Debian
sudo apt-get install mongodb

# macOS
brew install mongodb-community

# Start MongoDB service
sudo systemctl start mongodb

# Create database and user
mongosh
> use agentzero
> db.createUser({
    user: "agentzero",
    pwd: "secure_password",
    roles: [{ role: "readWrite", db: "agentzero" }]
  })
```

Connection string: `mongodb://agentzero:secure_password@localhost:27017/agentzero`

#### Option 3: Docker MongoDB

```bash
# Run MongoDB in Docker
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=secure_password \
  -v mongodb_data:/data/db \
  mongo:7

# Connection string
MONGODB_CONNECTION_STRING=mongodb://admin:secure_password@localhost:27017/agentzero?authSource=admin
```

## Building the Application

### Build JAR File

```bash
cd agentzeroapp

# Build with Gradle wrapper (recommended)
./gradlew clean bootJar

# Output location
# build/libs/agentzeroapp-0.0.1-SNAPSHOT.jar
```

The resulting JAR is a "fat JAR" containing all dependencies and can run standalone.

### Build Options

```bash
# Build without running tests (faster)
./gradlew bootJar -x test

# Build with custom version
./gradlew bootJar -Pversion=1.0.0

# Build for production (optimized)
./gradlew bootJar --build-cache --parallel
```

## Deployment Methods

### Method 1: Standalone JAR (Simple)

**Best for**: Development, small deployments, VPS hosting

#### Steps

1. **Transfer JAR to server**
```bash
scp build/libs/agentzeroapp-0.0.1-SNAPSHOT.jar user@server:/opt/agentzero/
```

2. **Set environment variables**
```bash
# Create environment file
sudo nano /opt/agentzero/.env
```

Add:
```
MONGODB_CONNECTION_STRING=mongodb://...
JWT_SECRET_BASE64=...
SERVER_PORT=8090
```

3. **Run application**
```bash
cd /opt/agentzero
java -jar agentzeroapp-0.0.1-SNAPSHOT.jar
```

4. **Run with custom JVM options**
```bash
java -Xmx512m -Xms256m \
  -Dserver.port=8090 \
  -jar agentzeroapp-0.0.1-SNAPSHOT.jar
```

5. **Run as background service**
```bash
nohup java -jar agentzeroapp-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

#### Create systemd Service (Linux)

Create `/etc/systemd/system/agentzero.service`:

```ini
[Unit]
Description=AgentZero API Service
After=network.target

[Service]
Type=simple
User=agentzero
WorkingDirectory=/opt/agentzero
EnvironmentFile=/opt/agentzero/.env
ExecStart=/usr/bin/java -jar /opt/agentzero/agentzeroapp-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable agentzero
sudo systemctl start agentzero
sudo systemctl status agentzero
```

### Method 2: Docker Container

**Best for**: Container orchestration, consistent environments, cloud deployments

#### Dockerfile

Create `Dockerfile` in project root:

```dockerfile
# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S agentzero && \
    adduser -u 1001 -S agentzero -G agentzero

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership
RUN chown -R agentzero:agentzero /app

# Switch to non-root user
USER agentzero

# Expose port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8090/auth/login || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Build and Run

```bash
# Build image
docker build -t agentzero-api:latest .

# Run container
docker run -d \
  --name agentzero-api \
  -p 8090:8090 \
  -e MONGODB_CONNECTION_STRING="mongodb://..." \
  -e JWT_SECRET_BASE64="..." \
  --restart unless-stopped \
  agentzero-api:latest

# View logs
docker logs -f agentzero-api
```

#### Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:7
    container_name: agentzero-mongodb
    restart: unless-stopped
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
    volumes:
      - mongodb_data:/data/db
    networks:
      - agentzero-network

  api:
    build: ./agentzeroapp
    container_name: agentzero-api
    restart: unless-stopped
    ports:
      - "8090:8090"
    environment:
      MONGODB_CONNECTION_STRING: mongodb://admin:${MONGO_PASSWORD}@mongodb:27017/agentzero?authSource=admin
      JWT_SECRET_BASE64: ${JWT_SECRET_BASE64}
    depends_on:
      - mongodb
    networks:
      - agentzero-network

volumes:
  mongodb_data:

networks:
  agentzero-network:
    driver: bridge
```

Create `.env` file:
```
MONGO_PASSWORD=secure_mongodb_password
JWT_SECRET_BASE64=your_base64_secret
```

Run:
```bash
docker-compose up -d
```

### Method 3: Cloud Platforms

#### AWS Elastic Beanstalk

1. **Package application**
```bash
./gradlew bootJar
cd build/libs
zip agentzero.zip agentzeroapp-0.0.1-SNAPSHOT.jar
```

2. **Create application**
```bash
eb init -p "Corretto 17" agentzero-api
eb create agentzero-prod
```

3. **Set environment variables**
```bash
eb setenv \
  MONGODB_CONNECTION_STRING="mongodb://..." \
  JWT_SECRET_BASE64="..."
```

4. **Deploy**
```bash
eb deploy
```

#### Google Cloud Platform (Cloud Run)

1. **Create Dockerfile** (see Docker section above)

2. **Build and push to GCR**
```bash
gcloud builds submit --tag gcr.io/PROJECT_ID/agentzero-api

# Or using Artifact Registry
gcloud builds submit --tag REGION-docker.pkg.dev/PROJECT_ID/agentzero/api
```

3. **Deploy to Cloud Run**
```bash
gcloud run deploy agentzero-api \
  --image gcr.io/PROJECT_ID/agentzero-api \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars MONGODB_CONNECTION_STRING="mongodb://..." \
  --set-env-vars JWT_SECRET_BASE64="..."
```

#### Azure App Service

1. **Package as JAR**
```bash
./gradlew bootJar
```

2. **Create App Service**
```bash
az webapp create \
  --resource-group agentzero-rg \
  --plan agentzero-plan \
  --name agentzero-api \
  --runtime "JAVA:17-java17"
```

3. **Configure environment**
```bash
az webapp config appsettings set \
  --resource-group agentzero-rg \
  --name agentzero-api \
  --settings \
    MONGODB_CONNECTION_STRING="mongodb://..." \
    JWT_SECRET_BASE64="..."
```

4. **Deploy**
```bash
az webapp deploy \
  --resource-group agentzero-rg \
  --name agentzero-api \
  --src-path build/libs/agentzeroapp-0.0.1-SNAPSHOT.jar
```

#### Heroku

1. **Create Procfile**
```
web: java -Dserver.port=$PORT -jar build/libs/agentzeroapp-0.0.1-SNAPSHOT.jar
```

2. **Deploy**
```bash
heroku create agentzero-api
heroku config:set MONGODB_CONNECTION_STRING="mongodb://..."
heroku config:set JWT_SECRET_BASE64="..."
git push heroku main
```

### Method 4: Kubernetes

#### Kubernetes Manifests

Create `k8s/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agentzero-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: agentzero-api
  template:
    metadata:
      labels:
        app: agentzero-api
    spec:
      containers:
      - name: api
        image: agentzero-api:latest
        ports:
        - containerPort: 8090
        env:
        - name: MONGODB_CONNECTION_STRING
          valueFrom:
            secretKeyRef:
              name: agentzero-secrets
              key: mongodb-uri
        - name: JWT_SECRET_BASE64
          valueFrom:
            secretKeyRef:
              name: agentzero-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /auth/login
            port: 8090
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /auth/login
            port: 8090
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: agentzero-api
spec:
  type: LoadBalancer
  selector:
    app: agentzero-api
  ports:
  - port: 80
    targetPort: 8090
```

Create secrets:
```bash
kubectl create secret generic agentzero-secrets \
  --from-literal=mongodb-uri="mongodb://..." \
  --from-literal=jwt-secret="..."
```

Deploy:
```bash
kubectl apply -f k8s/deployment.yaml
```

## Client Consumption

The API can be consumed by various client types:

### 1. Web Browsers (JavaScript/TypeScript)

**Supported Technologies**:
- React, Vue.js, Angular, Svelte
- Next.js, Nuxt.js
- Vanilla JavaScript

**Example** (React with Axios):
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8090',
});

// Login
const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  const { accessToken, refreshToken } = response.data;
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
};

// Make authenticated request
const getNotes = async () => {
  const accessToken = localStorage.getItem('accessToken');
  const response = await api.get('/notes', {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  return response.data;
};
```

### 2. Mobile Applications

**Supported Platforms**:
- **iOS**: Swift with URLSession, Alamofire
- **Android**: Kotlin with Retrofit, OkHttp
- **Cross-platform**: React Native, Flutter, Ionic

**Example** (Flutter):
```dart
import 'package:http/http.dart' as http;
import 'dart:convert';

class ApiService {
  static const String baseUrl = 'http://localhost:8090';

  Future<Map<String, dynamic>> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email, 'password': password}),
    );
    return jsonDecode(response.body);
  }

  Future<List<dynamic>> getNotes(String accessToken) async {
    final response = await http.get(
      Uri.parse('$baseUrl/notes'),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    return jsonDecode(response.body);
  }
}
```

### 3. Desktop Applications

**Supported Frameworks**:
- Electron (JavaScript/TypeScript)
- JavaFX (Java/Kotlin)
- .NET (C#)
- Qt (C++/Python)

### 4. CLI Tools & Scripts

**Languages**:
- Python (requests library)
- Shell scripts (curl)
- Node.js
- Go

**Example** (Python):
```python
import requests

BASE_URL = "http://localhost:8090"

def login(email, password):
    response = requests.post(f"{BASE_URL}/auth/login",
        json={"email": email, "password": password})
    return response.json()

def get_notes(access_token):
    headers = {"Authorization": f"Bearer {access_token}"}
    response = requests.get(f"{BASE_URL}/notes", headers=headers)
    return response.json()
```

### 5. API Testing Tools

- **Postman**: Import OpenAPI/Swagger spec
- **Insomnia**: REST client
- **cURL**: Command-line testing
- **HTTPie**: Modern CLI HTTP client

## CORS Configuration

For web clients from different domains, you may need to enable CORS. Add to `SecurityConfig.kt`:

```kotlin
@Bean
fun corsConfigurationSource(): CorsConfigurationSource {
    val configuration = CorsConfiguration()
    configuration.allowedOrigins = listOf("http://localhost:3000", "https://yourdomain.com")
    configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
    configuration.allowedHeaders = listOf("*")
    configuration.allowCredentials = true

    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    return source
}
```

## Reverse Proxy Setup (Production)

### Nginx Configuration

```nginx
server {
    listen 80;
    server_name api.agentzero.com;

    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.agentzero.com;

    ssl_certificate /etc/letsencrypt/live/api.agentzero.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.agentzero.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Security Best Practices

1. **Never expose environment variables** in version control
2. **Use HTTPS in production** (SSL/TLS certificates)
3. **Keep JWT secret secure** and rotate periodically
4. **Implement rate limiting** at reverse proxy level
5. **Use strong MongoDB passwords** and restrict network access
6. **Keep dependencies updated** regularly
7. **Monitor logs** for security events
8. **Implement backup strategy** for MongoDB
9. **Use firewall rules** to restrict access
10. **Enable MongoDB authentication** and encryption at rest

## Monitoring & Logging

### Application Logs

Logs are written to STDOUT by default. Configure logging in `application.properties`:

```properties
logging.level.root=INFO
logging.level.app.agentzero=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Health Checks

Basic health endpoint can be added for monitoring:

```bash
# Check if service is responding
curl http://localhost:8090/auth/login
```

### Monitoring Tools

- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **ELK Stack**: Centralized logging
- **New Relic / DataDog**: APM solutions

## Troubleshooting

### Application Won't Start

1. Check Java version: `java -version`
2. Verify environment variables are set
3. Check MongoDB connectivity
4. Review application logs
5. Verify port 8090 is available

### MongoDB Connection Issues

```bash
# Test MongoDB connection
mongosh "mongodb://user:pass@host:port/database"

# Check network connectivity
telnet mongodb-host 27017
```

### JWT Token Issues

- Verify JWT_SECRET_BASE64 is correctly set
- Ensure secret hasn't changed (invalidates tokens)
- Check token expiration times
- Validate token format with jwt.io

## Scaling Considerations

### Horizontal Scaling

- **Stateless design** allows multiple instances
- Use load balancer (nginx, HAProxy, cloud LB)
- Share MongoDB connection (consider connection pooling)
- Sticky sessions not required (JWT-based auth)

### Database Scaling

- **MongoDB sharding** for large datasets
- **Read replicas** for read-heavy workloads
- **Indexes** on frequently queried fields
- **Connection pooling** in application

### Caching Layer

Consider adding Redis for:
- Token validation caching
- User session data
- Frequently accessed notes

## Backup & Disaster Recovery

### MongoDB Backups

```bash
# Manual backup
mongodump --uri="mongodb://..." --out=/backup/$(date +%Y%m%d)

# Restore
mongorestore --uri="mongodb://..." /backup/20250106
```

### Automated Backups

- MongoDB Atlas: Automated backups included
- Self-hosted: Use cron jobs or backup tools
- Recommend: Daily backups, 30-day retention

## Support & Maintenance

- Monitor application logs regularly
- Keep dependencies updated (security patches)
- Review and rotate secrets periodically
- Monitor MongoDB performance and storage
- Set up alerts for critical failures

For additional support, refer to DOCUMENTATION.md or contact the development team.

