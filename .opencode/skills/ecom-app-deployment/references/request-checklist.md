# Current deployment ticket drafting rules

Use these maintainer-confirmed rules for the guided workflow. The wiki copy is outdated; do not restore its old fields or navigation steps as mandatory requirements.

## Communication style — guide a junior developer

Use plain, specific Spanish in questions, summaries, and generated Markdown. Treat the developer as capable but new to the deployment process; do not assume knowledge of infrastructure or Git internals.

- Ask for one decision per question, in small groups. Add one short explanation or concrete example only when it helps answer.
- Explain a necessary term on first use: DNS is the address used to reach the application; persistent storage keeps files between deployments. Keep exact names and versions in the ticket, without adding tutorials to its fields.
- Offer “No lo sé / Por confirmar” when the answer is uncertain. Help find evidence or suggest asking the team leader; do not require a junior developer to invent a command, service, or file path.
- For a checklist warning, say **what is missing → why it matters → what to do next**. Keep it short and actionable; do not turn the checklist into a technical audit.
- Show a useful filename as evidence when available. Do not dump ref names, commit hashes/counts, graph comparisons, or raw command output into the main checklist. Keep those details for an explicit technical follow-up.
- Say “Encontré la rama en la información guardada en tu equipo; falta confirmar cómo está en GitLab” rather than implying a live remote check. Use the traffic-light statuses below with a brief explanation. A green existence check confirms presence only, not correct operation.
- Separate a missing item from something not checked. “No encontré la rama test en este equipo” must not become “La rama test no existe en GitLab”.
- Keep all existing checks, security rules, and access restrictions. Simpler language must not imply that a project is ready or a permission is granted.

Examples of user-facing checklist guidance:

| Situation | Plain guidance |
| --- | --- |
| No Dockerfile/Compose found | “No encontré Dockerfile ni Compose. Estos archivos indican cómo preparar la aplicación para ejecutarla. Consulta con tu equipo cuál deben preparar antes del despliegue.” |
| No test branch in the clone | “No encontré test en la información de este equipo. Esa rama se usa para pruebas. Pide a tu equipo que confirme si está en GitLab y que tenga los cambios a desplegar.” |
| Branch exists but freshness is unknown | “Encontré información de test, pero no confirmé que tenga tus últimos cambios. Antes de pedir el despliegue, revísalo con tu equipo.” |
| No manual commands requested, automation unclear | “Indicaste que no hay comandos manuales. Falta confirmar cómo se actualizará la base de datos al desplegar; revísalo con tu equipo.” |

## Gather evidence, then ask

Start by confirming the application name: propose a name from the project context and allow manual entry. Identify the repository from the invocation directory and its Git remote. If the directory is not a repository, ask for its URL. If remotes disagree, ask which is the deployment target. Use a clean repository URL without embedded credentials; never echo credentials from remote configuration.

Always prepare one ticket per repository and environment. Never merge frontend/backend requests. Normalize TEST/testing and PROD/production/producción to TEST or PROD; no other environments are supported.

Read only relevant manifests, lockfiles, container definitions, and sanitized configuration examples. Retain repository-relative paths and useful line numbers for evidence; present only what helps the developer understand the finding, following the communication style above. Do not read real `.env` files or secret values. Do not execute builds, migrations, containers, or deployment commands.

For framework and language, output only their names and versions. Distinguish pinned versions, constraints, and the deployed runtime; ask when the exact version is unresolved. Do not list minor libraries. If no framework is used, confirm and write `No utiliza framework`.

## Preflight checklist — outside the ticket

| Check | Evidence and interpretation |
| --- | --- |
| Dockerfile or Docker Compose | Look for Dockerfile variants and `compose.yaml`, `compose.yml`, `docker-compose.yaml`, `docker-compose.yml`, including project-specific variants/subdirectories. List actual paths and identify relevance to the target. At least one relevant file satisfies this existence check; both are not required. If neither exists, warn the requester. Do not claim the file works merely because it exists. |
| Container working directory | Inspect the relevant Dockerfile and determine the effective working directory of the deployed stage. Require `WORKDIR /code`; report this separately from file existence. Follow the rules below. |
| Configuration example | Locate the target application's `.env.example`, including relevant subdirectories. Check its existence separately from variable-name compliance. Follow the names-only inspection and framework rules below. |
| Target branch | For TEST check `test`; for PROD check `main`. Distinguish local branches from remote-tracking refs. Record the ref inspected; if absent locally, remote existence remains not verified unless fresh remote evidence is available. Never create a branch. |
| Branch freshness | Remind the requester that intended changes must be committed and pushed to the target branch. Local or cached refs alone do not prove remote freshness. Mark freshness not verified unless current evidence establishes it; do not fetch, push, or switch branches automatically. |

