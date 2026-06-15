# Quickstart & Validation: Headless Invoice CLI

A runnable guide proving the feature works end-to-end. Details live in
[cli-contract.md](./contracts/cli-contract.md), [data-model.md](./data-model.md), and
[research.md](./research.md) — not duplicated here.

## Prerequisites

- macOS with Apple Mail configured (only fully implemented platform).
- Java 17 + Maven wrapper (`./mvnw`).
- For the Harvest hours source, environment variables set:
  ```
  export HARVEST_ACCOUNT_ID=...
  export HARVEST_ACCESS_TOKEN=...
  ```
- A customer `.properties` with `harvest-project-id=...` for any customer billed via Harvest.
- A downloaded Harvest official monthly detailed report PDF for the period (for the mail draft).

## Build & test

```
./mvnw test        # all unit tests (incl. new parser/Harvest/AppleScript-builder tests) pass
./mvnw package     # produces target/rechnung_generator_spring-0.0.1-SNAPSHOT.jar
```

## Validate each user story

### US1 — Non-interactive generation + machine-readable result (P1)
```
java -jar target/rechnung_generator_spring-0.0.1-SNAPSHOT.jar generate \
  --issuer=sellemond --customer=nook --period=2026-05 \
  --hours=142.5 --mail=false --open=false --format=json
```
**Expect**: no prompts; exit `0`; stdout is exactly one JSON object matching
`generation-result.schema.json` with `invoiceNumber`, absolute `pdfPath`, and `net/vat/gross`.
Pipe to `jq .` to confirm it parses with no stray log lines.

**Backward-compat check**: run the jar with **no arguments** → the original interactive prompts
(customer → issuer → period → hours) appear and behave exactly as before.

### US2 — Apple Mail draft with both attachments (P2)
```
java -jar target/...jar generate \
  --issuer=sellemond --customer=nook --period=2026-05 --hours=142.5 \
  --report-pdf="/abs/harvest_nook_2026-05.pdf" --format=json
```
**Expect**: Apple Mail opens a **draft** addressed to the customer, templated subject/body with the
period filled in, carrying **exactly two** attachments (generated invoice PDF + the Harvest report
PDF). The message is **not sent**. `attachments` in the JSON lists both absolute paths.
Negative: pass a non-existent `--report-pdf` → exit `2`, error code `MISSING_REPORT_PDF`, no draft.

### US3 — Harvest hours source (P3)
```
java -jar target/...jar generate \
  --issuer=sellemond --customer=nook --period=2026-05 \
  --hours-source=harvest --report-pdf="/abs/harvest_nook_2026-05.pdf" --format=json
```
**Expect**: `hours` in the JSON equals the customer's billable total for 2026-05 in Harvest.
Override check: add `--hours=99` → `99` is used, Harvest not consulted. Failure check: unset
`HARVEST_ACCESS_TOKEN` → exit `3`, error code `HARVEST_FETCH_FAILED`, no invoice written.

## Success-criteria mapping

| Criterion | Validated by |
|-----------|--------------|
| SC-001 zero-keystroke + readable result | US1 (json parses, no scraping) |
| SC-002 interactive unchanged | US1 backward-compat check |
| SC-003 hours match Harvest | US3 |
| SC-004 exactly two attachments, never auto-sent | US2 |
| SC-005 distinct non-zero + machine-readable error | US2/US3 negative checks, exit codes |
| SC-006 fully unattended, no desktop apps | US1 (`--mail=false --open=false`) |

## Post-change gate (constitution)

`./mvnw package` and `./mvnw test` succeed; billing/parsing changes ship with their tests; PDF/
email/file-open paths smoke-tested on macOS.
