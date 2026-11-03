---
name: antd
description: >
  Use when the user's task involves Ant Design (antd) — writing antd components,
  debugging antd issues, querying antd APIs/props/tokens/demos, migrating between
  antd versions, or analyzing antd usage in a project. Triggers on antd-related
  code, imports from 'antd', or explicit antd questions.
allowed-tools:
  - Bash(antd *)
  - Bash(which antd)
---

# Ant Design CLI

> **Xile Spa copy.** Source: `ant-design/ant-design-cli`, file `skills/antd/SKILL.md`, commit `9e4e358` (MIT
> license, © Ant Design team). Changed from the original: no self-install or self-upgrade of the CLI, bug-report sections
> removed (original scenarios 9, 10, 11 and key rules 6, 7), migration/changelog/env scenarios shortened, MCP example
> set to v6, "Xile Spa conventions" added. Update this copy in a separate `chore` PR.

## Xile Spa conventions (override everything below)

- The project uses **antd 6** (`frontend/package.json`): add `--version 6` to every lookup, MCP included.
- **Never install or upgrade** `@ant-design/cli` yourself (`npm install -g`, `antd upgrade`). CLI missing (`which antd`
  prints nothing) or an update notice appears → stop and ask the user to run it; tools are the card owner's call
  (`AGENTS.md` section 3).
- **Never file bug reports** to GitHub (`antd bug`, `antd bug-cli`), even when suggested.
- Repo rules beat antd examples: structure and CSS per `frontend/AGENTS.md` (CSS Modules, colors and spacing from
  `tokens.css`/`theme.ts`, no raw colors); layout, copy and states per `docs/design/ux-guidelines.md`. `antd demo` is a
  starting point only; never copy a demo's inline styles.
- Run `antd lint <changed file> --format json` after editing components; fix `deprecated` findings in the same part.

## Setup

Before first use, check that the CLI is installed:

```bash
which antd
```

If it is missing, or a command prints an "Update available" notice, stop and ask the user to install or upgrade it
(see "Quy ước Xile Spa"). Do not install or upgrade it yourself.


**Always use `--format json` for structured output you can parse programmatically.**

## Scenarios

### 1. Writing antd component code

Before writing any antd component code, look up its API first — don't rely on memory.

```bash
# Check what props are available
antd info Button --format json

# Get a working demo as starting point
antd demo Button basic --format json

# Check semantic classNames/styles for custom styling
antd semantic Button --format json

# Check component-level design tokens for theming
antd token Button --format json

# Get the overall design language (design.md): colors, typography, spacing, radius + principles
antd design.md --format json
```

**Workflow:** `antd info` → understand props → `antd demo` → grab a working example → write code.

### 2. Looking up full documentation

When you need comprehensive component docs (not just props):

```bash
antd doc Table --format json        # full markdown docs for Table
antd doc Table --lang zh            # Chinese docs
```

### 3. Debugging antd issues

`antd info <Component> --version 6 --format json` (does the prop exist in our version?), `antd lint <file> --format json`
(deprecated or wrong usage), `antd doctor --format json` (project configuration), `antd env --format json` (environment).

### 4. Version changes

Upgrading antd is a library change: ask first (`AGENTS.md` section 3). To understand a change:
`antd changelog <v1> <v2> [Component] --format json`, `antd migrate <from> <to> [--component X] --format json`.

### 5. Analyzing usage and choosing components

`antd usage ./src [--filter Form] --format json` (usage statistics), `antd lint ./src [--only deprecated|a11y|performance]
--format json` (best-practice violations), `antd list --version 6 --format json` (components by category).

### 6. Using as MCP server

If working in an IDE that supports MCP (Claude Desktop, Cursor, etc.), the CLI can also run as an MCP server, exposing all knowledge-query tools directly:

```json
{
  "mcpServers": {
    "antd": {
      "command": "antd",
      "args": ["mcp", "--version", "6"]
    }
  }
}
```

This provides 8 tools (`antd_list`, `antd_info`, `antd_doc`, `antd_demo`, `antd_token`, `antd_design_md`, `antd_semantic`, `antd_changelog`) and 2 prompts (`antd-expert`, `antd-page-generator`) via MCP protocol.

## Global Flags

| Flag | Purpose |
|---|---|
| `--format <format>` | Output format: `json`, `text`, or `markdown` (agents should prefer `json`) |
| `--version <v>` | Target a specific antd version (e.g. `5.20.0`) |
| `--lang zh` | Chinese output (default: `en`) |
| `--detail` | Include extra fields (description, since, deprecated, FAQ) |
| `-V, --cli-version` | Print CLI version and exit |

## Key Rules

1. **Always query before writing** — Don't guess antd APIs from memory. Run `antd info` first.
2. **Match the user's version** — Knowledge queries (`list/info/doc/demo/token/semantic/changelog`) support antd v4+. If the project uses antd 4.x/5.x/6.x, pass `--version 4.24.0` / `5.24.0` / `6.x`. For antd v3 projects, use `antd migrate 3 4` first.
3. **Use `--format json`** — Every command supports it. Parse the JSON output rather than regex-matching text output.
4. **Check before suggesting migration** — Run `antd changelog <v1> <v2>` and `antd migrate` before advising on version upgrades.
5. **Lint after changes** — After writing or modifying antd code, run `antd lint` on the changed files to catch deprecated or problematic usage.