### Docker working directory

Infrastructure requires `WORKDIR /code`. This is a read-only check and a suggestion, never permission to edit or build the project.

- Identify the Dockerfile and stage actually used by the deployment, using Compose's build context, dockerfile, and target when available. Do not treat a Compose `working_dir` as a replacement for the Dockerfile requirement; flag a conflicting override separately.
- Check active instructions, not comments or a text match anywhere in the file. An earlier `WORKDIR /code` followed by another working directory does not satisfy the check. In a multi-stage build, a builder's working directory alone does not prove the runtime stage's directory.
- Green requires evidence that the deployed stage's effective working directory is exactly `/code`. Resolve local-stage inheritance and relative instructions only when the file makes the result clear. If the stage, base-image default, or variable expansion cannot be resolved without running code or accessing services, mark yellow and explain what needs confirmation.
- If the relevant stage has a confirmed different directory, mark red and suggest `WORKDIR /code`. If no WORKDIR is declared and inheritance cannot be established, mark yellow and ask the team to declare `WORKDIR /code` explicitly. With Compose alone or an external image, file existence may be green while the Dockerfile check remains yellow.
- Explain that changing this directory may require reviewing relative `COPY`, startup commands, and volume paths. Do not rewrite them or promise that changing one line guarantees a working container.

Example guidance: “El contenedor usa otra carpeta de trabajo. Infraestructura solicita `WORKDIR /code`. Pide a tu equipo ajustar el Dockerfile y revisar las rutas que dependen de esa carpeta.”

### Configuration example — names only

Check the relevant `.env.example`, not a real `.env`, and never assume that an example contains safe values.

1. Locate candidate paths without opening their contents. If multiple examples belong to different services, select the target application's example using repository evidence; ask if ambiguous. Confirmed absence in the inspected target scope is red; inaccessible or unresolved scope is yellow.
2. Extract only assignment **names** with a bounded local text-processing command, without executing or sourcing the file or importing project code. Output only the path, line number, and valid key before the first `=`; allow optional leading whitespace and `export `. Ignore blank lines and comments. Never return right-hand values, inline comments, whole source lines, or malformed line contents. For unsupported syntax, report only affected line numbers as unverified. Do not use a raw file reader, grep output containing assignments, shell tracing, or diagnostics that echo source contents. If a safe projection is unavailable, mark content yellow and request a sanitized list of names only.
3. Compare exact, case-sensitive names against the applicable list below. Empty values are valid: this check is about names, not whether credentials or settings are populated. Do not test passwords, ports, hosts, URLs, booleans, or secret strength. Never reproduce values in chat, evidence, ticket, or Markdown.
4. Inspect only relevant, non-secret configuration code to identify which variable names the application consumes. Do not open real environment files. An example's naming compliance does not prove the application reads those names or that the deployment works. If consumption cannot be established safely, mark that verification yellow.
5. Report missing expected names and confirmed mismatched aliases by name only. Do not require every variable to carry the framework prefix: unrelated third-party or system variables may be valid. Do not infer the framework solely from prefixes or require either framework's list for another stack.
6. A confirmed required-name mismatch is red. If a group may not apply (for example, no database, superuser provisioning, or media), ask whether it is used before declaring those names mandatory; record justified non-applicability explicitly. Unknown framework or applicability is yellow, not a guessed failure. Green requires all applicable names present, without claiming their values were checked.
7. Suggest additions as blank assignments only and explain confirmed renames as old name → expected name. The developer must review both the example and application configuration together; never rename automatically, discard custom keys, or suggest that a cosmetic example-only edit fixes runtime behavior. Keep all suggestions outside the ticket.

#### FastAPI expected database names

For a FastAPI application using a database, compare these exact names:

```dotenv
FASTAPI_DB_HOST=
FASTAPI_DB_PORT=5432
FASTAPI_DB_DATABASE=
FASTAPI_DB_USERNAME=
FASTAPI_DB_PASSWORD=
```

The maintainer's `5432` is an example, not a required or verified port and not a reason to infer the database engine. Generated missing-key suggestions use `FASTAPI_DB_PORT=` like other blank assignments. If the application has no database, record that this group does not apply.

#### Django expected names

Compare these exact groups against the confirmed application needs:

```dotenv
# BASE
DJANGO_SECRET_KEY=
DJANGO_DEBUG=
DJANGO_ALLOWED_HOSTS=
DJANGO_CSRF_TRUSTED_ORIGINS=
DJANGO_CORS_ALLOWED_ORIGINS=
DJANGO_CORS_ALLOW_CREDENTIALS=

# DATABASE
DJANGO_DB_HOST=
DJANGO_DB_PORT=
DJANGO_DB_DATABASE=
DJANGO_DB_USERNAME=
DJANGO_DB_PASSWORD=

# SUPERUSER
DJANGO_SUPERUSER_USERNAME=
DJANGO_SUPERUSER_EMAIL=
DJANGO_SUPERUSER_PASSWORD=

# MEDIA
DJANGO_MEDIA_ROOT=
```

Section comments and ordering are illustrative, not validation requirements. If database, CORS, superuser, or media handling is not used or applicability is uncertain, clarify that group's applicability rather than inventing a requirement. Never generate a secret key or superuser credentials.

Example guidance: “Encontré `.env.example`, pero falta `DJANGO_DB_HOST` para la base de datos confirmada. Agrega ese nombre con el valor vacío y revisa con tu equipo que la aplicación lo lea. Las credenciales se comparten solo por PWPush o Passbolt.”

### Traffic-light checklist

Use an icon **and a text label**, so the status remains understandable without color:

| Status | When to use it | What to show |
| --- | --- | --- |
| **🔴 Falta** | A required item is confirmed absent or a check has a confirmed problem within the inspected scope. | Bold the missing item or problem, explain why it matters, and give the next action. |
| **🟡 Por confirmar** | There is doubt, incomplete information, an inaccessible check, or a required verification has not been performed. | Explain what remains unknown and how the developer can confirm it with the team. |
| **🟢 Comprobado** | Evidence establishes that this specific check is satisfied. | State exactly what was checked and give brief evidence; never imply broader deployment readiness. |

Before the checklist table, include **Atención prioritaria** with every red item first. Highlight its name in bold and give one short next step. Also highlight any yellow concern needing special attention, such as an unresolved step required before deployment, but keep it yellow until its status is known. Do not invent priority concerns or hide them in a long evidence section. If there are no priority items, say so without claiming the whole application is ready.

Sort table rows red → yellow → green. An existing Dockerfile may be green for **file existence** while its functionality remains untested. A branch visible only in saved local information does not establish its current state in GitLab; freshness stays yellow without current evidence. If a branch is absent from the local listing and the server was not checked, use yellow, not a claim of confirmed remote absence.

Explain the inspected scope in plain language. When only a URL is available, ask for evidence or use yellow; do not clone or access private services automatically. Keep missing prerequisites visible even when the user requests a draft. Do not add the colors or priority guidance to the copyable deployment ticket body.

## Guided question blocks

Ask in small groups, not one exhaustive questionnaire. Offer evidence-based suggestions, a manual answer, and explicit negative choices where applicable. Use plain questions if structured choices cannot represent the answer. Never require the developer to re-enter a known confirmed fact.

### DNS and access

Derive DNS proposals from the repository **slug** (last URL segment, not a Git tag). Normalize an SSH/HTTPS remote to its clean repository identity; remove trailing slash and transport suffix `.git` before extracting the slug. Remove only a final `-web` or `_web` for the DNS proposal. Preserve `-api`, `_api`, and occurrences of “web” elsewhere. Do not rename the repository or modify its URL.

| Environment | Proposal |
| --- | --- |
| TEST | `<normalized-slug>.ecomdev.ar` |
| PROD, government project | Ask whether the full hostname should use `.chaco.gob.ar`, `.gob.ar`, or a manually supplied domain. Government status alone does not select the suffix. |
| PROD, other project | Propose `<normalized-slug>.ecom.com.ar`, allowing manual correction. |

Confirm government/non-government status for PROD when unknown, then confirm the proposed hostname or accept manual input. Output DNS as a hostname, not a URL with a scheme.

Examples for `https://git.ecom.com.ar/equipo21/sige/<slug>`:

| Slug | TEST | PROD with confirmed .chaco.gob.ar |
| --- | --- | --- |
| sige-produccion | sige-produccion.ecomdev.ar | sige-produccion.chaco.gob.ar |
| sige-produccion-web | sige-produccion.ecomdev.ar | sige-produccion.chaco.gob.ar |
| sige-produccion_web | sige-produccion.ecomdev.ar | sige-produccion.chaco.gob.ar |
| sige-produccion-api | sige-produccion-api.ecomdev.ar | sige-produccion-api.chaco.gob.ar |

