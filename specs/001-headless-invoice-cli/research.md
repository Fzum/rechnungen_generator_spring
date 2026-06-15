# Phase 0 Research: Headless Invoice CLI

All items below were resolvable from the codebase, the interview decisions, and platform
documentation. **0 NEEDS CLARIFICATION remain.**

## R1. Interactive vs non-interactive dispatch

- **Decision**: Keep the single `RechnungGeneratorCommandLineRunner`. At the top of `run()`,
  inspect the program arguments. If the `generate` subcommand (or any recognised `--flag`) is
  present, route to the non-interactive path (`InvoiceCliArgsParser` → `GenerateInvoiceCommand`).
  If arguments are empty, run today's interactive prompt sequence unchanged.
- **Rationale**: One entry point, zero behaviour change for the bare `java -jar …` invocation
  (FR-002), no second `main`. Spring Boot already passes raw args into `CommandLineRunner.run`.
- **Alternatives considered**: A separate Spring `@Profile`/second runner (rejected: more wiring,
  risk of both running); an env-var switch (rejected: less explicit than args for an agent).

## R2. Argument parsing library

- **Decision**: Parse with Spring Boot's built-in `ApplicationArguments` (option args:
  `--customer=nook`) inside a small, unit-tested `InvoiceCliArgsParser` that produces a typed,
  validated `InvoiceCliArgs`. No new dependency.
- **Rationale**: Constitution V (Simplicity/YAGNI) — there are ~7 flags with simple validation;
  `ApplicationArguments` is already injected by Spring Boot and needs no `pom.xml` change, keeping
  the dependency set frozen.
- **Alternatives considered**: **picocli** (clean help/validation, but a new dependency and a
  second arg-parsing paradigm next to Spring — rejected as unjustified weight); hand-rolled
  `args: Array<String>` scanning (rejected: reinvents what `ApplicationArguments` already gives).

## R3. Clean machine-readable stdout

- **Decision**: In `--format=json`, the **only** thing written to stdout is the single JSON
  result object. Achieve this by: (a) launching with the Spring banner off and logging routed to
  stderr for JSON runs; (b) routing all human/log/progress text to stderr; (c) writing the final
  JSON with a plain `System.out` write at the end. Exit code communicates success/failure (R8).
- **Rationale**: FR-004 — the agent must parse stdout without stripping log lines. stderr stays
  available for diagnostics.
- **Alternatives considered**: A `--quiet` flag only (rejected: fragile, easy to leak a stray
  `println`); writing JSON to a file and printing its path (rejected: extra round-trip for the
  agent, and the path is already inside the JSON).
- **Note**: Audit the existing `println` prompt calls so none fire on the non-interactive path.

## R4. JSON serialization

- **Decision**: Use Jackson (`com.fasterxml.jackson.module.kotlin` is on the Spring Boot
  classpath) to serialize a dedicated `GenerationResult` DTO. `BigDecimal` amounts serialize as
  JSON numbers with their scale preserved (e.g. `14250.00`).
- **Rationale**: Already available; no new dependency; deterministic field order via the DTO.
- **Alternatives considered**: Manual string building (rejected: escaping/▷precision bugs);
  kotlinx.serialization (rejected: new dependency).

## R5. Apple Mail draft with attachments (AppleScript)

- **Decision**: Replace the `open mailto:` call in the macOS mail adapter with an AppleScript run
  via `osascript`. A pure `AppleMailScriptBuilder` produces the script text from recipient,
  subject, body, and attachment paths; the adapter writes it to a temp `.scpt` file and runs
  `osascript <file>`. The script creates a **visible** outgoing message, sets recipient/subject/
  body, adds one `attachment` per file via `POSIX file`, and `activate`s Mail. It **never** calls
  `send`.
- **Script shape**:
  ```applescript
  tell application "Mail"
      set msg to make new outgoing message with properties {subject:"…", content:"…", visible:true}
      tell msg
          make new to recipient at end of to recipients with properties {address:"…"}
          make new attachment with properties {file name:(POSIX file "/abs/invoice.pdf")} at after last paragraph
          make new attachment with properties {file name:(POSIX file "/abs/harvest-report.pdf")} at after last paragraph
      end tell
      activate
  end tell
  ```
- **Rationale**: `mailto:` cannot carry attachments (FR-010); AppleScript is the supported macOS
  path and keeps the draft for manual review (FR-011). Keeping the script builder pure makes path
  escaping and template substitution unit-testable (Constitution III/IV).
