# AgentZero - JWT Token Management Guide

## Overview

AgentZero uses a dual-token JWT (JSON Web Token) authentication system with **access tokens** and **refresh tokens** to provide secure, stateless authentication while maintaining a good user experience.

## Token Types

### Access Token

**Purpose**: Short-lived token used for authenticating API requests.

**Characteristics**:
- **Lifetime**: 15 minutes
- **Usage**: Sent with every API request to protected endpoints
- **Format**: JWT (JSON Web Token)
- **Algorithm**: HS256 (HMAC-SHA256)
- **Claims**:
  - `sub` (subject): User ID (MongoDB ObjectId as hex string)
  - `type`: "access"
  - `iat` (issued at): Token creation timestamp
  - `exp` (expiration): Token expiration timestamp

**Example Structure**:
```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "507f1f77bcf86cd799439011",  // User ID
  "type": "access",
  "iat": 1699876543,
  "exp": 1699877443  // 15 minutes later
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  JWT_SECRET_BASE64
)
```

### Refresh Token

**Purpose**: Long-lived token used to obtain new access tokens without re-authenticating.

**Characteristics**:
- **Lifetime**: 30 days
- **Usage**: Sent to `/auth/refresh` endpoint when access token expires
- **Format**: JWT (JSON Web Token)
- **Algorithm**: HS256 (HMAC-SHA256)
- **Storage**: Stored in MongoDB `refresh_tokens` collection
- **One-time use**: Invalidated after use (token rotation)
- **Claims**:
  - `sub` (subject): User ID (MongoDB ObjectId as hex string)
  - `type`: "refresh"
  - `iat` (issued at): Token creation timestamp
  - `exp` (expiration): Token expiration timestamp

**Example Structure**:
```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "507f1f77bcf86cd799439011",  // User ID
  "type": "refresh",
  "iat": 1699876543,
  "exp": 1702468543  // 30 days later
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  JWT_SECRET_BASE64
)
```

---

## Token Lifecycle

### 1. Registration & Login

When a user registers or logs in, they receive both tokens:

```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Server Actions**:
1. Validates credentials
2. Deletes all existing refresh tokens for the user (logout other sessions)
3. Generates new access token (15 min expiry)
4. Generates new refresh token (30 day expiry)
5. Stores refresh token in MongoDB with expiry timestamp
6. Returns both tokens to client

### 2. Making Authenticated Requests

Use the access token in the `Authorization` header:

```http
GET /notes
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Server Actions**:
1. Extracts token from `Authorization: Bearer <token>` header
2. Validates token signature using JWT secret
3. Checks token type is "access"
4. Checks token hasn't expired
5. Extracts user ID from `sub` claim
6. Sets user ID in security context
7. Processes request

**If token is valid**: Request succeeds (200 OK)
**If token is invalid/expired**: Returns 401 Unauthorized

### 3. Token Expiration & Refresh

When the access token expires (after 15 minutes):

```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // New access token
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."   // New refresh token
}
```

**Server Actions**:
1. Validates refresh token signature
2. Checks token type is "refresh"
3. Extracts user ID from token
4. Looks up refresh token in MongoDB
5. **Deletes the used refresh token** (one-time use)
6. Generates new access token (15 min expiry)
7. Generates new refresh token (30 day expiry)
8. Stores new refresh token in MongoDB
9. Returns new token pair

**Important**: The old refresh token is invalidated. Always use the new tokens from the response.

### 4. Logout

To end a session and invalidate the refresh token:

