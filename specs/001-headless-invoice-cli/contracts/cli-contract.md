# CLI Contract: Headless Invoice CLI

This is the command-line surface the `create-invoice` skill (and a human) depends on. It is the
external contract for the feature; the JSON result schema is in
[`generation-result.schema.json`](./generation-result.schema.json).

## Invocation

```
java -jar rechnung_generator_spring-0.0.1-SNAPSHOT.jar [generate] [options]
```

- **No arguments** → existing interactive mode (prompts on stdin), unchanged. The rest of this
  contract applies only when arguments are present.
- `generate` is the (only) subcommand; it may be implied when options are present.

## Options

| Option | Required | Default | Meaning |
|--------|----------|---------|---------|
| `--issuer=<short-name>` | yes | — | Issuer (Unternehmen) short-name; must exist |
| `--customer=<short-name>` | yes | — | Customer (Kunde) short-name; must exist |
| `--period=<YYYY-MM>` | yes | — | Billing month; must be well-formed and not in the future |
| `--hours-source=<manual\|harvest>` | no | `manual` | Where billed hours come from |
| `--hours=<decimal>` | cond. | — | Billed hours; **required** if source is `manual`; **overrides** when source is `harvest` |
| `--report-pdf=<path>` | cond. | — | Path to Harvest's official monthly PDF; **required** when `--mail=true`; attached verbatim |
| `--mail=<true\|false>` | no | `true` | Open the Apple Mail draft for manual review |
| `--open=<true\|false>` | no | `true` | Reveal the generated PDF + folder in Finder |
| `--format=<text\|json>` | no | `text` | `json` prints a single machine-readable result to stdout |

## Output channels

- **`--format=json`**: stdout carries **exactly one** JSON object and nothing else — either a
  `GenerationResult` (success) or a `{"error":{…}}` object (failure). All logs/diagnostics go to
  stderr.
- **`--format=text`**: human-readable summary on stdout (today's style).

## Exit codes

| Code | Meaning |
|------|---------|
| `0` | Invoice generated (a non-fatal side-effect failure is reported in `warnings`, still `0`) |
| `2` | Invalid/missing arguments or failed input validation (unknown id, bad period, missing required hours, missing/unreadable report PDF, unmapped Harvest project) |
| `3` | Harvest fetch failed (unreachable, unauthorized, no matching project row) |
| `1` | Unexpected error |

## Behavioural guarantees

- The email draft is **opened, never sent** (`--mail=true`); with `--mail=false` no mail app is touched.
- With `--mail=false --open=false` no desktop application is launched; PDF + JSON are still produced.
- An explicit `--hours` always wins over a Harvest fetch.
- The two attachments, when mail is enabled, are exactly the generated invoice PDF and the
  supplied `--report-pdf`, in that order.
- Money values (`hours`, `net`, `vat`, `gross`) are exact decimals with preserved scale.

## Examples

Headless, hours from Harvest, draft with attachments (the skill's normal path):
```
java -jar app.jar generate \
  --issuer=sellemond --customer=nook --period=2026-05 \
  --hours-source=harvest \
  --report-pdf="/abs/harvest_nook_2026-05.pdf" \
  --format=json
```

Manual hours, fully unattended (no desktop side effects):
```
java -jar app.jar generate \
  --issuer=sellemond --customer=nook --period=2026-05 \
  --hours=142.5 --mail=false --open=false --format=json
```

Success stdout (json):
```json
{"invoiceNumber":"sellemond_nook_5_2026","pdfPath":"/abs/rechnungen/nook/rechnung_nook_5_2026.pdf","customer":"nook","issuer":"sellemond","period":"2026-05","hours":142.50,"net":14250.00,"vat":2850.00,"gross":17100.00,"attachments":["/abs/rechnungen/nook/rechnung_nook_5_2026.pdf","/abs/harvest_nook_2026-05.pdf"],"warnings":[]}
```

Failure stdout (json), exit `3`:
```json
{"error":{"code":"HARVEST_FETCH_FAILED","message":"No project row for harvest-project-id 12345678 in 2026-05"}}
```