Offer only `Solo interno` and `Interno y externo` as access values. Explain that internet access also includes internal access. In TEST, external client/referent validation requires internal and external access. In PROD, propose internal-only initially; request external visibility only on explicit instruction from the client, leader, or responsible superior. Do not promise automatic later publication.

### Manual initialization commands

Ask whether the deployment operator must run commands that are **not automated**. Suggest commands only with repository evidence, then confirm whether they are needed and already automated. Offer manual entry and `No hay comandos manuales`. Record confirmed commands in execution order; never run them. Migration files alone do not establish a manual deployment step.

### Database, cache, and persistent storage

- **Database:** ask no database / existing database / new database required for this environment. For an existing database, request only non-sensitive identifying information when needed; never access credentials. For a new database, ask the engine, suggesting PostgreSQL, SQL Server, MariaDB, MySQL, Oracle, or another engine. State new/existing status in the database field. Do not require a version or add one by default. Explain outside the ticket that the deployment team manages versions, normally the newest version per the stated workflow; do not guarantee compatibility or discard an explicit version requirement.
- **Cache:** ask none / Redis / Memcached, or a manual answer if both are needed. A dependency alone is not proof a service is required. The technical deployment team provisions the requested services.
- **Persistent volume:** ask no / yes and the exact path(s) required now. Explain that this stores user-uploaded files, documents, images, or other dynamic content across deployments. Do not request hypothetical future storage. Never invent or silently rewrite a path; resolve uncertainty about container mount paths before finalizing.

### People, configuration delivery, and observations

- Ask the requester to manually supply each involved developer's **first name, surname, and team**. Do not infer people from commit history.
- Ask whether sensitive configuration will be delivered via PWPush or company Passbolt. The ticket may identify the channel and an explicitly supplied sharing URL, but never include secrets, the PWPush password, or a raw `.env`. Do not create, open, or retrieve a secret-sharing link.
- Tell the requester to privately prepare exactly the variables/configuration needed for this environment using the approved channel. New database/service credentials assigned by the provisioning team must remain blank pending provisioning, not fabricated. Discuss variable names only, and keep those reminders outside the ticket.
- Ask for deployment considerations or `Sin observaciones adicionales`. Include only confirmed relevant details.

## Developer tools and access — outside the deployment ticket

Always include the template's tools section in the generated Markdown, even when secret delivery uses PWPush. Show both environments clearly; do not suggest developers can access production Vault.

| Tool | TEST | PROD |
| --- | --- | --- |
| Argo CD | `argocd.ecomdev.ar` | https://argocd.ecom.com.ar/ |
| Vault | `vault.ecomdev.ar:8200` | Developers have no access. |
| Passbolt | https://pb2.ecom.com.ar/ | https://pb2.ecom.com.ar/ |

Explain that members must be registered in the group named for their corresponding team to access these tools, and can access only their team's applications. Do not infer that membership or visibility has been verified.

If members cannot see their applications, explain the administrative route: the **team leader** must formally request access by ticket to **Jefatura**, which then routes it to **Jefatura de Infraestructura**. That separate request must identify each person's first name, surname, team, and LDAP username. Never request LDAP passwords or add these administrative identifiers as mandatory fields in the deployment ticket. Do not submit the access request or change memberships automatically.

These are maintainer-provided addresses and process instructions, not evidence of service availability, granted access, or authorization for the agent to connect.

## Assemble and hand off

Use [the editable template](../assets/deployment-request.md). Preserve its ticket field labels and keep explanatory comments outside the copyable body. Fill all fields or explicitly mark pending ones; never present an incomplete draft as ready to submit.

Construct the title exactly as:

`Solicitud de despliegue de aplicaciones en cluster Kubernetes <TEST|PROD> - <confirmed application name>`

Always include manual submission instructions, regardless of environment:

1. Enter https://app.ecom.com.ar/tickets/servlet/com.gestiondesolicitudes.gamexamplelogin.
2. Go to **Tickets → Plantillas**.
3. Search for **Pedido de Despliegue en Kubernetes** and select **+**.
4. Replace the preloaded title with the generated title and paste only the ticket body.

Keep the checklist, source paths, unanswered questions, and security reminders in their own section of the Markdown, explicitly marked **No copiar al ticket**. Also retain the developer tools and access section, marked the same way; it is guidance for the developer, not part of the deployment request. Return the file path so the requester can review and edit before manual submission.