```http
POST /auth/logout
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response**:
```json
{
  "success": true
}
```

**Server Actions**:
1. Validates refresh token
2. Extracts user ID from token
3. Deletes the refresh token from MongoDB
4. Returns success

**Client Actions**:
1. Delete both access and refresh tokens from storage
2. Redirect to login page

---

## How to Use Tokens

### Client-Side Storage

#### Web Applications

**Recommended Approaches**:

1. **HttpOnly Cookies (Most Secure)**:
   ```javascript
   // Server sets cookies (requires backend modification)
   Set-Cookie: accessToken=...; HttpOnly; Secure; SameSite=Strict
   Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict
   ```

2. **localStorage (Current Implementation)**:
   ```javascript
   // Save tokens after login
   localStorage.setItem('accessToken', response.accessToken);
   localStorage.setItem('refreshToken', response.refreshToken);

   // Retrieve tokens
   const accessToken = localStorage.getItem('accessToken');

   // Clear tokens on logout
   localStorage.removeItem('accessToken');
   localStorage.removeItem('refreshToken');
   ```

3. **sessionStorage (Session Only)**:
   ```javascript
   // Similar to localStorage but cleared when tab closes
   sessionStorage.setItem('accessToken', response.accessToken);
   ```

**Security Notes**:
- localStorage/sessionStorage are vulnerable to XSS attacks
- Never log tokens to console in production
- Use HTTPS in production to prevent token interception

#### Mobile Applications

**Recommended Approaches**:

1. **iOS (Keychain)**:
   ```swift
   // Save to Keychain (encrypted by OS)
   KeychainWrapper.standard.set(accessToken, forKey: "accessToken")
   ```

2. **Android (Keystore)**:
   ```kotlin
   // Save to SharedPreferences with encryption
   val encryptedPrefs = EncryptedSharedPreferences.create(...)
   encryptedPrefs.edit().putString("accessToken", token).apply()
   ```

3. **React Native (Keychain)**:
   ```javascript
   import * as Keychain from 'react-native-keychain';

   await Keychain.setGenericPassword('accessToken', token);
   ```

### Making Authenticated Requests

#### JavaScript/TypeScript (Fetch API)

```javascript
async function makeAuthenticatedRequest(url, options = {}) {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    }
  });

  if (response.status === 401) {
    // Token expired, try to refresh
    const newToken = await refreshAccessToken();
    if (newToken) {
      // Retry the request with new token
      return makeAuthenticatedRequest(url, options);
    } else {
      // Refresh failed, redirect to login
      window.location.href = '/login';
    }
  }

  return response;
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');

  const response = await fetch('http://localhost:8090/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  if (response.ok) {
    const { accessToken, refreshToken: newRefreshToken } = await response.json();
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', newRefreshToken);
    return accessToken;
  }

  return null;
}
```

#### JavaScript/TypeScript (Axios)

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8090'
});

// Request interceptor - add token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// Response interceptor - handle 401 and refresh
let isRefreshing = false;
let failedQueue = [];

api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await api.post('/auth/refresh', { refreshToken });

        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);

        api.defaults.headers.common.Authorization = `Bearer ${data.accessToken}`;
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;

        failedQueue.forEach(prom => prom.resolve(data.accessToken));
        failedQueue = [];

        return api(originalRequest);
      } catch (err) {
        failedQueue.forEach(prom => prom.reject(err));
        failedQueue = [];
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

#### Python

```python
import requests

class AgentZeroClient:
    def __init__(self, base_url="http://localhost:8090"):
        self.base_url = base_url
        self.access_token = None
        self.refresh_token = None

    def login(self, email, password):
        response = requests.post(
            f"{self.base_url}/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()

        data = response.json()
        self.access_token = data["accessToken"]
        self.refresh_token = data["refreshToken"]
        return data

    def refresh_tokens(self):
        response = requests.post(
            f"{self.base_url}/auth/refresh",
            json={"refreshToken": self.refresh_token}
        )
        response.raise_for_status()

        data = response.json()
        self.access_token = data["accessToken"]
        self.refresh_token = data["refreshToken"]
        return data

    def _make_request(self, method, endpoint, **kwargs):
        headers = kwargs.get('headers', {})
        headers['Authorization'] = f'Bearer {self.access_token}'
        kwargs['headers'] = headers

        response = requests.request(
            method, f"{self.base_url}{endpoint}", **kwargs
        )

        # Auto-refresh on 401
        if response.status_code == 401:
            self.refresh_tokens()
            headers['Authorization'] = f'Bearer {self.access_token}'
            response = requests.request(
                method, f"{self.base_url}{endpoint}", **kwargs
            )

        return response

    def get_notes(self):
        return self._make_request('GET', '/notes').json()

    def create_note(self, title, content, color):
        return self._make_request('POST', '/notes', json={
            'id': None,
            'title': title,
            'content': content,
            'color': color
        }).json()
```

#### cURL

```bash
# Save tokens from login
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}')

# Extract tokens (requires jq)
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refreshToken')

