# Quickstart & Validation: Headless Invoice CLI

A runnable guide proving the feature works end-to-end against the real flat architecture. Details in
[cli-contract.md](./contracts/cli-contract.md), [data-model.md](./data-model.md), and
[research.md](./research.md).

## Prerequisites

- macOS with Apple Mail configured (only fully implemented platform).
- Java 17 + Maven wrapper (`./mvnw`).
- For the Harvest hours source, env vars:
  ```
  export HARVEST_ACCOUNT_ID=...
  export HARVEST_ACCESS_TOKEN=...
  ```
- A customer `.properties` with `harvest-project-id=...` for any customer billed via Harvest.
- A downloaded Harvest official monthly detailed report PDF for the period (for the mail draft).

## Build & test

```
./mvnw test        # all unit tests (incl. new parser/USt/Harvest/AppleScript-builder tests) pass
./mvnw package     # produces target/rechnung_generator_spring-0.0.1-SNAPSHOT.jar
```

> The jar resolves `kunden/`, `unternehmen/`, and `rechnungen/` relative to the working directory
> (as `rechnung.sh` does by `cd`-ing into the Rechnungen folder). Run the examples from a directory
> that has those, or rely on the bundled `test-kunde`/`test-unternehmen` fallbacks.

## Validate each user story

### US1 — Non-interactive generation + machine-readable result (P1)
```
java -jar target/rechnung_generator_spring-0.0.1-SNAPSHOT.jar generate \
  --issuer=test-unternehmen --customer=test-kunde --period=2026-05 \
  --hours=142.5 --mail=false --open=false --format=json
```
**Expect**: no prompts; exit `0`; stdout is exactly one JSON object matching
`generation-result.schema.json` with `invoiceNumber` (`"Rechnung test-kunde-2026/05"`), an absolute
`pdfPath`, and `net/vat/gross`. `… --format=json | jq .` parses with no stray log lines (logs went
to stderr, banner is off).

**Backward-compat check**: run the jar with **no arguments** → the original interactive prompts
(Kunde → Unternehmen → period → hours) appear and behave exactly as before.

### US2 — Apple Mail draft with both attachments (P2)
```
java -jar target/...jar generate \
  --issuer=test-unternehmen --customer=test-kunde --period=2026-05 --hours=142.5 \
  --report-pdf="/abs/harvest_test-kunde_2026-05.pdf" --format=json
```
**Expect**: Apple Mail opens a **draft** to the customer email, templated subject/body with the
period filled in, carrying **exactly two** attachments (generated invoice PDF + the supplied Harvest
report PDF), **not sent**. `attachments` lists both absolute paths.
Negative: a non-existent `--report-pdf` → exit `2`, `MISSING_REPORT_PDF`, no draft.

### US3 — Harvest hours source (P3)
```
java -jar target/...jar generate \
  --issuer=test-unternehmen --customer=test-kunde --period=2026-05 \
  --hours-source=harvest --report-pdf="/abs/harvest_test-kunde_2026-05.pdf" --format=json
```
**Expect**: `hours` equals the customer's billable total for 2026-05 in Harvest. Override: add
`--hours=99` → `99` used, Harvest not consulted. Failure: unset `HARVEST_ACCESS_TOKEN` → exit `3`,
`HARVEST_FETCH_FAILED`, no invoice written.

## Success-criteria mapping

| Criterion | Validated by |
|-----------|--------------|
| SC-001 zero-keystroke + readable result | US1 |
| SC-002 interactive unchanged | US1 backward-compat check |
| SC-003 hours match Harvest | US3 |
| SC-004 exactly two attachments, never auto-sent | US2 |
| SC-005 distinct non-zero + machine-readable error | US2/US3 negatives, exit codes |
| SC-006 fully unattended, no desktop apps | US1 (`--mail=false --open=false`) |

## Post-change gate (constitution)

`./mvnw package` and `./mvnw test` succeed; billing/parsing changes ship with their tests; PDF/
Apple-Mail/Finder paths smoke-tested on macOS.