- **Gotchas captured for implementation**:
  - Set `content` **before** adding attachments; Mail's attachment indexing depends on existing
    body text. A short `delay 1` after the last attachment avoids the known race where `activate`
    fires before the attachment finishes loading.
  - `POSIX file` handles spaces in paths (the output folder lives under a Google Drive path with
    spaces), so no shell quoting of the path is needed inside AppleScript.
  - Escape `"` and `\` in subject/body when interpolating into the script string; the builder owns
    this and is tested.
  - Writing a temp `.scpt` avoids brittle multi-`-e` shell escaping through `Runtime.exec`.
- **Alternatives considered**: `osascript -e` per line (rejected: escaping fragility); JXA
  JavaScript-for-Automation (rejected: no benefit over AppleScript here); a `.eml`/drag approach
  (rejected: doesn't pre-attach).

## R6. Harvest hours fetch

- **Decision**: `HarvestHoursProvider` calls the Harvest v2 **Project time report**:
  `GET https://api.harvestapp.com/v2/reports/time/projects?from=YYYYMMDD&to=YYYYMMDD`
  with headers `Authorization: Bearer <HARVEST_ACCESS_TOKEN>`,
  `Harvest-Account-Id: <HARVEST_ACCOUNT_ID>`, and a `User-Agent`. It finds the result row whose
  `project_id` equals the customer's configured `harvest-project-id` and reads `billable_hours`.
  `from`/`to` are the first and last calendar day of the invoice period.
- **Rationale**: This is the same per-project, date-ranged billable total the operator reads in
  Harvest's UI and that the current skill fetches via `get_time_report` grouped by project
  (SC-003). One HTTP request, no pagination for a single project row.
- **Auth/config**: `HARVEST_ACCOUNT_ID` and `HARVEST_ACCESS_TOKEN` from environment via
  `HarvestProperties` (Constitution I); `harvest-project-id` per customer in `kunden/*.properties`.
- **Hours type**: `billable_hours` is parsed straight into `BigDecimal` (Constitution II) — never
  through `Double`.
- **Open implementation note (not a blocker)**: confirm the exact JSON field name
  (`billable_hours`) and date format against the current Harvest API docs during implementation;
  the `HarvestTimeReportClient` response model is the single place that binds it.
- **Alternatives considered**: Summing `/v2/time_entries` per entry (rejected: pagination +
  client-side billable filtering for no gain); reusing the report PDF as the hours source
  (rejected: the PDF is for attachment only, hours come from the API per the interview).

## R7. Hours source selection & precedence

- **Decision**: `--hours-source=harvest|manual` selects the provider; an explicit `--hours=<dec>`
  always overrides and forces the manual value even when `--hours-source=harvest` (FR-007).
  `HoursProvider` is the port; `ManualHoursProvider` wraps the supplied value, `HarvestHoursProvider`
  does R6. Selection happens in the non-interactive path; interactive mode keeps using the prompt.
- **Rationale**: Interchangeable behind one port (Constitution IV/V); precedence is unambiguous.
- **Alternatives considered**: Spring `@Profile` per provider (rejected: per-run choice, not
  per-deploy); injecting a `Map<source, provider>` (acceptable, kept minimal with a `when`).

## R8. Error model, exit codes, side-effect failures

- **Decision**: Validate all inputs up front in `InvoiceCliArgsParser` (existence of issuer/
  customer ids, well-formed period, manual hours present when required, report-pdf path exists,
  customer has `harvest-project-id` when source is harvest). On any validation/Harvest/IO failure,
  exit non-zero and emit a machine-readable error object (`{"error":{"code","message"}}`) on
  stdout in JSON mode (FR-005, FR-014). A failure of a **non-essential desktop side effect**
  (mail draft or Finder open) after the PDF is written is logged as a warning and does **not**
  fail the run; the result still reports success with a `warnings` array (FR-016).
- **Exit codes**: `0` success; `2` invalid arguments/validation; `3` Harvest fetch failure;
  `1` unexpected error. (Distinct, stable codes for the agent.)
- **Rationale**: Distinguishes "no invoice produced" (non-zero) from "invoice produced, a desktop
  nicety failed" (zero + warning) — exactly the spec's edge-case split.
- **Alternatives considered**: Always exit 0 and signal via JSON only (rejected: agents and shells
  rely on exit status); failing the run on a mail-open error (rejected: contradicts FR-016).

## R9. Side-effect gating for unattended runs

- **Decision**: `--mail=true|false` and `--open=true|false` (both default `true`) gate the email
  draft and the Finder reveal independently. With both `false`, the run touches no desktop app and
  still produces the PDF + JSON (FR-013, SC-006). The use case calls the ports only when enabled.
- **Rationale**: Supports fully unattended generation while preserving today's defaults for humans.
- **Alternatives considered**: A single `--headless` flag (rejected: less granular than the two
  independent concerns the spec calls out).