# Use access token for requests
curl -X GET http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Refresh when expired
REFRESH_RESPONSE=$(curl -s -X POST http://localhost:8090/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

# Update tokens
ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | jq -r '.accessToken')
REFRESH_TOKEN=$(echo $REFRESH_RESPONSE | jq -r '.refreshToken')
```

---

## Token Security

### Best Practices

#### 1. Token Storage

**✅ DO**:
- Use HttpOnly cookies when possible (prevents XSS)
- Use secure storage on mobile (Keychain/Keystore)
- Clear tokens on logout
- Use HTTPS in production

**❌ DON'T**:
- Store tokens in URLs or query parameters
- Log tokens to console
- Share tokens between users
- Store tokens in plain text files

#### 2. Token Transmission

**✅ DO**:
- Always use HTTPS in production
- Use `Authorization: Bearer <token>` header
- Validate tokens on every request

**❌ DON'T**:
- Send tokens in URL query strings
- Send tokens over HTTP (unencrypted)
- Include tokens in error messages or logs

#### 3. Token Validation

**Server-Side Validation** (automatic in AgentZero):
- Signature verification using JWT secret
- Expiration check
- Token type verification (access vs refresh)
- User existence check

**Client-Side Checks**:
- Check for token presence before requests
- Handle 401 errors gracefully
- Implement automatic refresh on expiration

#### 4. Token Refresh Strategy

**Proactive Refresh** (before expiration):
```javascript
// Refresh 1 minute before expiry
function scheduleTokenRefresh() {
  const expiresIn = 15 * 60 * 1000; // 15 minutes
  const refreshTime = expiresIn - (60 * 1000); // 14 minutes

  setTimeout(async () => {
    await refreshAccessToken();
    scheduleTokenRefresh(); // Schedule next refresh
  }, refreshTime);
}
```

**Reactive Refresh** (on 401 error):
```javascript
// Refresh only when access denied
if (response.status === 401) {
  await refreshAccessToken();
  // Retry original request
}
```

---

## Common Scenarios

### Scenario 1: User Login

```javascript
async function login(email, password) {
  const response = await fetch('http://localhost:8090/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  const { accessToken, refreshToken } = await response.json();

  // Save tokens
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);

  // Redirect to app
  window.location.href = '/dashboard';
}
```

### Scenario 2: Access Token Expired

```javascript
async function getNotes() {
  let accessToken = localStorage.getItem('accessToken');

  let response = await fetch('http://localhost:8090/notes', {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });

  if (response.status === 401) {
    // Token expired, refresh it
    const refreshToken = localStorage.getItem('refreshToken');

    const refreshResponse = await fetch('http://localhost:8090/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (refreshResponse.ok) {
      const tokens = await refreshResponse.json();
      localStorage.setItem('accessToken', tokens.accessToken);
      localStorage.setItem('refreshToken', tokens.refreshToken);

      // Retry original request with new token
      response = await fetch('http://localhost:8090/notes', {
        headers: { 'Authorization': `Bearer ${tokens.accessToken}` }
      });
    } else {
      // Refresh failed, redirect to login
      window.location.href = '/login';
      return;
    }
  }

  return await response.json();
}
```

### Scenario 3: Refresh Token Expired

```javascript
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');

  const response = await fetch('http://localhost:8090/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  if (response.ok) {
    const tokens = await response.json();
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    return true;
  } else {
    // Refresh token expired or invalid
    // Clear storage and redirect to login
    localStorage.clear();
    window.location.href = '/login';
    return false;
  }
}
```

### Scenario 4: User Logout

```javascript
async function logout() {
  const refreshToken = localStorage.getItem('refreshToken');

  // Invalidate refresh token on server
  await fetch('http://localhost:8090/auth/logout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  // Clear local storage
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');

  // Redirect to login
  window.location.href = '/login';
}
```

### Scenario 5: Multiple Tabs/Windows

**Problem**: User logs out in one tab, but other tabs still have tokens.

**Solution**: Use `storage` event listener:

```javascript
// Listen for storage changes
window.addEventListener('storage', (event) => {
  if (event.key === 'accessToken' && event.newValue === null) {
    // Token removed in another tab, redirect to login
    window.location.href = '/login';
  }
});
```

---

## Token Debugging

### Decode Token (Client-Side)

```javascript
function parseJwt(token) {
  const base64Url = token.split('.')[1];
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const jsonPayload = decodeURIComponent(
    atob(base64).split('').map(c =>
      '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    ).join('')
  );
  return JSON.parse(jsonPayload);
}

// Usage
const accessToken = localStorage.getItem('accessToken');
const payload = parseJwt(accessToken);

console.log('User ID:', payload.sub);
console.log('Token Type:', payload.type);
console.log('Issued At:', new Date(payload.iat * 1000));
console.log('Expires At:', new Date(payload.exp * 1000));
console.log('Time Until Expiry:',
  Math.floor((payload.exp * 1000 - Date.now()) / 1000), 'seconds');
```

### Check Token Expiry

```javascript
function isTokenExpired(token) {
  try {
    const payload = parseJwt(token);
    return Date.now() >= payload.exp * 1000;
  } catch (e) {
    return true;
  }
}

const accessToken = localStorage.getItem('accessToken');
if (isTokenExpired(accessToken)) {
  console.log('Token is expired, need to refresh');
}
```

### Online Token Decoder

For debugging (⚠️ **never use with production tokens**):
- [jwt.io](https://jwt.io/) - Decode and verify JWT tokens

---

## Troubleshooting

### Problem: "401 Unauthorized" on All Requests

**Possible Causes**:
1. Access token expired
2. Token not included in request
3. Wrong token format
4. Invalid JWT secret

**Solution**:
```javascript
// Check if token exists
const token = localStorage.getItem('accessToken');
console.log('Token exists:', !!token);

// Check token format
console.log('Token starts with Bearer:',
  request.headers.Authorization?.startsWith('Bearer '));

// Check expiry
const payload = parseJwt(token);
console.log('Expired:', Date.now() >= payload.exp * 1000);

// Try refresh
await refreshAccessToken();
```

### Problem: Refresh Token Fails

**Possible Causes**:
1. Refresh token expired (30 days)
2. Refresh token already used (one-time use)
3. Refresh token deleted (logout)

**Solution**:
```javascript
// Force re-login
localStorage.clear();
window.location.href = '/login';
```

### Problem: Token Not Persisting

**Possible Causes**:
1. Browser in private/incognito mode
2. localStorage disabled
3. Token not being saved after login

**Solution**:
```javascript
// Check if localStorage is available
try {
  localStorage.setItem('test', 'test');
  localStorage.removeItem('test');
  console.log('localStorage available');
} catch (e) {
  console.error('localStorage not available:', e);
  // Use sessionStorage or cookies instead
}
```

### Problem: "Invalid JWT Secret" Error

**Cause**: JWT_SECRET_BASE64 environment variable not set or changed.

**Solution**:
```bash
# Ensure JWT secret is set
echo $JWT_SECRET_BASE64

# If changed, all existing tokens are invalidated
# Users need to login again
```

---

## Summary

### Token Flow Diagram

```
┌─────────────────┐
│ User Registers  │
│   or Logs In    │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  Server Issues Tokens:      │
│  • Access Token (15 min)    │
│  • Refresh Token (30 days)  │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ Client Saves Tokens         │
│ (localStorage, Keychain,    │
│  or HttpOnly Cookies)       │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ Client Makes API Request    │
│ Authorization: Bearer <AT>  │
└────────┬────────────────────┘
         │
    ┌────┴────┐
    │ Valid?  │
    └────┬────┘
         │
    ┌────┴─────────┐
    │              │
   YES            NO (401)
    │              │
    ▼              ▼
┌────────┐    ┌──────────────────┐
│Success │    │ Refresh Token     │
│200 OK  │    │ POST /auth/refresh│
└────────┘    └───────┬───────────┘
                      │
                 ┌────┴────┐
                 │ Valid?  │
                 └────┬────┘
                      │
                 ┌────┴─────────┐
                 │              │
                YES            NO
                 │              │
                 ▼              ▼
         ┌───────────────┐ ┌─────────┐
         │ New Token Pair│ │ Logout  │
         │ Return to User│ │ Redirect│
         └───────────────┘ └─────────┘
```

### Key Points

✅ **Access Token**: Short-lived (15 min), used for API requests
✅ **Refresh Token**: Long-lived (30 days), used to get new access tokens
✅ **Token Rotation**: Refresh tokens are one-time use (invalidated after refresh)
✅ **Security**: Tokens stored in MongoDB, automatic cleanup via TTL indexes
✅ **Logout**: Deletes refresh token from database
✅ **Login**: Deletes all previous refresh tokens (logout other sessions)

---

## Additional Resources

- **API Usage**: See `USAGE.md` for API examples
- **Postman Testing**: See `POSTMAN-GUIDE.md` for GUI testing
- **Local Setup**: See `STEPS.md` for getting started
- **Architecture**: See `DOCUMENTATION.md` for implementation details
- **JWT Standard**: [RFC 7519](https://tools.ietf.org/html/rfc7519)

---

**Security Note**: Always use HTTPS in production to prevent token interception. Store secrets securely and never commit them to version control.

