# Getting Started with Claude-in-the-Loop

This guide walks you through your first feature using the spec-driven, multi-agent workflow.

## Prerequisites

- [Claude Code](https://claude.com/claude-code) installed
- Basic understanding of your project's tech stack
- Git for version control

## Quick Start

### 1. Fork & Setup

```bash
# Clone or fork this repository
git clone <your-fork-url>
cd claude-in-the-loop

# Run the interactive setup script
# This configures your project name, package IDs, and environment
./setup.sh
```

### 2. Create Your First Spec

```bash
# Use the helper script
./scripts/new-spec.sh my-first-feature

# Or manually
cp specs/feature-template.md specs/my-first-feature.md
```

### 3. Refine the Spec with Claude

```bash
# Launch Claude Code
claude

# In Claude, use the /spec command
/spec specs/my-first-feature.md
```

Claude will:
- Ask clarifying questions about your feature
- Help you define clear acceptance criteria
- Ensure nothing is ambiguous

**Example interaction:**
```
You: /spec specs/my-first-feature.md

Claude: I'm reviewing your spec. I have a few questions:

1. What should happen when a user tries to [edge case]?
2. How do we verify that [requirement] is met?
3. Is [behavior] in scope for this feature?

You: [Answer the questions]

Claude: [Updates spec with clear acceptance criteria]
```

### 4. Validate the Spec

```bash
./scripts/validate-spec.sh specs/my-first-feature.md
```

This ensures:
- All required sections are complete
- Acceptance criteria are defined
- No placeholders remain

### 5. Generate Implementation Plan

```bash
claude

# In Claude
/plan specs/my-first-feature.md
```

Claude will:
- Explore your codebase
- Understand existing patterns
- Break the spec into small, sequential tasks
- Create `specs/my-first-feature-plan.md`

### 6. Execute the Implementation Loop

```bash
# In Claude
/implement specs/my-first-feature-plan.md
```

This runs the three-agent cycle:

**Engineer Agent:**
- Implements the next task
- Follows the spec exactly
- Makes incremental changes

**Tester Agent:**
- Runs tests
- Validates acceptance criteria
- Reports any failures

**Reviewer Agent:**
- Checks spec alignment
- Identifies security issues
- Flags edge cases

### 7. Iterate Until Complete

The `/implement` command runs one task at a time. After each task:

- Review the changes
- Confirm the commit
- Run `/implement` again for the next task

**Continue until:**
```
✓ All tasks complete
✓ All tests passing
✓ All acceptance criteria met
✓ Code review approved
```

## Example: End-to-End Flow

Let's build a simple "Hello Screen" feature:

### Step 1: Create Spec

```bash
./scripts/new-spec.sh hello-screen
```

Edit `specs/hello-screen.md`:

```markdown
# Feature: Hello Screen

## 1. Problem statement
Need a simple welcome screen that displays a greeting.

## 2. Goals
- G1: Create a Hello Screen

## 3. Non-goals
- NG1: User input
- NG2: Custom greetings

## 4. Functional requirements
- R1: Screen displays "Hello, World!" text
- R2: Text is centered

## 5. Acceptance criteria
- A1: Screen displays "Hello, World!"
- A2: Text element is centered in the view
- A3: (iOS) VoiceOver reads "Hello, World!"
- A4: (Android) TalkBack reads "Hello, World!"

## 6. Constraints & invariants
- C1: Use standard platform typography
- C2: Follow system theme (light/dark mode)

## 7. Open questions
- None
```

### Step 2: Refine with Claude

```bash
claude

# In Claude:
> /spec specs/hello-api.md
```

### Step 3: Generate Plan

```bash
# In Claude:
> /plan specs/hello-screen.md
```

Claude creates `specs/hello-screen-plan.md`:

```markdown
## Task 1: Create Hello Screen
- Implement basic UI with "Hello, World!" text
- Ensure centering
- Add accessibility labels

## Task 2: Add tests
- Verify text presence
- Verify accessibility attributes
```

### Step 4: Implement

```bash
# In Claude:
> /implement specs/hello-screen-plan.md
```

**What happens:**

1. Engineer implements Task 1
2. Tester validates the implementation
3. Reviewer checks spec alignment
4. Changes are committed
5. Process repeats for Task 2, then Task 3

### Step 5: Done!

```
✓ Feature complete
✓ All tests passing
✓ Spec requirements met
✓ Ready for deployment
```

## Tips for Success

### Write Clear Acceptance Criteria

**Bad:**
```
- A1: API should work correctly
```

**Good:**
```
- A1: GET /api/hello returns 200 status
- A2: Response contains "message" and "timestamp" fields
- A3: Timestamp is ISO 8601 format in UTC timezone
```

### Start Small

Your first spec should be simple:
- 2-3 requirements
- 3-5 acceptance criteria
- Can be implemented in 1-2 hours

### Use the Examples

Reference `specs/example-todo-api.md` to see:
- How to structure complex specs
- How to write testable acceptance criteria
- How to document constraints

### Ask Questions

When using `/spec`, don't hesitate to:
- Ask Claude for clarification
- Request examples
- Discuss edge cases

### Iterate on CLAUDE.md

As you work, you'll discover project-specific patterns. Add them to `CLAUDE.md`:

```markdown
## Project-Specific Patterns

- All API routes use Express.js Router
- Tests use Jest with supertest
- Database access uses Prisma ORM
```

## Common Issues

### "Claude is inventing requirements"

**Solution:** Your spec is too vague. Use `/spec` to refine it with explicit acceptance criteria.

### "Tests keep failing"

**Solution:**
1. Check if acceptance criteria are actually met
2. Review test implementation against spec
3. Update spec if requirements were unclear

### "Changes are too large"

**Solution:** Break tasks into smaller pieces in your plan. Each task should be < 3 hours of work.

## Next Steps

- Read [docs/workflow.md](./workflow.md) for detailed workflow explanation
- See [docs/examples/](./examples/) for more complete examples
- Review [docs/best-practices.md](./best-practices.md) for advanced patterns
- Check [CLAUDE.md](../CLAUDE.md) for complete reference

## Getting Help

1. Check the [troubleshooting guide](./troubleshooting.md)
2. Review existing specs in `specs/` for patterns
3. Ask Claude: "How should I structure a spec for [your use case]?"
