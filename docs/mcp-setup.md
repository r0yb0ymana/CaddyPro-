# MCP Server Setup Guide

Model Context Protocol (MCP) servers extend Claude Code with additional capabilities like database access, API integrations, and custom tools.

## What is MCP?

MCP servers provide Claude with:
- **Database access** - Query PostgreSQL, SQLite, etc.
- **API integrations** - GitHub, Slack, custom APIs
- **Browser automation** - Screenshots, testing with Puppeteer
- **Web search** - Brave Search, Google, etc.
- **Custom tools** - Your own integrations

## Configuration

### Option 1: Project-Specific (Recommended for Templates)

Create `.mcp.json` in your project root:

```bash
# Copy the template
cp .mcp.json.template .mcp.json

# Edit to enable servers you need
vim .mcp.json
```

**Benefits:**
- Checked into git (team-wide access)
- Different config per project
- Easy to share and replicate

### Option 2: Global Configuration

Add to `~/.claude.json`:

```json
{
  "mcpServers": {
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "DATABASE_URL": "postgresql://localhost/mydb"
      }
    }
  }
}
```

**Benefits:**
- Available in all projects
- Personal tools and credentials
- No need to configure per project

## Common MCP Servers

### Database Access

#### PostgreSQL

```json
{
  "mcpServers": {
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "DATABASE_URL": "postgresql://user:pass@localhost:5432/dbname"
      }
    }
  }
}
```

**Use cases:**
- Query database to understand schema
- Verify data changes during implementation
- Run SQL to validate acceptance criteria
- Generate test data

**Example usage:**
```
You: "Query the users table to see the schema"
Claude: [Uses postgres MCP server]
Claude: "The users table has columns: id, email, password_hash, created_at"
```

#### SQLite

```json
{
  "mcpServers": {
    "sqlite": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-sqlite", "./data/app.db"]
    }
  }
}
```

---

### GitHub Integration

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_TOKEN": "ghp_your_personal_access_token"
      }
    }
  }
}
```

**Use cases:**
- Create issues from spec sections
- Update PRs with implementation notes
- Close issues when tasks complete
- Triage and categorize issues

**Example usage:**
```
You: "Create a GitHub issue for implementing specs/user-auth.md"
Claude: [Uses GitHub MCP server]
Claude: "Created issue #42: Implement User Authentication"
```

**Required permissions:**
- `repo` scope for private repos
- `public_repo` for public repos only

---

### Browser Automation (Puppeteer)

```json
{
  "mcpServers": {
    "puppeteer": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
    }
  }
}
```

**Use cases:**
- Screenshot current UI state
- Visual regression testing
- Iterate on UI to match designs
- Verify responsive layouts

**Example usage:**
```
You: "Screenshot the login page at localhost:3000/login"
Claude: [Uses Puppeteer MCP server]
Claude: [Shows screenshot]
Claude: "The login form is centered and matches the design mock"
```

**Workflow pattern:**
1. Implement UI component
2. Screenshot result
3. Compare to design
4. Iterate until matching
5. Commit

---

### Web Search

```json
{
  "mcpServers": {
    "brave-search": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
      "env": {
        "BRAVE_API_KEY": "your_api_key_from_brave.com"
      }
    }
  }
}
```

**Use cases:**
- Research libraries and best practices
- Find documentation for APIs
- Look up error messages
- Discover solutions to problems

**Example usage:**
```
You: "Search for best practices for JWT token storage"
Claude: [Uses Brave Search MCP server]
Claude: "Found recommendations: Use httpOnly cookies for web apps..."
```

**Get API key:**
- Sign up at https://brave.com/search/api/

---

### HTTP Fetch

```json
{
  "mcpServers": {
    "fetch": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-fetch"]
    }
  }
}
```

**Use cases:**
- Test your API endpoints
- Fetch documentation from URLs
- Validate external API integrations
- Check service health

**Example usage:**
```
You: "Test POST /api/users with sample data"
Claude: [Uses fetch MCP server]
Claude: "Response: 201 Created with user object. Matches acceptance criterion A1."
```

---

### Persistent Memory

```json
{
  "mcpServers": {
    "memory": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-memory"]
    }
  }
}
```

**Use cases:**
- Remember project decisions across sessions
- Store context about codebase
- Track implementation patterns
- Maintain todo lists between sessions

**Example usage:**
```
You: "Remember that we use Prisma ORM for all database access"
Claude: [Uses memory MCP server]
Claude: "Stored: Database access pattern uses Prisma ORM"

