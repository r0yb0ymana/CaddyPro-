# Best Practices for Claude-in-the-Loop Development

Advanced patterns and recommendations for spec-driven, multi-agent development.

## Spec Writing

### Make Acceptance Criteria Testable

Every criterion should be verifiable by a test:

**Bad:**
```markdown
- A1: The system should be fast
- A2: Error messages should be helpful
```

**Good:**
```markdown
- A1: API responds within 200ms for 95% of requests
- A2: Error responses include HTTP status, error code, and human-readable message
```

### Use Given-When-Then Format

Structure acceptance criteria clearly:

```markdown
### A1: Create user with valid data
- GIVEN: Valid user email and password
- WHEN: POST /api/users with {"email": "test@example.com", "password": "secure123"}
- THEN: Returns 201 with user object containing id and email (password excluded)
```

### Document Non-Goals Explicitly

Prevent scope creep by listing what's NOT included:

```markdown
## 3. Non-goals
- NG1: Social authentication (OAuth) - future feature
- NG2: Password reset flow - separate spec required
- NG3: Email verification - not MVP requirement
```

### Include Negative Test Cases

Test what should NOT happen:

```markdown
### A5: Reject duplicate email
- GIVEN: User with email "test@example.com" exists
- WHEN: POST /api/users with same email
- THEN: Returns 409 with error "Email already registered"
```

## Planning

### Keep Tasks Small

Each task should be:
- Implementable in 1-3 hours
- Independently verifiable
- Committable on its own

**Bad:**
```markdown
## Task 1: Implement user authentication
- Add login/logout/register endpoints
- Add JWT middleware
- Add password hashing
- Add tests
```

**Good:**
```markdown
## Task 1: Add password hashing utility
- Create hashPassword() and verifyPassword() functions
- Use bcrypt with cost factor 10
- Add unit tests

## Task 2: Add user registration endpoint
- POST /api/register route
- Validate email/password
- Store hashed password
- Add integration tests

## Task 3: Add login endpoint
- POST /api/login route
- Verify credentials
- Generate JWT
- Add integration tests
```

### Order by Dependencies

```markdown
## Task 1: Database schema
Dependencies: None

## Task 2: Data access layer
Dependencies: Task 1

## Task 3: API endpoints
Dependencies: Task 2

## Task 4: Integration tests
Dependencies: Task 3
```

### Include Verification Steps

Each task should specify how to verify completion:

```markdown
## Task 2: Add login endpoint

### Verification:
- [ ] `npm test -- login.test.js` passes
- [ ] Manual test: POST /api/login returns JWT
- [ ] Manual test: Invalid credentials return 401
- [ ] Code review: password not logged or exposed
```

## Implementation

### Use Test-Driven Development

1. Write test for acceptance criterion
2. Confirm test fails (red)
3. Implement minimal code to pass (green)
4. Refactor if needed
5. Commit

**Benefits:**
- Tests validate spec directly
- Prevents over-engineering
- Provides regression protection

### Commit After Each Task

```bash
git add .
git commit -m "[Task 3] Add login endpoint

Spec: specs/user-auth.md
Section: R2 - User Login

Changes:
- Added POST /api/login route
- Validates credentials against database
- Returns JWT on success
- Returns 401 for invalid credentials

Verified by:
- Engineer: implemented per spec
- Tester: all acceptance criteria pass
- Reviewer: approved with 0 issues"
```

### Stop on Ambiguity

If the spec is unclear, **stop implementing**:

1. Document the ambiguity
2. Update the spec with a question
3. Get clarification from stakeholders
4. Update spec with the decision
5. Resume implementation

**Never guess what the spec means.**

## Multi-Agent Workflow

### Let Agents Do Their Job

**Engineer:**
- Implements exactly what spec says
- Doesn't add "nice to have" features
- Stops if spec is ambiguous

**Tester:**
- Validates against acceptance criteria only
- Doesn't test for unstated requirements
- Reports spec mismatches objectively

**Reviewer:**
- Checks spec alignment strictly
- Flags security issues
- Doesn't nitpick style (unless spec specifies standards)

### Use Parallel Instances for Complex Work

For large features, run multiple Claude instances:

```bash
# Terminal 1: Main implementation
cd ~/project
claude
> /implement specs/feature-plan.md

# Terminal 2: Test writing
cd ~/project-test-worktree
claude
> Focus on writing comprehensive tests for specs/feature.md

# Terminal 3: Documentation
cd ~/project-docs-worktree
claude
> Update API documentation for specs/feature.md
```

### Preserve Context with Subagents

For exploration during implementation:

```
You: I need to understand how authentication works in this codebase

Claude: Let me use a subagent to explore the authentication patterns
[spawns explore agent]

Claude: Based on the exploration:
- Auth uses JWT middleware in src/middleware/auth.js
- Tokens stored in httpOnly cookies
- Token expiry set to 7 days

Now implementing your feature using these patterns...
```

## Code Quality

### Follow Existing Patterns

Spec should reference codebase patterns:

```markdown
## 6. Constraints & invariants

### C1: Code patterns
- Follow existing Express.js router structure in src/routes/
- Use Prisma ORM for database access (no raw SQL)
- Error handling via src/middleware/errorHandler.js
- Tests use Jest + supertest pattern from existing tests/
```

### Security by Default

Always include security checks:

```markdown
### A8: Prevent SQL injection
- GIVEN: Malicious input with SQL syntax
- WHEN: Any database operation
- THEN: Input is parameterized/sanitized, query executes safely

### A9: Prevent XSS
- GIVEN: User input contains <script> tags
- WHEN: Data is returned in API response
- THEN: No script execution possible (proper content-type headers)
```

### Write Tests That Match Specs

Test names should reference acceptance criteria:

```javascript
// spec: A1 - Create user with valid data
test('A1: POST /api/users with valid data returns 201', async () => {
  const response = await request(app)
    .post('/api/users')
    .send({ email: 'test@example.com', password: 'secure123' });

  expect(response.status).toBe(201);
  expect(response.body).toHaveProperty('id');
  expect(response.body.email).toBe('test@example.com');
  expect(response.body).not.toHaveProperty('password');
});
```

## Context Management

### Use /clear Strategically

Clear context when:
- Switching to a different feature
- After completing several tasks
- When Claude seems to lose focus
- Before starting a new planning session

**Don't clear when:**
- In the middle of implementing a task
- During an active debugging session
- When context is still relevant

### Reference Files Explicitly

```
# Bad
"Update the user model"

# Good
"Read src/models/User.js and update according to specs/user-auth.md section R2"
```

### Provide Visual Context

For UI work:
- Paste screenshots of designs
- Drag-drop mockups
- Screenshot current state for comparison

```bash
# Take screenshot (macOS)
Cmd+Ctrl+Shift+4

# In Claude
Ctrl+V to paste
"Implement the login form to match this design"
```

## Iteration Patterns

### Explore → Plan → Code → Commit

**Always follow this order:**

1. **Explore:** Understand codebase, read relevant files
2. **Plan:** Create task breakdown with verification steps
3. **Code:** Implement one task at a time
4. **Commit:** Verify and commit before next task

**Don't:**
- Code before understanding existing patterns
- Skip planning for "quick fixes"
- Batch multiple tasks before committing

### Fix Forward, Not Backward

If you discover an issue after committing:

```bash
# Good: Fix in new commit
git commit -m "[Task 3] Add login endpoint"
# [discover issue]
git commit -m "[Task 3.1] Fix password validation in login"

# Bad: Amending or rebasing
git commit --amend  # Don't do this
git rebase -i       # Don't do this
```

**Exception:** Very recent commits not pushed yet.

### Evolve Specs Based on Reality

If implementation reveals spec issues:

1. Stop implementation
2. Document the discovery
3. Update spec with correct requirements
4. Update plan if needed
5. Resume implementation

**Example:**
```markdown
## 7. Open questions

~~Q1: Should validation happen client or server side?~~
**Decision (2024-01-15):** Server-side validation only. Client validation
added as NG1 (not in scope). Discovered during implementation that our
API is used by mobile apps, so client validation wouldn't be reliable.
```

## Anti-Patterns to Avoid

### Don't Invent Requirements

**Bad:**
```
Spec says: "Add user registration"
Engineer implements: Registration + email verification + password reset
```

**Good:**
```
Spec says: "Add user registration"
Engineer asks: "Spec only mentions registration. Should I add email
verification? It's not in the acceptance criteria."
```

### Don't Skip Tests

**Bad:**
```
"Implementation works manually, tests can come later"
```

**Good:**
```
"Tests written first, implementation makes them pass"
```

### Don't Batch Commits

**Bad:**
```
[Implements tasks 1, 2, 3, 4]
git commit -m "Implemented user authentication"
```

**Good:**
```
[Implements task 1]
git commit -m "[Task 1] Add password hashing"
[Implements task 2]
git commit -m "[Task 2] Add registration endpoint"
```

### Don't Over-Engineer

**Bad:**
```
Spec: "Store todos in a file"
Implementation:
- Abstract storage interface
- File storage adapter
- Memory storage adapter
- Factory pattern for storage selection
- Config system for storage backend
```

**Good:**
```
Spec: "Store todos in a file"
Implementation:
- Simple functions: saveTodos(), loadTodos()
- JSON file at configurable path
- Atomic writes for safety
```

## Measuring Success

You're doing it right when:

- ✅ Every feature has a complete spec before coding
- ✅ Tests validate acceptance criteria directly
- ✅ Code implements exactly what spec says (no more, no less)
- ✅ Commits are small and focused
- ✅ No "looks good to me" without verification
- ✅ Specs evolve based on learnings

You need to improve when:

- ❌ Implementing features without specs
- ❌ Tests written after the fact
- ❌ Code does more than spec requires
- ❌ Large, multi-purpose commits
- ❌ Approval based on code review alone
- ❌ Specs never updated after creation

## Resources

- [Claude Code Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)
- [Getting Started Guide](./getting-started.md)
- [Workflow Documentation](./workflow.md)
- [Example Specs](../specs/)
