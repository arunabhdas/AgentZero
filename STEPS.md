# AgentZero - Local Setup and Testing Guide

This guide provides step-by-step instructions to run and test the AgentZero application on your local machine.

## Prerequisites Checklist

Before starting, ensure you have the following installed:

- [ ] Java Development Kit (JDK) 17 or higher
- [ ] MongoDB (local installation, Docker, or MongoDB Atlas account)
- [ ] Terminal/Command Prompt access
- [ ] Text editor (optional, for viewing files)
- [ ] cURL or Postman (for API testing)

---

## Step 1: Verify Java Installation

### Check if Java is Installed

Open your terminal and run:

```bash
java -version
```

**Expected Output** (version 17 or higher):
```
openjdk version "17.0.x" 2023-xx-xx
OpenJDK Runtime Environment (build 17.0.x+x)
OpenJDK 64-Bit Server VM (build 17.0.x+x, mixed mode)
```

### Install Java (if needed)

**macOS**:
```bash
brew install openjdk@17
```

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

**Windows**:
- Download from [Adoptium](https://adoptium.net/)
- Install and add to PATH

### Verify Installation
```bash
java -version
javac -version
```

---

## Step 2: Setup MongoDB

You have three options: MongoDB Atlas (cloud, recommended if you already have an account), Docker (recommended for local), or native installation.

### Option A: MongoDB with Docker (Recommended)

**Step 2.1**: Install Docker Desktop
- Download from [docker.com](https://www.docker.com/products/docker-desktop)
- Install and start Docker Desktop

**Step 2.2**: Start MongoDB Container
```bash
docker run -d \
  --name agentzero-mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=admin123 \
  -v agentzero-mongo-data:/data/db \
  mongo:7
```

**Step 2.3**: Verify MongoDB is Running
```bash
docker ps
```

You should see `agentzero-mongodb` in the list.

**Step 2.4**: Your MongoDB Connection String
```
mongodb://admin:admin123@localhost:27017/agentzero?authSource=admin
```

**To Stop MongoDB Later**:
```bash
docker stop agentzero-mongodb
```

**To Start MongoDB Again**:
```bash
docker start agentzero-mongodb
```

**To Remove MongoDB Container** (if you want to start fresh):
```bash
docker stop agentzero-mongodb
docker rm agentzero-mongodb
docker volume rm agentzero-mongo-data
```

### Option B: Native MongoDB Installation

**macOS**:
```bash
brew tap mongodb/brew
brew install mongodb-community@7.0
brew services start mongodb-community@7.0
```

**Ubuntu/Debian**:
```bash
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org
sudo systemctl start mongod
```

**Windows**:
- Download from [MongoDB Download Center](https://www.mongodb.com/try/download/community)
- Install and start MongoDB service

**Verify MongoDB is Running**:
```bash
mongosh --eval "db.version()"
```

**Your MongoDB Connection String** (native):
```
mongodb://localhost:27017/agentzero
```

---

### Option C: MongoDB Atlas (Cloud-Hosted Database)

If you already have a MongoDB Atlas account with an existing database, use this option. No local installation needed!

**Step 2.1**: Log into MongoDB Atlas
- Visit [https://cloud.mongodb.com/](https://cloud.mongodb.com/)
- Sign in with your credentials (email/password, Google, or GitHub)

**Step 2.2**: Locate Your Cluster
- Click **"Database"** in the left sidebar
- You should see your existing cluster(s) listed

**Step 2.3**: Get Your Connection String
- Click the **"Connect"** button on your cluster
- Select **"Connect your application"**
- Choose **"Driver"** as connection method
- Select **"Driver: Java"** and **"Version: 4.3 or later"**
- Copy the connection string (it will look like):
  ```
  mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
  ```

**Step 2.4**: Find Your Database Credentials

If you don't remember your database user credentials:
1. Go to **"Database Access"** in the left sidebar
2. You'll see your database user(s) listed
3. If you need to reset the password:
   - Click **"Edit"** on your database user
   - Click **"Edit Password"**
   - Enter a new password and save it
   - Click **"Update User"**

**Step 2.5**: Complete Your Connection String

Your final connection string should include:
- Replace `<username>` with your database username
- Replace `<password>` with your database password
- Add `/agentzero` (database name) before the `?` character

**Example transformation**:
```
Before: mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
After:  mongodb+srv://myuser:mypassword@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority
```

**Step 2.6**: Whitelist Your IP Address

MongoDB Atlas requires you to whitelist IP addresses for security:
1. In Atlas, go to **"Network Access"** in the left sidebar
2. Click **"Add IP Address"**
3. Choose one of:
   - **"Add Current IP Address"** (recommended for security)
   - **"Allow Access from Anywhere"** (easier for development, less secure)
4. Click **"Confirm"**

**Step 2.7**: Verify Connection (Optional)

You can test your connection string using MongoDB Shell:
```bash
mongosh "mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority"
```

**Your MongoDB Connection String** (Atlas):
```
mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority
```

**Benefits of MongoDB Atlas**:
- ✅ Already set up and running (if you have existing database)
- ✅ No local installation or Docker needed
- ✅ Accessible from anywhere with internet
- ✅ Automatic backups included
- ✅ Free tier available (512MB storage)
- ✅ Built-in monitoring and performance insights
- ✅ Managed service (no maintenance required)

**Note**: Since you're using an existing Atlas database, you can skip Docker/native installation and jump directly to Step 3!

---

## Step 3: Generate JWT Secret

The application requires a secure JWT secret key.

**Step 3.1**: Generate the secret using OpenSSL:

```bash
openssl rand -base64 32
```

**Example Output**:
```
Ky8ZJ9X2vN5pQ7mR4tW6yB8cE1fH3gK0L9nM5oP2qS=
```

**Step 3.2**: Copy this value - you'll need it in the next step.

⚠️ **Important**: Keep this secret secure and never commit it to version control!

---

## Step 4: Configure Environment Variables

**Step 4.1**: Navigate to the application directory:

```bash
cd /Users/coder/repos/ad/githubrepos/AgentZero/agentzeroapp
```

**Step 4.2**: Create a `.env` file in the `agentzeroapp` directory:

```bash
# Create the file
touch .env
```

**Step 4.3**: Open `.env` in a text editor and add the following:

**If using MongoDB Atlas (from Step 2, Option C) - Cloud Database**:
```bash
MONGODB_CONNECTION_STRING=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority
JWT_SECRET_BASE64=YOUR_GENERATED_SECRET_HERE
```

**If using Docker MongoDB (from Step 2, Option A)**:
```bash
MONGODB_CONNECTION_STRING=mongodb://admin:admin123@localhost:27017/agentzero?authSource=admin
JWT_SECRET_BASE64=YOUR_GENERATED_SECRET_HERE
```

**If using native MongoDB (from Step 2, Option B)**:
```bash
MONGODB_CONNECTION_STRING=mongodb://localhost:27017/agentzero
JWT_SECRET_BASE64=YOUR_GENERATED_SECRET_HERE
```

**Step 4.4**: Replace `YOUR_GENERATED_SECRET_HERE` with the secret from Step 3.

**Example `.env` file (MongoDB Atlas)**:
```bash
MONGODB_CONNECTION_STRING=mongodb+srv://myuser:mypassword@cluster0.abc123.mongodb.net/agentzero?retryWrites=true&w=majority
JWT_SECRET_BASE64=Ky8ZJ9X2vN5pQ7mR4tW6yB8cE1fH3gK0L9nM5oP2qS=
```

**Example `.env` file (Docker)**:
```bash
MONGODB_CONNECTION_STRING=mongodb://admin:admin123@localhost:27017/agentzero?authSource=admin
JWT_SECRET_BASE64=Ky8ZJ9X2vN5pQ7mR4tW6yB8cE1fH3gK0L9nM5oP2qS=
```

**Step 4.5**: Export the environment variables in your terminal:

**macOS/Linux (MongoDB Atlas)**:
```bash
export MONGODB_CONNECTION_STRING="mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority"
export JWT_SECRET_BASE64="YOUR_GENERATED_SECRET_HERE"
```

**macOS/Linux (Docker)**:
```bash
export MONGODB_CONNECTION_STRING="mongodb://admin:admin123@localhost:27017/agentzero?authSource=admin"
export JWT_SECRET_BASE64="YOUR_GENERATED_SECRET_HERE"
```

**Windows (Command Prompt)**:
```cmd
set MONGODB_CONNECTION_STRING=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority
set JWT_SECRET_BASE64=YOUR_GENERATED_SECRET_HERE
```

**Windows (PowerShell)**:
```powershell
$env:MONGODB_CONNECTION_STRING="mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority"
$env:JWT_SECRET_BASE64="YOUR_GENERATED_SECRET_HERE"
```

---

## Step 5: Build the Application

**Step 5.1**: Make sure you're in the `agentzeroapp` directory:

```bash
cd /Users/coder/repos/ad/githubrepos/AgentZero/agentzeroapp
pwd  # Should show: /Users/coder/repos/ad/githubrepos/AgentZero/agentzeroapp
```

**Step 5.2**: Make the Gradle wrapper executable (macOS/Linux):

```bash
chmod +x gradlew
```

**Step 5.3**: Build the application:

```bash
./gradlew clean build

OR

./gradlew clean build -x test
```

**Windows**:
```cmd
gradlew.bat clean build
```

**Expected Output**:
```
BUILD SUCCESSFUL in Xs
```

**Note**: The first build may take 2-5 minutes as it downloads dependencies.

**If build fails**, check:
- Java 17 is installed and active
- You're in the correct directory (`agentzeroapp`)
- Internet connection is working (for dependency download)

---

## Step 6: Run the Application

**Step 6.1**: Start the application using Gradle:

```bash
# Set the environment variables from your .env file
export MONGODB_CONNECTION_STRING="mongodb+srv://your-username:your-password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority"
export JWT_SECRET_BASE64="your-jwt-secret-from-step-3"

# Then run the application
./gradlew bootRun
```

**Windows**:
```cmd
gradlew.bat bootRun
```

**Expected Output**:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.4.5)

... (startup logs) ...

2025-01-06 10:30:00 - Started AgentZeroAppApplication in X.XXX seconds
```

**Step 6.2**: Verify the application is running:

Open a new terminal and run:
```bash
curl http://localhost:8090/auth/login
```

**Expected Response** (405 Method Not Allowed is OK - means the server is running):
```json
{"timestamp":"...","status":405,"error":"Method Not Allowed"...}
```

**Application is now running on**: `http://localhost:8090`

---

## Step 7: Test the API

Now let's test the API endpoints step by step.

### Test 7.1: Register a New User

Open a new terminal (keep the application running) and run:

```bash
curl -X POST http://localhost:8090/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "SecurePassword123"
  }'
```

**Expected Response**: `200 OK` (no body)

**If you see an error**, check:
- Application is still running
- MongoDB is running
- Environment variables are set correctly

---

### Test 7.2: Login and Get Tokens

```bash
curl -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "SecurePassword123"
  }'
```

**Expected Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Step 7.2.1**: Copy the `accessToken` value. You'll need it for the next tests.

**Step 7.2.2**: Save it as an environment variable for easier testing:

**macOS/Linux**:
```bash
export ACCESS_TOKEN="paste-your-access-token-here"
```

**Windows (PowerShell)**:
```powershell
$ACCESS_TOKEN="paste-your-access-token-here"
```

---

### Test 7.3: Create a Note

```bash
curl -X POST http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": null,
    "title": "My First Note",
    "content": "This is a test note created from the API",
    "color": "#FFD700"
  }'
```

**Expected Response**:
```json
{
  "id": "507f1f77bcf86cd799439011",
  "title": "My First Note",
  "content": "This is a test note created from the API",
  "color": "#FFD700",
  "createdAt": "2025-01-06T10:30:00Z"
}
```

**Step 7.3.1**: Copy the `id` value from the response.

---

### Test 7.4: Get All Notes

```bash
curl -X GET http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response**:
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "title": "My First Note",
    "content": "This is a test note created from the API",
    "color": "#FFD700",
    "createdAt": "2025-01-06T10:30:00Z"
  }
]
```

---

### Test 7.5: Update a Note

Replace `NOTE_ID` with the actual ID from Test 7.3:

```bash
curl -X POST http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "507f1f77bcf86cd799439011",
    "title": "Updated Note Title",
    "content": "This note has been updated!",
    "color": "#FF6347"
  }'
```

**Expected Response**: Updated note with new values.

---

### Test 7.6: Delete a Note

Replace `NOTE_ID` with the actual ID:

```bash
curl -X DELETE http://localhost:8090/notes/507f1f77bcf86cd799439011 \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected Response**: `200 OK` (no body)

---

### Test 7.7: Test Token Refresh

First, save your refresh token from Test 7.2:

```bash
export REFRESH_TOKEN="paste-your-refresh-token-here"
```

Then refresh:

```bash
curl -X POST http://localhost:8090/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**Expected Response**: New access and refresh tokens.

---

### Test 7.8: Logout

```bash
curl -X POST http://localhost:8090/auth/logout \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**Expected Response**:
```json
{
  "success": true
}
```

---

## Step 8: Testing with Postman (Alternative)

If you prefer a GUI, use Postman with our pre-built collection:

**Step 8.1**: Download and install [Postman](https://www.postman.com/downloads/)

**Step 8.2**: Import the AgentZero Collection

1. Open Postman
2. Click **Import** button (top left)
3. Select **File** tab
4. Choose `AgentZero-Postman-Collection.json` from the repository root
5. Click **Import**

**Step 8.3**: Configure and Test

The collection includes:
- ✅ **Automatic token management** - Login saves tokens automatically
- ✅ **All API endpoints** - Auth (register, login, refresh, logout) and Notes (CRUD)
- ✅ **Built-in tests** - Validates responses automatically
- ✅ **Pre-configured variables** - Ready to use

**Quick Test Flow**:
1. Run **Authentication** → **Register**
2. Run **Authentication** → **Login** (tokens saved automatically!)
3. Run **Notes** → **Create Note**
4. Run **Notes** → **Get All Notes**
5. Run **Notes** → **Update Note**
6. Run **Notes** → **Delete Note**

**📚 For detailed Postman usage, see `POSTMAN-GUIDE.md`**

**Manual Setup (Alternative)**:

If you prefer to create your own collection:

1. Create a new Collection called "AgentZero"
2. Add collection variables: `baseUrl`, `accessToken`, `refreshToken`
3. Add requests for each endpoint (see USAGE.md for details)
4. Configure Bearer token authentication using `{{accessToken}}`

---

## Step 9: Verify MongoDB Data

You can verify that data is being saved to MongoDB:

**Using Docker**:
```bash
docker exec -it agentzero-mongodb mongosh -u admin -p admin123 --authenticationDatabase admin
```

**Native MongoDB**:
```bash
mongosh
```

**Then in MongoDB shell**:
```javascript
use agentzero
db.users.find().pretty()
db.notes.find().pretty()
db.refresh_tokens.find().pretty()
```

**Exit MongoDB shell**:
```
exit
```

---

## Step 10: Stopping the Application

**Step 10.1**: Stop the Spring Boot application:
- In the terminal where it's running, press `Ctrl + C`

**Step 10.2**: Stop MongoDB (if using Docker):
```bash
docker stop agentzero-mongodb
```

**Step 10.3**: (Optional) Clean up:
```bash
# Remove MongoDB container and data
docker rm agentzero-mongodb
docker volume rm agentzero-mongo-data
```

---

## Troubleshooting

### Problem: "Port 8090 already in use"

**Solution**: Change the port in `application.properties` or kill the process:

```bash
# Find process using port 8090
lsof -i :8090  # macOS/Linux
netstat -ano | findstr :8090  # Windows

# Kill the process
kill -9 PID  # Replace PID with actual process ID
```

### Problem: "Connection refused to MongoDB"

**Solution**:
1. Verify MongoDB is running: `docker ps` or `brew services list`
2. Check connection string in environment variables
3. Restart MongoDB: `docker restart agentzero-mongodb`

### Problem: "Cannot connect to MongoDB Atlas"

**Solution**:
1. **Check IP Whitelist**: Go to Atlas → Network Access → Add your current IP
2. **Verify Credentials**: Ensure username and password are correct in connection string
3. **Check Connection String Format**: Should be `mongodb+srv://...` (with `srv`)
4. **Database Name**: Ensure `/agentzero` is included before the `?`
5. **Test Connection**: Use `mongosh "your-connection-string"` to test
6. **Internet Connection**: MongoDB Atlas requires internet access
7. **Firewall**: Check if your firewall is blocking MongoDB Atlas ports

**Common Atlas Errors**:
- `MongoTimeoutError`: IP not whitelisted or firewall blocking
- `Authentication failed`: Wrong username/password
- `getaddrinfo ENOTFOUND`: Check connection string format

### Problem: "JWT validation failed"

**Solution**:
1. Verify JWT_SECRET_BASE64 is set correctly
2. Make sure you're using the latest access token
3. Check token hasn't expired (15 minutes)
4. Try logging in again to get fresh tokens

### Problem: "401 Unauthorized" when accessing notes

**Solution**:
1. Ensure you're including the Authorization header
2. Check token format: `Bearer <token>` (space after Bearer)
3. Token may have expired - use refresh endpoint or login again

### Problem: Build fails with dependency errors

**Solution**:
```bash
# Clean and rebuild
./gradlew clean
./gradlew --refresh-dependencies
./gradlew build
```

### Problem: Application starts but crashes immediately

**Solution**:
1. Check environment variables are set
2. Verify MongoDB connection
3. Check application logs for specific error
4. Ensure no other instance is running

---

## Quick Reference Commands

### Start Everything (MongoDB Atlas)
```bash
# 1. Set environment variables (MongoDB Atlas is already running in cloud)
export MONGODB_CONNECTION_STRING="mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/agentzero?retryWrites=true&w=majority"
export JWT_SECRET_BASE64="your-secret-here"

# 2. Navigate to app directory
cd /Users/coder/repos/ad/githubrepos/AgentZero/agentzeroapp

# 3. Run application
./gradlew bootRun
```

### Start Everything (Docker MongoDB)
```bash
# 1. Start MongoDB (Docker)
docker start agentzero-mongodb

# 2. Set environment variables
export MONGODB_CONNECTION_STRING="mongodb://admin:admin123@localhost:27017/agentzero?authSource=admin"
export JWT_SECRET_BASE64="your-secret-here"

# 3. Navigate to app directory
cd /Users/coder/repos/ad/githubrepos/AgentZero/agentzeroapp

# 4. Run application
./gradlew bootRun
```

### Test Sequence
```bash
# 1. Register
curl -X POST http://localhost:8090/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# 2. Login and save token
RESPONSE=$(curl -s -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}')
ACCESS_TOKEN=$(echo $RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# 3. Create note
curl -X POST http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":null,"title":"Test","content":"Content","color":"#FFD700"}'

# 4. Get notes
curl -X GET http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### Stop Everything

**If using MongoDB Atlas**:
```bash
# Stop Spring Boot (Ctrl+C in running terminal)
# MongoDB Atlas runs in cloud - no need to stop
```

**If using Docker MongoDB**:
```bash
# 1. Stop Spring Boot (Ctrl+C in running terminal)
# 2. Stop MongoDB
docker stop agentzero-mongodb
```

---

## Next Steps

Now that you have the application running locally:

1. **Explore the API**: Try different combinations of requests
2. **Build a Client**: Create a web or mobile app that consumes this API
3. **Modify the Code**: Add new features or customize existing ones
4. **Read Documentation**: Check DOCUMENTATION.md for architecture details
5. **Deploy**: See DEPLOY.md for production deployment options

---

## Additional Resources

- **API Usage Guide**: See `USAGE.md` for detailed API documentation
- **Token Management Guide**: See `TOKENS.md` for JWT authentication flow and best practices
- **Postman Collection Guide**: See `POSTMAN-GUIDE.md` for GUI testing
- **Developer Documentation**: See `DOCUMENTATION.md` for code architecture
- **Deployment Guide**: See `DEPLOY.md` for production deployment
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **MongoDB Docs**: https://docs.mongodb.com/
- **MongoDB Atlas**: https://cloud.mongodb.com/

---

## Support

If you encounter issues:

1. Check this troubleshooting section
2. Review application logs in the terminal
3. Verify MongoDB status:
   - **Atlas**: Check [https://cloud.mongodb.com/](https://cloud.mongodb.com/) → Clusters
   - **Docker**: `docker logs agentzero-mongodb`
4. Check environment variables: `echo $MONGODB_CONNECTION_STRING`
5. Refer to DOCUMENTATION.md for architecture details

---

**Congratulations!** 🎉 You now have AgentZero running locally and have successfully tested the API endpoints.