[Later session]
You: "How do we access the database?"
Claude: [Retrieves from memory]
Claude: "We use Prisma ORM for all database access"
```

---

## Security Best Practices

### DO NOT commit secrets to `.mcp.json`

**Bad:**
```json
{
  "mcpServers": {
    "postgres": {
      "env": {
        "DATABASE_URL": "postgresql://admin:secretpassword@localhost/prod"
      }
    }
  }
}
```

**Good:**
```json
{
  "mcpServers": {
    "postgres": {
      "env": {
        "DATABASE_URL": "${DATABASE_URL}"
      }
    }
  }
}
```

Then set in your shell:
```bash
export DATABASE_URL="postgresql://admin:secretpassword@localhost/prod"
```

### Use environment variables

```bash
# In ~/.bashrc or ~/.zshrc
export GITHUB_TOKEN="ghp_your_token"
export DATABASE_URL="postgresql://localhost/mydb"
export BRAVE_API_KEY="your_key"
```

### Keep secrets in global config

Put sensitive configs in `~/.claude.json` (not checked into git):

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  }
}
```

### Team configuration template

In `.mcp.json.template` (checked into git):

```json
{
  "mcpServers": {
    "postgres": {
      "comment": "Set DATABASE_URL environment variable",
      "disabled": true,
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "DATABASE_URL": "${DATABASE_URL}"
      }
    }
  }
}
```

Team members copy and configure:
```bash
cp .mcp.json.template .mcp.json
# Edit .mcp.json with your credentials (not committed)
```

Add to `.gitignore`:
```
.mcp.json
```

---

## Enabling MCP Servers

### Enable a server

Edit `.mcp.json` and change `"disabled": true` to `"disabled": false`:

```json
{
  "mcpServers": {
    "postgres": {
      "disabled": false,  // ← changed from true
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "DATABASE_URL": "postgresql://localhost/mydb"
      }
    }
  }
}
```

Or remove the `disabled` field entirely.

### Restart Claude Code

```bash
# Exit Claude
exit

# Restart
claude
```

### Verify server loaded

```
You: "List available MCP servers"
Claude: "The following MCP servers are available:
- postgres: PostgreSQL database access
- ..."
```

---

## Debugging MCP Issues

### Use --mcp-debug flag

```bash
claude --mcp-debug
```

Shows:
- Which servers loaded successfully
- Configuration errors
- Connection issues
- Server stderr output

### Common issues

**Server not loading:**
- Check `"disabled": false`
- Verify command and args are correct
- Ensure npx packages are installable

**Connection errors:**
- Verify credentials (tokens, passwords)
- Check URLs and ports
- Ensure services are running (database, etc.)

**Permission errors:**
- Verify environment variables are set
- Check file permissions
- Ensure tokens have required scopes

---

## Custom MCP Servers

You can create your own MCP servers for project-specific tools.

### Example: Custom Test Runner

```javascript
// scripts/mcp-test-runner.js
const { Server } = require('@modelcontextprotocol/sdk/server');

const server = new Server({
  name: 'test-runner',
  version: '1.0.0',
});

server.tool('run-tests', async (args) => {
  const { file } = args;
  // Run tests and return results
  const results = await runTests(file);
  return { results };
});

server.listen();
```

### Configure in .mcp.json

```json
{
  "mcpServers": {
    "test-runner": {
      "command": "node",
      "args": ["scripts/mcp-test-runner.js"]
    }
  }
}
```

### Use in workflow

```
You: "Run tests for the login feature"
Claude: [Uses test-runner MCP server]
Claude: "Tests passed: 8/8. All acceptance criteria validated."
```

---

## Recommended Setup for This Template

For spec-driven development, recommended MCP servers:

### Minimal Setup

```json
{
  "mcpServers": {
    "fetch": {
      "comment": "Test API endpoints",
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-fetch"]
    }
  }
}
```

### With Database

```json
{
  "mcpServers": {
    "fetch": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-fetch"]
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "DATABASE_URL": "${DATABASE_URL}"
      }
    }
  }
}
```

### With UI Development

```json
{
  "mcpServers": {
    "fetch": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-fetch"]
    },
    "puppeteer": {
      "comment": "Screenshots for visual iteration",
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
    }
  }
}
```

### Full Stack Development

```json
{
  "mcpServers": {
    "fetch": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-fetch"]
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "DATABASE_URL": "${DATABASE_URL}"
      }
    },
    "puppeteer": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  }
}
```

---

## Resources

- [MCP Documentation](https://modelcontextprotocol.io/)
- [Available MCP Servers](https://github.com/modelcontextprotocol/servers)
- [Building Custom Servers](https://modelcontextprotocol.io/docs/building-servers)
- [Claude Code MCP Integration](https://docs.anthropic.com/claude/docs/claude-code#mcp-integration)
