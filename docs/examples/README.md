# Workflow Examples

Complete examples demonstrating the claude-in-the-loop workflow.

## Available Examples

### 1. Todo API (specs/example-todo-api.md)

A comprehensive REST API example showing:
- Complete spec with all sections
- Multiple functional requirements
- Detailed acceptance criteria using Given-When-Then
- Constraints and invariants
- Data persistence requirements

**What it demonstrates:**
- How to write testable acceptance criteria
- How to document API requirements
- How to specify data models
- How to handle CRUD operations
- How to define validation rules

**Use this as reference when:**
- Building REST APIs
- Defining data models
- Writing integration tests
- Specifying validation requirements

---

### 2. End-to-End Workflow (docs/getting-started.md)

A simple "Hello API" example showing the complete workflow:
- Creating a spec from template
- Refining with `/spec` command
- Generating plan with `/plan` command
- Executing with `/implement` command
- Verification and commit cycle

**What it demonstrates:**
- The complete development cycle
- Minimal viable spec
- Task breakdown process
- Three-agent verification cycle

**Use this when:**
- Learning the workflow for the first time
- Teaching others the process
- Starting a very simple feature

---

## Running the Examples

### Example 1: Todo API

```bash
# 1. Review the spec
cat specs/example-todo-api.md

# 2. Generate a plan
claude
> /plan specs/example-todo-api.md

# 3. Implement (if you want to try it out)
> /implement specs/example-todo-api-plan.md

# Note: This requires setting up a project with your chosen stack
# (Node.js/Express, Python/Flask, etc.)
```

### Example 2: Hello API (Getting Started)

Follow the step-by-step guide in `docs/getting-started.md`:

```bash
# 1. Create the spec
./scripts/new-spec.sh hello-api

# 2. Edit specs/hello-api.md
# (Follow example in getting-started.md)

# 3. Run the workflow
claude
> /spec specs/hello-api.md
> /plan specs/hello-api.md
> /implement specs/hello-api-plan.md
```

---

## Example Patterns

### Pattern 1: API Endpoint

```markdown
# Feature: User Login Endpoint

## 1. Problem statement
Users need to authenticate to access protected resources.

## 4. Functional requirements

### R1: Login endpoint
- POST /api/login
- Request: `{ "email": string, "password": string }`
- Response: `{ "token": string, "user": object }`

## 5. Acceptance criteria

### A1: Successful login
- GIVEN: User exists with email "test@example.com" and password "pass123"
- WHEN: POST /api/login with correct credentials
- THEN: Returns 200 with JWT token and user object (excluding password)

### A2: Invalid credentials
- GIVEN: Incorrect password
- WHEN: POST /api/login with wrong password
- THEN: Returns 401 with error "Invalid credentials"
- AND: Does not reveal whether email exists (security requirement)

### A3: Missing fields
- GIVEN: Request missing email or password
- WHEN: POST /api/login
- THEN: Returns 400 with error listing required fields
```

---

### Pattern 2: Data Validation

```markdown
# Feature: Email Validation

## 5. Acceptance criteria

### A1: Valid email format accepted
- GIVEN: Email in format "user@domain.com"
- WHEN: Any endpoint accepting email
- THEN: Validation passes

### A2: Invalid format rejected
- GIVEN: Email without @ symbol
- WHEN: Any endpoint accepting email
- THEN: Returns 400 with error "Invalid email format"

### A3: Missing domain rejected
- GIVEN: Email like "user@"
- WHEN: Any endpoint accepting email
- THEN: Returns 400 with error "Invalid email format"

## 6. Constraints & invariants

### C1: Email validation rules
- Must contain exactly one @ symbol
- Must have non-empty local part (before @)
- Must have valid domain (after @)
- Max length: 254 characters (RFC 5321)
- Case-insensitive comparison
```

---

### Pattern 3: Database Operation

```markdown
# Feature: User Data Persistence

## 4. Functional requirements

### R1: Store user data
- Users persisted to database
- Password stored as bcrypt hash
- Email unique constraint enforced

## 5. Acceptance criteria

### A1: User created in database
- GIVEN: Valid user data
- WHEN: User registration succeeds
- THEN: User record exists in database with hashed password

### A2: Duplicate email rejected
- GIVEN: User with email "test@example.com" exists
- WHEN: Attempt to create another user with same email
- THEN: Returns 409 with error "Email already registered"

### A3: Password not stored in plain text
- GIVEN: User created with password "mypassword"
- WHEN: Query database for user record
- THEN: Password field contains bcrypt hash (starts with "$2")
- AND: Plain password not recoverable from hash

## 6. Constraints & invariants

### C1: Password hashing
- Use bcrypt with cost factor 10
- Hash generated server-side only
- Never log or expose hash
```

---

### Pattern 4: Error Handling

```markdown
# Feature: Error Handling

## 5. Acceptance criteria

### A1: Validation errors return 400
- GIVEN: Invalid input (missing required field)
- WHEN: Any endpoint with validation
- THEN: Returns 400 with JSON error describing issue

### A2: Not found returns 404
- GIVEN: Resource doesn't exist
- WHEN: GET /api/resource/:id
- THEN: Returns 404 with error "Resource not found"

### A3: Server errors return 500
- GIVEN: Unexpected error (database down, etc.)
- WHEN: Any endpoint
- THEN: Returns 500 with generic error message
- AND: Detailed error logged server-side
- AND: No stack trace or sensitive info in response

### A4: Error response format
- GIVEN: Any error occurs
- WHEN: Error response sent
- THEN: Response is JSON with:
  - `status`: HTTP status code
  - `error`: Human-readable message
  - `code`: Machine-readable error code (optional)

## 6. Constraints & invariants

### C1: Error logging
- All errors logged to stderr
- Include timestamp, request ID, and error details
- Never log sensitive data (passwords, tokens, etc.)

### C2: Security
- Never expose stack traces to clients
- Never reveal whether user exists in login errors
- Generic messages for server errors
```

---

## Creating Your Own Examples

As you build features:

1. **Save successful specs** as examples:
   ```bash
   cp specs/my-feature.md docs/examples/feature-name-example.md
   ```

2. **Document learnings:**
   ```markdown
   # What Worked Well
   - Given-When-Then format made tests obvious
   - Breaking tasks by endpoint kept changes small

   # What Could Be Improved
   - Should have specified error codes upfront
   - Missed edge case for empty arrays
   ```

3. **Add to this README:**
   - Brief description
   - What it demonstrates
   - When to reference it

---

## Contributing Examples

Good examples to add:

- **Frontend component** - React/Vue/etc with visual specs
- **Background job** - Async processing with retry logic
- **Data migration** - Schema changes with rollback plan
- **Third-party integration** - API client with error handling
- **Batch processing** - File upload and processing
- **Real-time feature** - WebSocket or SSE implementation

Each example should include:
- Complete spec following template
- Generated plan (optional)
- Notes on what worked well
- Common pitfalls encountered
