# Changelog

All notable changes to this skill are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
The version applies to this skill, not the entire repository.

This history was recorded retrospectively. Version 1.1.0 describes the improvements integrated on 2026-10-31; 1.0.0 identifies the preceding behavior. No historical release tag or publication date is asserted for 1.0.0.

## [Unreleased]

## [1.2.0] - 2026-11-03

### Added

- Read-only preflight guidance for the deployed Dockerfile stage's effective `WORKDIR /code`, with actionable suggestions and explicit uncertainty for unresolved inheritance or targets.
- Separate `.env.example` existence and names-only checks against FastAPI database and Django variable groups, without exposing values or editing consumer projects.

## [1.1.0] - 2026-10-31

### Added

- Guided questions with confirmation of the application, repository, and TEST or PROD environment.
- Editable Spanish Markdown containing the ticket title, body, and manual submission instructions.
- A separate, junior-friendly checklist with traffic-light statuses, priority warnings, and next actions.
- Developer references for Argo CD, TEST-only Vault access, Passbolt, and the team leader's access-request route.

### Changed

- Scope narrowed to drafting one ticket per repository and environment, without ticket-system integration or deployment execution.
- DNS proposals derived from the repository slug, removing a final `-web` or `_web` while preserving API suffixes.
- Explicit questions for manual commands, database creation or reuse, cache, current persistence needs, developers, secure configuration delivery, and observations.
- Docker and branch checks kept outside the ticket body, distinguishing observed presence from functionality and current GitLab state.
- Questions and explanations simplified for developers new to deployment.
- The older wiki copy retained as historical background instead of the current ticket template.

## [1.0.0]

### Added

- Initial guidance for preparing ECOM environment and deployment requests.
- Repository-readiness checklist and a local copy of the deployment wiki.
- Security boundaries for secret sharing and operational access.
