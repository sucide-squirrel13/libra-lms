# LIBRA LMS — Shared Agent Instructions

## Project identity
LIBRA is a college-level Library Management System.

## Architecture
- Preserve the existing Spring Boot architecture.
- Use Java and the existing PostgreSQL database.
- Follow the existing project structure and coding conventions.
- Do not introduce unnecessary frameworks, dependencies or abstractions.

## Development rules
1. Inspect existing code before modifying it.
2. Understand existing functionality before proposing changes.
3. Make focused, minimal changes.
4. Preserve existing backend integrations and database behaviour.
5. Never expose, overwrite or commit secrets and credentials.
6. Do not modify environment or deployment configuration without approval.
7. Explain significant changes and their purpose.
8. Run relevant tests after implementation.
9. Report failed tests honestly; never claim unexecuted tests passed.

## Agent collaboration
- OpenCode is the primary implementation agent.
- Freebuff acts as an independent reviewer and assistant.
- Freebuff should review plans, code changes and test results.
- OpenCode handles approved implementation changes.
- Never allow both agents to modify the same files simultaneously.
- Review suggestions before applying them.

## UI/UX
- Use UI/UX Pro Max for design guidance.
- Use 21st.dev skills when appropriate for component discovery and development.
- Maintain consistent navigation, typography, spacing and colours.
- Make interfaces responsive and accessible.
- Preserve existing application functionality.

## Safety
- Do not delete project files or database records without approval.
- Do not run destructive Git commands without approval.
- Do not deploy changes without approval.
