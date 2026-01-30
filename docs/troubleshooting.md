# Troubleshooting Guide

Common issues and solutions for claude-in-the-loop development.

## Spec Issues

### Problem: Claude keeps inventing requirements

**Symptoms:**
- Engineer adds features not in spec
- Implementation includes "nice to have" extras
- Code does more than acceptance criteria require

**Causes:**
- Spec is too vague
- Acceptance criteria not explicit
- Non-goals not stated

**Solutions:**

1. **Refine the spec** with `/spec` command:
   ```bash
   /spec specs/my-feature.md
   ```

2. **Add explicit non-goals:**
   ```markdown
   ## 3. Non-goals
   - NG1: User authentication (separate feature)
   - NG2: Real-time updates (future enhancement)
   - NG3: File uploads (out of scope)
   ```

3. **Make acceptance criteria testable:**
   ```markdown
   # Bad
   - A1: System should handle errors gracefully

   # Good
   - A1: Invalid input returns 400 with JSON error message
   - A2: Server errors return 500 with generic message (no stack trace)
   - A3: All errors logged to stderr with timestamp and request ID
   ```

4. **Remind the engineer agent:**
   ```
   "Implement ONLY what specs/my-feature.md requires.
   Do not add features, refactoring, or improvements not in the spec."
   ```

---

### Problem: Spec and reality don't match

**Symptoms:**
- Tests pass but don't validate actual requirements
- Implementation works but violates spec
- Code is correct but spec is wrong

**Causes:**
- Spec written before understanding problem
- Requirements changed during implementation
- Edge cases discovered late

**Solutions:**

1. **Update the spec** (not the code):
   ```markdown
   ## 7. Open questions

   ~~Q1: Should we validate email format?~~
   **Decision (2024-01-15):** Yes. Added as A6.
   Discovered users entering invalid emails during testing.

   ## 5. Acceptance criteria

   ### A6: Validate email format (ADDED)
   - GIVEN: Invalid email format
   - WHEN: POST /api/users
   - THEN: Returns 400 with "Invalid email format"
   ```

2. **Update the plan:**
   ```markdown
   ## Task 2.1: Add email validation (NEW)
   **Spec reference**: Section 5, A6
   **Reason**: Discovered during testing
   **Dependencies**: Task 2
   ```

3. **Commit the spec change:**
   ```bash
   git add specs/my-feature.md
   git commit -m "Update spec: Add email validation requirement

   Discovered during Task 2 testing that invalid emails were
   accepted. Added A6 to require format validation."
   ```

4. **Resume implementation** with updated spec.

---

### Problem: Acceptance criteria too vague

**Symptoms:**
- Tests don't clearly verify requirements
- Unclear what "done" means
- Different interpretations possible

**Causes:**
- Using subjective language
- Not specifying exact behavior
- Missing edge cases

**Solutions:**

1. **Use Given-When-Then:**
   ```markdown
   # Bad
   - A1: Login should work correctly

   # Good
   - A1: Valid credentials return JWT
     - GIVEN: User exists with email "test@example.com" and password "pass123"
     - WHEN: POST /api/login with correct credentials
     - THEN: Returns 200 with JWT in response body
   ```

2. **Specify exact values:**
   ```markdown
   # Bad
   - A2: API should be fast

   # Good
   - A2: GET /api/users responds within 200ms for 95th percentile
   ```

3. **Include negative cases:**
   ```markdown
   ### A3: Reject invalid credentials
   - GIVEN: Incorrect password
   - WHEN: POST /api/login with wrong password
   - THEN: Returns 401 with error "Invalid credentials"
   - AND: Does not reveal whether email exists
   ```

---

## Implementation Issues

### Problem: Tests keep failing

**Symptoms:**
- Tester agent reports failures
- Implementation seems correct
- Tests seem correct

**Diagnosis Steps:**

1. **Read the test output:**
   ```bash
   # Ask Claude to show the full test output
   "Show me the complete test output from the last run"
   ```

2. **Verify against spec:**
   ```
   "Does the test validate acceptance criterion A2 exactly as written?"
   ```

