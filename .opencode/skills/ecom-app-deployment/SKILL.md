---
name: ecom-app-deployment
description: 'Prepare ECOM Kubernetes deployment tickets through guided questions, repository evidence, and an editable Markdown draft. Use when requesting or reviewing a deployment ticket, not to deploy or integrate the ticket system. English triggers include "request an environment", "ECOM deployment", "deploy ticket", "provision environment". Spanish triggers include "solicitar entorno", "pedir ambiente", "despliegue ecom", "deploy en ecom", "ticket de deploy", "solicitud de despliegue", "provisionar ambiente".'
metadata:
  version: "1.2.0"
  short-description: Prepare ECOM deployment requests
---

# ECOM application deployment requests

## Activation Contract

Guide the developer toward a title and body to copy manually into Gestión Tickets. Work on one repository and one environment per ticket. Do not submit tickets, deploy applications, or integrate services.

## Hard Rules

- Read [the current drafting rules](references/request-checklist.md) before asking questions or drafting. They supersede the historical wiki copy for this workflow.
- Use the invocation directory to identify the target repository. Never combine frontend and backend repositories. Confirm the application name, repository, and environment.
- Gather evidence without executing project code. Never infer an exact version from a version range or invent unavailable facts.
- Never request, read, reproduce, or save raw secrets or secret-bearing configuration values. Inspect variable names in sanitized examples only, using a names-only projection that never returns assignment values; the `.env.example` filename alone does not prove sanitization. Keep secrets out of chat, tickets, and generated Markdown; use PWPush or company Passbolt.
- Drafting authorizes no access to Vault, Argo CD, Kubernetes, production, or internal services, and no changes to code, branches, infrastructure, or configuration.
- Preserve the requested architecture and resources. Do not replace explicit requirements with platform defaults.
- Write for a junior developer: ask short, specific questions, explain unfamiliar terms when first needed, and offer “No lo sé / Por confirmar” instead of guessing. Explain each missing item as what is missing, why it matters, and the next action. Keep technical investigation details out of the main guidance; retain only useful file evidence and explain limits plainly.
- Keep preflight evidence and guidance outside the copyable ticket body. Use the reference's traffic-light checklist: 🔴 Falta, 🟡 Por confirmar, 🟢 Comprobado. Highlight confirmed missing items and other priority concerns before the table, with a concrete next step. Never turn uncertainty into a confirmed failure. File existence is not proof of functionality; branch existence is not proof of freshness.

## Decision Gates

| Situation | Action |
| --- | --- |
| No Git repository or no usable remote URL | Ask for the repository URL; never silently choose another project. |
| Multiple candidate names or repositories | Ask the developer to confirm the target, with a manual alternative. |
| Missing or inaccessible evidence | Mark missing or not verified, explain what is needed; do not fabricate success. |
| Unanswered ticket field | Ask only for missing information; if deferred, label the document as a draft with pending fields. |
| Wiki contradicts this workflow | Use the current drafting rules; do not present the old wiki as updated policy. |

## Execution Steps

1. Identify the repository and propose the application name from context for confirmation or manual correction.
2. Ask TEST or PROD, normalizing testing/test and producción/production/prod. Gather repository evidence and the separate Docker/branch/configuration checklist, including effective `WORKDIR /code` and framework-specific `.env.example` variable names as defined in the reference.
3. Show concise framework/language findings with versions. Ask only about unresolved facts.
4. Follow the reference's question blocks for DNS, access, manual commands, database, cache, persistence, developers, secure configuration delivery, and observations. Retain answers; do not repeat resolved questions.
5. Present a summary for confirmation. Generate an editable Markdown file from [the ticket template](assets/deployment-request.md), replacing placeholders with confirmed answers. Use a safe repository-local filename such as `solicitud-despliegue-<slug>-<test|prod>.md`; do not overwrite an existing document without agreement.
6. Return its path, pending details, and preflight findings. Explain where to copy the title and body; never claim submission or deployment.

## Output Contract

Produce Spanish ticket content matching the actual form. Separate navigation instructions, title, copyable body, and a non-copyable checklist with evidence, warnings, and reminders. Always retain the template's developer tools and access section outside the ticket body, including environment-specific addresses and the leader-owned administrative request route. If file writing is unavailable, provide the Markdown in the response and explicitly state it was not saved.

## References

- [Current drafting rules and preflight checks](references/request-checklist.md).
- [Editable ticket template](assets/deployment-request.md).
- [Historical wiki copy](references/solicitar-entorno-para-app.md): background only; not the current template or navigation instructions.
