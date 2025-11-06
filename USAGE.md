# AgentZero - Usage Guide

## Overview

AgentZero provides a secure REST API for user authentication and personal note management. This guide demonstrates how to use the API effectively with practical examples and best practices.

## Quick Start

### Base URL

```
Development: http://localhost:8090
Production:  https://api.agentzero.com
```

### Authentication Flow

1. **Register** a new account
2. **Login** to receive access and refresh tokens
3. Use **access token** for API requests
4. **Refresh** token when access token expires
5. **Logout** to invalidate refresh token

**📚 For detailed token management, security best practices, and implementation examples, see [TOKENS.md](TOKENS.md)**

## API Endpoints

### Authentication Endpoints

All authentication endpoints are publicly accessible (no token required).

#### 1. Register New User

Create a new user account.

**Endpoint**: `POST /auth/register`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Success Response**: `200 OK` (No body)

**Error Responses**:
- `400 Bad Request` - Invalid input format
- `409 Conflict` - Email already exists (if validation added)

**Example** (cURL):
```bash
curl -X POST http://localhost:8090/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'
```

**Example** (JavaScript):
```javascript
const response = await fetch('http://localhost:8090/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!'
  })
});
```

**Example** (Python):
```python
import requests

response = requests.post('http://localhost:8090/auth/register', json={
    'email': 'user@example.com',
    'password': 'SecurePassword123!'
})
```

---

#### 2. Login

Authenticate and receive JWT tokens.

**Endpoint**: `POST /auth/login`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Success Response**: `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Token Details**:
- `accessToken`: Short-lived (15 minutes), use for API requests
- `refreshToken`: Long-lived (30 days), use to get new access tokens

**Error Responses**:
- `400 Bad Request` - Invalid input format
- `401 Unauthorized` - Invalid credentials

**Example** (cURL):
```bash
curl -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'
```

**Example** (JavaScript):
```javascript
const response = await fetch('http://localhost:8090/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!'
  })
});

const { accessToken, refreshToken } = await response.json();

// Store tokens securely
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);
```

**Example** (Python):
```python
import requests

response = requests.post('http://localhost:8090/auth/login', json={
    'email': 'user@example.com',
    'password': 'SecurePassword123!'
})

tokens = response.json()
access_token = tokens['accessToken']
refresh_token = tokens['refreshToken']
```

---

#### 3. Refresh Tokens

Exchange refresh token for new token pair.

**Endpoint**: `POST /auth/refresh`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response**: `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Important Notes**:
- Old refresh token is **invalidated** after use (one-time use)
- Store new tokens and use them for subsequent requests
- Refresh tokens are rotated for security

**Error Responses**:
- `400 Bad Request` - Invalid or expired refresh token
- `401 Unauthorized` - Token not found or already used

**Example** (cURL):
```bash
curl -X POST http://localhost:8090/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'
```

**Example** (JavaScript):
```javascript
async function refreshTokens() {
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
  } else {
    // Refresh failed, redirect to login
    window.location.href = '/login';
  }
}
```

---

#### 4. Logout

Invalidate refresh token and end session.

**Endpoint**: `POST /auth/logout`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Success Response**: `200 OK`
```json
{
  "success": true
}
```

**Error Responses**:
- `400 Bad Request` - Invalid token format

**Example** (cURL):
```bash
curl -X POST http://localhost:8090/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token-here"
  }'
```

**Example** (JavaScript):
```javascript
async function logout() {
  const refreshToken = localStorage.getItem('refreshToken');

  await fetch('http://localhost:8090/auth/logout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  // Clear local tokens
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');

  // Redirect to login
  window.location.href = '/login';
}
```

---

### Notes Endpoints

All notes endpoints require authentication via access token.

#### Authentication Header Format

```
Authorization: Bearer <access-token>
```

#### 1. Create or Update Note

Create a new note or update existing one (upsert operation).

**Endpoint**: `POST /notes`

**Headers**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Request Body (Create)**:
```json
{
  "id": null,
  "title": "My First Note",
  "content": "This is the content of my note",
  "color": "#FFD700"
}
```

