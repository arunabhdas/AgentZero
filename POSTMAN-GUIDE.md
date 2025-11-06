# AgentZero Postman Collection Guide

## Quick Start

### 1. Import the Collection

1. Open Postman
2. Click **Import** button (top left)
3. Select **File** tab
4. Choose `AgentZero-Postman-Collection.json` from this repository
5. Click **Import**

### 2. Configure Variables (Optional)

The collection comes with default variables:
- `baseUrl`: `http://localhost:8090` (change if your server is elsewhere)
- `userEmail`: `testuser@example.com` (change to your email)
- `userPassword`: `SecurePassword123` (change to your password)

To edit variables:
1. Click on the collection name "AgentZero API"
2. Go to **Variables** tab
3. Update the **Current Value** column
4. Click **Save**

### 3. Test the API (Recommended Flow)

#### Step 1: Register a User
1. Open **Authentication** → **Register**
2. Click **Send**
3. Should return `200 OK`

#### Step 2: Login
1. Open **Authentication** → **Login**
2. Click **Send**
3. Should return `200 OK` with `accessToken` and `refreshToken`
4. **Tokens are automatically saved!** ✨

#### Step 3: Create a Note
1. Open **Notes** → **Create Note**
2. Modify the note body if desired
3. Click **Send**
4. Should return `200 OK` with your created note
5. **Note ID is automatically saved as `lastNoteId`!** ✨

#### Step 4: Get All Notes
1. Open **Notes** → **Get All Notes**
2. Click **Send**
3. Should return array of your notes

#### Step 5: Update a Note
1. Open **Notes** → **Update Note**
2. Automatically uses the `lastNoteId` from Step 3
3. Modify the note data if desired
4. Click **Send**
5. Should return the updated note

#### Step 6: Delete a Note
1. Open **Notes** → **Delete Note**
2. Automatically uses the `lastNoteId`
3. Click **Send**
4. Should return `200 OK`

#### Step 7: Logout
1. Open **Authentication** → **Logout**
2. Click **Send**
3. Tokens are automatically cleared

## Features

### ✨ Automatic Token Management

The collection automatically handles JWT tokens:
- **Login**: Saves `accessToken` and `refreshToken` to collection variables
- **Refresh**: Updates both tokens automatically
- **Logout**: Clears tokens from collection variables
- **All Note Requests**: Automatically use the saved `accessToken`

### 🔍 Built-in Tests

Each request includes tests that:
- Verify response status codes
- Validate response structure
- Log important information to console
- Detect token expiration (401 errors)

### 📝 Request Descriptions

Every request includes a description explaining:
- What the endpoint does
- What parameters it expects
- What it returns

## Variables Reference

### Collection Variables

| Variable | Description | Auto-Updated |
|----------|-------------|--------------|
| `baseUrl` | API server URL | No |
| `userEmail` | User email for auth | No |
| `userPassword` | User password for auth | No |
| `accessToken` | JWT access token (15 min) | Yes ✅ |
| `refreshToken` | JWT refresh token (30 days) | Yes ✅ |
| `lastNoteId` | Last created/updated note ID | Yes ✅ |

### How to Use Variables

Variables are referenced with double curly braces: `{{variableName}}`

Example in URL:
```
{{baseUrl}}/notes/{{lastNoteId}}
```

Example in request body:
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Example in authorization header (automatic):
```
Authorization: Bearer {{accessToken}}
```

## Tips & Tricks

### View Console Logs

Open Postman Console (bottom left icon or `Cmd/Ctrl + Alt + C`) to see:
- Token save confirmations
- Request/response details
- Warning messages for expired tokens

### Manual Token Testing

To manually set or view tokens:
1. Click collection name "AgentZero API"
2. Go to **Variables** tab
3. View or edit `accessToken` and `refreshToken` values

### Test Token Expiration

Access tokens expire after 15 minutes. To test refresh:
1. Login and wait 15+ minutes
2. Try any Notes request → Should get 401 error
3. Run **Refresh Token** request
4. Try the Notes request again → Should work

### Use Different Environments

For testing against different servers (dev, staging, prod):
1. Create Postman Environments
2. Set `baseUrl` variable in each environment
3. Switch between environments

Example environments:
- **Local**: `baseUrl` = `http://localhost:8090`
- **Dev**: `baseUrl` = `https://dev-api.agentzero.com`
- **Prod**: `baseUrl` = `https://api.agentzero.com`

### Test Multiple Users

To test with different users:
1. Change `userEmail` and `userPassword` variables
2. Run Register → Login sequence
3. Each user's notes are isolated

## Troubleshooting

### "No access token set" in Console

**Solution**: Run the **Login** request first to get tokens.

### 401 Unauthorized Error

**Solution**:
1. Check if access token expired (15 min lifetime)
2. Run **Refresh Token** request
3. If refresh fails, run **Login** again

### 400 Bad Request

**Possible causes**:
- Missing required fields in request body
- Invalid data format
- Invalid ObjectId format for note IDs

**Solution**: Check the request body matches the expected format.

### Connection Refused / Network Error

**Solution**:
1. Verify the server is running: `./gradlew bootRun`
2. Check `baseUrl` variable points to correct server
3. Verify port 8090 is accessible

### "Note not found" Error

**Solution**:
1. Make sure you created a note first
2. Verify `lastNoteId` variable is set
3. You can only delete your own notes

## Advanced Usage

### Running Collection with Newman (CLI)

Install Newman:
```bash
npm install -g newman
```

Run the collection:
```bash
newman run AgentZero-Postman-Collection.json
```

### Exporting Collection

To share with team members:
1. Right-click collection "AgentZero API"
2. Select **Export**
3. Choose **Collection v2.1** format
4. Save and share the JSON file

### Creating Test Scripts

Add custom tests to any request:
1. Open the request
2. Go to **Tests** tab
3. Add JavaScript test code

Example test:
```javascript
pm.test("Response time is less than 200ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(200);
});
```

## Example Workflow: Complete CRUD Operations

Here's a complete workflow for testing all operations:

```
1. Register        → Create user account
2. Login           → Get tokens (auto-saved)
3. Create Note     → Create first note (ID auto-saved)
4. Get All Notes   → See your notes list
5. Update Note     → Modify the note (uses auto-saved ID)
6. Get All Notes   → Verify update
7. Create Note     → Create second note
8. Get All Notes   → See both notes
9. Delete Note     → Remove a note (uses auto-saved ID)
10. Get All Notes  → Verify deletion
11. Logout         → Clear tokens
```

## Support

For more information:
- **API Documentation**: See `USAGE.md`
- **Setup Guide**: See `STEPS.md`
- **Deployment**: See `DEPLOY.md`

---

**Happy Testing!** 🚀