3. **Check for mismatches:**
   - Test tests something not in spec → Fix test
   - Code doesn't implement spec → Fix code
   - Spec is wrong → Update spec first

**Solutions:**

**If test is wrong:**
```javascript
// Bad test (testing something not in spec)
test('should send welcome email', async () => { // Not in acceptance criteria!
  // ...
});

// Good test (matches A1 exactly)
test('A1: POST /api/users returns 201 with user object', async () => {
  // ...
});
```

**If code is wrong:**
```
"The test correctly validates A2, but the implementation doesn't match.
Read specs/my-feature.md section 5, criterion A2, and fix the code."
```

**If spec is wrong:**
```
1. Stop implementation
2. Update spec with correct requirement
3. Update test to match new spec
4. Update implementation
```

---

### Problem: Changes are too large

**Symptoms:**
- Tasks take many hours
- Commits touch 10+ files
- Hard to review changes
- Difficult to verify completion

**Causes:**
- Tasks not broken down enough
- Multiple tasks done at once
- Over-engineering

**Solutions:**

1. **Re-plan with smaller tasks:**
   ```markdown
   # Bad
   ## Task 1: Implement user management
   - Add create/read/update/delete endpoints
   - Add validation
   - Add tests
   - Add documentation

   # Good
   ## Task 1: Add create user endpoint
   - POST /api/users route only
   - Basic validation (email, password)
   - Integration test for success case

   ## Task 2: Add validation error tests
   - Test missing email
   - Test invalid email format
   - Test weak password

   ## Task 3: Add get user endpoint
   - GET /api/users/:id route only
   - Integration tests
   ```

2. **Use 1-3 hour rule:**
   - Each task should be implementable in 1-3 hours
   - If longer, break it down more

3. **Commit incrementally:**
   ```bash
   # Don't wait until everything is done
   git commit -m "[Task 1] Add create user endpoint"
   # Then continue to Task 2
   ```

---

### Problem: Code doesn't follow project patterns

**Symptoms:**
- Implementation uses different style
- Doesn't match existing code
- Review flags architectural inconsistencies

**Causes:**
- Spec doesn't reference existing patterns
- Engineer didn't explore codebase first
- Patterns not documented in CLAUDE.md

**Solutions:**

1. **Update CLAUDE.md** with project patterns:
   ```markdown
   ## Code Style

   - All API routes use Express.js Router in src/routes/
   - Database access via Prisma ORM (no raw SQL)
   - Error handling via src/middleware/errorHandler.js
   - Tests use Jest with supertest
   - Config from environment variables via src/config/env.js
   ```

2. **Add to spec constraints:**
   ```markdown
   ## 6. Constraints & invariants

   ### C1: Follow existing patterns
   - Use Express Router pattern from src/routes/users.js
   - Database queries via Prisma (see src/db/client.js)
   - Errors thrown as AppError (see src/errors/AppError.js)
   - Tests follow pattern in tests/integration/users.test.js
   ```

3. **Have engineer explore first:**
   ```
   "Before implementing, read these files to understand existing patterns:
   - src/routes/users.js
   - src/middleware/errorHandler.js
   - tests/integration/users.test.js"
   ```

---

## Workflow Issues

### Problem: Claude Code slash commands not working

**Symptoms:**
- `/spec`, `/plan`, or `/implement` not recognized
- "Unknown command" error

**Causes:**
- Commands not in `.claude/commands/` directory
- Incorrect file format
- Claude Code not in repository root

**Solutions:**

1. **Verify directory structure:**
   ```bash
   ls -la .claude/commands/
   # Should show: spec.md, plan.md, implement.md, review.md
   ```

2. **Check you're in repo root:**
   ```bash
   pwd
   # Should be the directory containing .claude/
   ```

3. **Restart Claude Code:**
   ```bash
   exit  # or Ctrl+D
   claude
   ```

4. **Try full path:**
   ```
   # Instead of
   /spec specs/my-feature.md

   # Try
   Read .claude/commands/spec.md and apply it to specs/my-feature.md
   ```