**Request Body (Update)**:
```json
{
  "id": "507f1f77bcf86cd799439011",
  "title": "Updated Note Title",
  "content": "Updated content",
  "color": "#FF6347"
}
```

**Success Response**: `200 OK`
```json
{
  "id": "507f1f77bcf86cd799439011",
  "title": "My First Note",
  "content": "This is the content of my note",
  "color": "#FFD700",
  "createdAt": "2025-01-06T10:30:00Z"
}
```

**Error Responses**:
- `401 Unauthorized` - Missing or invalid access token
- `400 Bad Request` - Invalid input format

**Example** (cURL):
```bash
# Create new note
curl -X POST http://localhost:8090/notes \
  -H "Authorization: Bearer your-access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "id": null,
    "title": "My First Note",
    "content": "This is the content of my note",
    "color": "#FFD700"
  }'

# Update existing note
curl -X POST http://localhost:8090/notes \
  -H "Authorization: Bearer your-access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "507f1f77bcf86cd799439011",
    "title": "Updated Title",
    "content": "Updated content",
    "color": "#FF6347"
  }'
```

**Example** (JavaScript):
```javascript
async function saveNote(note) {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch('http://localhost:8090/notes', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      id: note.id || null,
      title: note.title,
      content: note.content,
      color: note.color
    })
  });

  if (response.status === 401) {
    // Token expired, refresh and retry
    await refreshTokens();
    return saveNote(note);
  }

  return await response.json();
}
```

**Example** (Python):
```python
import requests

def save_note(access_token, note):
    headers = {
        'Authorization': f'Bearer {access_token}',
        'Content-Type': 'application/json'
    }

    response = requests.post('http://localhost:8090/notes',
        headers=headers,
        json={
            'id': note.get('id'),
            'title': note['title'],
            'content': note['content'],
            'color': note['color']
        }
    )

    return response.json()
```

---

#### 2. Get All Notes

Retrieve all notes for the authenticated user.

**Endpoint**: `GET /notes`

**Headers**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Success Response**: `200 OK`
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "title": "My First Note",
    "content": "This is the content",
    "color": "#FFD700",
    "createdAt": "2025-01-06T10:30:00Z"
  },
  {
    "id": "507f1f77bcf86cd799439012",
    "title": "Another Note",
    "content": "More content here",
    "color": "#FF6347",
    "createdAt": "2025-01-06T11:00:00Z"
  }
]
```

**Error Responses**:
- `401 Unauthorized` - Missing or invalid access token

**Example** (cURL):
```bash
curl -X GET http://localhost:8090/notes \
  -H "Authorization: Bearer your-access-token"