---

### Problem: Agents not behaving as expected

**Symptoms:**
- Engineer agent modifying specs
- Tester agent not running tests
- Reviewer agent making changes

**Causes:**
- Agent frontmatter configuration incorrect
- Not using Task tool to spawn agents
- Agents not understanding their role

**Solutions:**

1. **Check agent frontmatter:**
   ```yaml
   ---
   name: engineer
   description: Implements tasks strictly from the spec.
   tools: Read, Grep, Glob, Bash, Write
   model: sonnet
   permissionMode: acceptEdits
   ---
   ```

2. **Invoke with Task tool:**
   ```
   "Use the Task tool with subagent_type='engineer' to implement Task 1"
   ```

3. **Explicitly state role:**
   ```
   "Acting as the engineer agent from .claude/agents/engineer.md,
   implement Task 1 from the plan. Do not modify the spec."
   ```

---

### Problem: Context window filling up

**Symptoms:**
- Claude responses getting slower
- Seems to "forget" earlier conversation
- Long pauses before responses

**Causes:**
- Long session without clearing
- Many file reads accumulating
- Large command outputs in history

**Solutions:**

1. **Use `/clear` command:**
   ```bash
   /clear
   ```

2. **Clear between features:**
   ```
   [Complete feature A]
   /clear
   [Start feature B]
   ```

3. **Use subagents for exploration:**
   ```
   "Use a subagent to explore the authentication code and summarize
   the patterns, so we preserve context in this conversation."
   ```

4. **Don't clear mid-task:**
   ```
   # Bad
   [Start implementing task 1]
   /clear  # Don't do this!

   # Good
   [Complete task 1]
   [Verify and commit]
   /clear  # OK, task done
   [Start task 2]
   ```

---

## Git Issues

### Problem: Commits don't follow convention

**Symptoms:**
- Unclear commit messages
- Missing spec references
- Can't track which commits belong to which feature

**Causes:**
- No commit template configured
- Not specifying format to engineer agent
- Batch committing multiple tasks

**Solutions:**

1. **Add to CLAUDE.md:**
   ```markdown
   ## Git Workflow

   ### Commit Message Format
   ```
   [Task N] Brief description

   Spec: specs/feature-name.md
   Section: [Section reference]

   Changes:
   - Change 1
   - Change 2

   Verified by:
   - Engineer: implemented per spec
   - Tester: acceptance criteria pass
   - Reviewer: approved
   ```
   ```

2. **Create git commit template:**
   ```bash
   cat > .gitmessage <<EOF
   [Task N] Brief description

   Spec: specs/
   Section:

   Changes:
   -

   Verified by:
   - Engineer:
   - Tester:
   - Reviewer:
   EOF

   git config commit.template .gitmessage
   ```

3. **Specify in /implement command:**
   - Already included in `.claude/commands/implement.md`

---

## Performance Issues

### Problem: Tasks taking too long

**Symptoms:**
- Single task takes multiple hours
- Getting stuck in details
- Over-engineering solutions

**Causes:**
- Tasks not small enough
- Trying to handle all edge cases at once
- Not following spec strictly

**Solutions:**

1. **Break task down further:**
   ```markdown
   # If Task 2 is too large
   ## Task 2.1: Happy path only
   ## Task 2.2: Add error handling
   ## Task 2.3: Add edge cases
   ```

2. **Implement strictly to spec:**
   ```
   "Implement only what A1 and A2 require. Do not add error handling
   for cases not in the acceptance criteria."
   ```

3. **Use time-boxing:**
   ```
   "If this task takes more than 2 hours, stop and break it into
   smaller tasks."
   ```

---

## Need More Help?

1. **Check CLAUDE.md** - Repository-specific guidance
2. **Review examples** - See `specs/example-todo-api.md`
3. **Read best practices** - See `docs/best-practices.md`
4. **Ask Claude:**
   ```
   "I'm having trouble with [specific issue]. What should I check?"
   ```

5. **Review Claude Code docs:**
   - [Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)
   - [Official Documentation](https://claude.com/claude-code)