```

**Example** (JavaScript):
```javascript
async function getNotes() {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch('http://localhost:8090/notes', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  if (response.status === 401) {
    // Token expired, refresh and retry
    await refreshTokens();
    return getNotes();
  }

  return await response.json();
}
```

**Example** (Python):
```python
import requests

def get_notes(access_token):
    headers = {'Authorization': f'Bearer {access_token}'}
    response = requests.get('http://localhost:8090/notes', headers=headers)
    return response.json()
```

---

#### 3. Delete Note

Delete a specific note by ID.

**Endpoint**: `DELETE /notes/{id}`

**Headers**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Path Parameters**:
- `id`: Note ID (ObjectId hex string)

**Success Response**: `200 OK` (No body)

**Error Responses**:
- `401 Unauthorized` - Missing or invalid access token
- `400 Bad Request` - Note not found or not owned by user
- `404 Not Found` - Note ID doesn't exist

**Example** (cURL):
```bash
curl -X DELETE http://localhost:8090/notes/507f1f77bcf86cd799439011 \
  -H "Authorization: Bearer your-access-token"
```

**Example** (JavaScript):
```javascript
async function deleteNote(noteId) {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch(`http://localhost:8090/notes/${noteId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  if (response.status === 401) {
    // Token expired, refresh and retry
    await refreshTokens();
    return deleteNote(noteId);
  }

  return response.ok;
}
```

**Example** (Python):
```python
import requests

def delete_note(access_token, note_id):
    headers = {'Authorization': f'Bearer {access_token}'}
    response = requests.delete(f'http://localhost:8090/notes/{note_id}',
        headers=headers)
    return response.ok
```

---

## Complete Usage Examples

### Example 1: Complete Authentication Flow

```javascript
class AuthService {
  constructor(baseUrl = 'http://localhost:8090') {
    this.baseUrl = baseUrl;
  }

  async register(email, password) {
    const response = await fetch(`${this.baseUrl}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) throw new Error('Registration failed');
  }

  async login(email, password) {
    const response = await fetch(`${this.baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) throw new Error('Login failed');

    const { accessToken, refreshToken } = await response.json();
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);

    return { accessToken, refreshToken };
  }

  async refreshTokens() {
    const refreshToken = localStorage.getItem('refreshToken');

    const response = await fetch(`${this.baseUrl}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (!response.ok) {
      this.logout();
      throw new Error('Refresh failed');
    }

    const { accessToken, refreshToken: newRefreshToken } = await response.json();
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', newRefreshToken);

    return accessToken;
  }

  async logout() {
    const refreshToken = localStorage.getItem('refreshToken');

    try {
      await fetch(`${this.baseUrl}/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      });
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
    }
  }

  getAccessToken() {
    return localStorage.getItem('accessToken');
  }

  isAuthenticated() {
    return !!this.getAccessToken();
  }
}
```

### Example 2: Notes Management with Auto-Retry

```javascript
class NotesService {
  constructor(baseUrl = 'http://localhost:8090', authService) {
    this.baseUrl = baseUrl;
    this.authService = authService;
  }

  async makeAuthenticatedRequest(url, options = {}) {
    const accessToken = this.authService.getAccessToken();

    if (!options.headers) options.headers = {};
    options.headers['Authorization'] = `Bearer ${accessToken}`;

    let response = await fetch(url, options);

    // Auto-refresh on 401
    if (response.status === 401) {
      await this.authService.refreshTokens();
      const newAccessToken = this.authService.getAccessToken();
      options.headers['Authorization'] = `Bearer ${newAccessToken}`;
      response = await fetch(url, options);
    }

    return response;
  }

  async getAllNotes() {
    const response = await this.makeAuthenticatedRequest(
      `${this.baseUrl}/notes`
    );

    if (!response.ok) throw new Error('Failed to fetch notes');
    return await response.json();
  }

  async saveNote(note) {
    const response = await this.makeAuthenticatedRequest(
      `${this.baseUrl}/notes`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: note.id || null,
          title: note.title,
          content: note.content,
          color: note.color
        })
      }
    );

    if (!response.ok) throw new Error('Failed to save note');
    return await response.json();
  }

  async deleteNote(noteId) {
    const response = await this.makeAuthenticatedRequest(
      `${this.baseUrl}/notes/${noteId}`,
      { method: 'DELETE' }
    );

    if (!response.ok) throw new Error('Failed to delete note');
  }
}
```

### Example 3: Python Complete Client

```python
import requests
from typing import Optional, Dict, List

class AgentZeroClient:
    def __init__(self, base_url: str = "http://localhost:8090"):
        self.base_url = base_url
        self.access_token: Optional[str] = None
        self.refresh_token: Optional[str] = None

    def register(self, email: str, password: str) -> None:
        """Register a new user"""
        response = requests.post(
            f"{self.base_url}/auth/register",
            json={"email": email, "password": password}
        )
        response.raise_for_status()

    def login(self, email: str, password: str) -> Dict[str, str]:
        """Login and store tokens"""
        response = requests.post(
            f"{self.base_url}/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()

        tokens = response.json()
        self.access_token = tokens["accessToken"]
        self.refresh_token = tokens["refreshToken"]
        return tokens

    def refresh_tokens(self) -> str:
        """Refresh access token"""
        if not self.refresh_token:
            raise Exception("No refresh token available")

        response = requests.post(
            f"{self.base_url}/auth/refresh",
            json={"refreshToken": self.refresh_token}
        )
        response.raise_for_status()

        tokens = response.json()
        self.access_token = tokens["accessToken"]
        self.refresh_token = tokens["refreshToken"]
        return self.access_token

    def logout(self) -> None:
        """Logout and clear tokens"""
        if self.refresh_token:
            try:
                requests.post(
                    f"{self.base_url}/auth/logout",
                    json={"refreshToken": self.refresh_token}
                )
            finally:
                self.access_token = None
                self.refresh_token = None

    def _make_authenticated_request(self, method: str, endpoint: str,
                                   **kwargs) -> requests.Response:
        """Make authenticated request with auto-retry on 401"""
        if not self.access_token:
            raise Exception("Not authenticated")

        headers = kwargs.get('headers', {})
        headers['Authorization'] = f'Bearer {self.access_token}'
        kwargs['headers'] = headers

        response = requests.request(method, f"{self.base_url}{endpoint}", **kwargs)

        # Auto-refresh on 401
        if response.status_code == 401:
            self.refresh_tokens()
            headers['Authorization'] = f'Bearer {self.access_token}'
            response = requests.request(method, f"{self.base_url}{endpoint}", **kwargs)

        return response

    def get_notes(self) -> List[Dict]:
        """Get all notes"""
        response = self._make_authenticated_request('GET', '/notes')
        response.raise_for_status()
        return response.json()

    def create_note(self, title: str, content: str, color: str) -> Dict:
        """Create a new note"""
        response = self._make_authenticated_request(
            'POST', '/notes',
            json={
                'id': None,
                'title': title,
                'content': content,
                'color': color
            }
        )
        response.raise_for_status()
        return response.json()

    def update_note(self, note_id: str, title: str, content: str, color: str) -> Dict:
        """Update existing note"""
        response = self._make_authenticated_request(
            'POST', '/notes',
            json={
                'id': note_id,
                'title': title,
                'content': content,
                'color': color
            }
        )
        response.raise_for_status()
        return response.json()

    def delete_note(self, note_id: str) -> None:
        """Delete a note"""
        response = self._make_authenticated_request('DELETE', f'/notes/{note_id}')
        response.raise_for_status()


# Usage example
if __name__ == "__main__":
    client = AgentZeroClient()

    # Register and login
    client.register("user@example.com", "SecurePassword123!")
    client.login("user@example.com", "SecurePassword123!")

    # Create note
    note = client.create_note(
        title="My First Note",
        content="This is a test note",
        color="#FFD700"
    )
    print(f"Created note: {note['id']}")

    # Get all notes
    notes = client.get_notes()
    print(f"Total notes: {len(notes)}")

    # Update note
    updated = client.update_note(
        note_id=note['id'],
        title="Updated Title",
        content="Updated content",
        color="#FF6347"
    )

    # Delete note
    client.delete_note(note['id'])

    # Logout
    client.logout()
```

## Best Practices

### Token Management

1. **Store Tokens Securely**
   - Web: Use httpOnly cookies or secure localStorage
   - Mobile: Use secure storage (Keychain, KeyStore)
   - Never log tokens or expose in URLs

2. **Handle Token Expiration**
   - Implement automatic refresh on 401 responses
   - Refresh proactively before expiration
   - Handle refresh failures gracefully (redirect to login)

3. **Token Rotation**
   - Always use new tokens from refresh endpoint
   - Old refresh tokens are invalidated
   - Don't reuse tokens

### Error Handling

1. **Network Errors**
```javascript
try {
  const notes = await notesService.getAllNotes();
} catch (error) {
  if (error.message === 'Failed to fetch') {
    // Network error
    showError('Network error. Please check your connection.');
  } else {
    // API error
    showError('Failed to load notes. Please try again.');
  }
}
```

2. **Authentication Errors**
```javascript
// Redirect to login on authentication failure
if (response.status === 401 && !isRefreshing) {
  localStorage.clear();
  window.location.href = '/login';
}
```

3. **Validation Errors**
```javascript
// Handle 400 errors with user feedback
if (response.status === 400) {
  const error = await response.json();
  showError(error.message || 'Invalid input');
}
```

### API Request Optimization

1. **Debounce Save Operations**
```javascript
const debouncedSave = debounce(async (note) => {
  await notesService.saveNote(note);
}, 1000);
```

2. **Cache Responses**
```javascript
const notesCache = new Map();

async function getCachedNotes() {
  if (notesCache.has('all')) {
    return notesCache.get('all');
  }

  const notes = await notesService.getAllNotes();
  notesCache.set('all', notes);
  setTimeout(() => notesCache.delete('all'), 60000); // 1 min cache

  return notes;
}
```

3. **Batch Operations** (when available)
```javascript
// Instead of deleting one by one
for (const id of noteIds) {
  await deleteNote(id); // N requests
}

// Consider implementing batch delete in future
await deleteNotes(noteIds); // 1 request
```

### Security Best Practices

1. **Never Log Sensitive Data**
```javascript
// Bad
console.log('Token:', accessToken);

// Good
console.log('User authenticated');
```

2. **Validate Input**
```javascript
function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function validatePassword(password) {
  return password.length >= 8;
}
```

3. **Use HTTPS in Production**
```javascript
const baseUrl = process.env.NODE_ENV === 'production'
  ? 'https://api.agentzero.com'
  : 'http://localhost:8090';
```

### Performance Tips

1. **Minimize Token Size**: JWT tokens contain claims. Don't add unnecessary data.

2. **Efficient Polling**: If polling for updates, use reasonable intervals
```javascript
// Poll every 30 seconds instead of constantly
setInterval(async () => {
  const notes = await getNotes();
  updateUI(notes);
}, 30000);
```

3. **Lazy Loading**: Load notes only when needed
```javascript
// Don't fetch on app start if not immediately visible
if (isNotesPageVisible) {
  loadNotes();
}
```

## Troubleshooting

### Common Issues

**401 Unauthorized**
- Token expired → Use refresh endpoint
- Token invalid → Re-login
- Missing Authorization header → Add header
- Wrong token format → Check "Bearer " prefix

**400 Bad Request**
- Missing required fields in request body
- Invalid JSON format
- Invalid ObjectId format for note ID

**Network Errors**
- Check API server is running
- Verify correct base URL
- Check CORS configuration for web clients
- Verify network connectivity

**Note Not Found**
- Verify note ID is correct
- User may not own the note
- Note may have been deleted

## Testing

### Using cURL

```bash
# Register
curl -X POST http://localhost:8090/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Login
TOKEN_RESPONSE=$(curl -X POST http://localhost:8090/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}')

# Extract access token (using jq)
ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.accessToken')

# Create note
curl -X POST http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":null,"title":"Test","content":"Content","color":"#FFD700"}'

# Get notes
curl -X GET http://localhost:8090/notes \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### Using Postman

1. Create a collection for AgentZero
2. Set environment variable `baseUrl` = `http://localhost:8090`
3. Create requests for each endpoint
4. Use collection variables for tokens
5. Set up pre-request scripts for auto-refresh

**Pre-request Script for Auto-Token**:
```javascript
const accessToken = pm.environment.get('accessToken');
if (accessToken) {
  pm.request.headers.add({
    key: 'Authorization',
    value: `Bearer ${accessToken}`
  });
}
```

## Advanced Usage

### Implementing Token Refresh Interceptor (Axios)

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8090'
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
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

        api.defaults.headers.common['Authorization'] = 'Bearer ' + data.accessToken;
        originalRequest.headers['Authorization'] = 'Bearer ' + data.accessToken;

        processQueue(null, data.accessToken);
        return api(originalRequest);
      } catch (err) {
        processQueue(err, null);
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

## Support

For more information:
- **API Documentation**: See DOCUMENTATION.md
- **Deployment Guide**: See DEPLOY.md
- **Issues**: Report on GitHub repository

---

**Version**: 1.0.0
**Last Updated**: January 2025

