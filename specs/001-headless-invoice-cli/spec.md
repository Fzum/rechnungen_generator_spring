# Feature Specification: Headless Invoice CLI

**Feature Branch**: `001-headless-invoice-cli`

**Created**: 2026-06-15

**Status**: Draft

**Input**: User description: "Make the invoice generator drivable headlessly by the Claude create-invoice skill while keeping the current interactive mode unchanged. Add a Harvest-or-manual hours source. Open an Apple Mail draft pre-attached with the invoice PDF and the official Harvest monthly report PDF, for manual review only. All local on the Mac. No VPS, hosting, cron, SMTP auto-send, or web UI."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate an invoice without interaction and read the result (Priority: P1)

The automation agent (the Claude "create-invoice" skill) needs to produce a monthly
invoice end-to-end without typing into interactive prompts, and needs to read back the
outcome — invoice number, monetary totals, and file locations — from a single
machine-readable result, instead of scraping console text or driving the program with an
`expect` script.

**Why this priority**: This is the core of the request and the biggest source of current
friction. Today the program only accepts interactive keyboard input, so the agent must
discover prompts in a throwaway run and then replay answers blindly, and it still cannot
reliably learn the output path or amounts. Delivering only this story already removes the
fragile prompt-driving and gives the agent a dependable result — a viable MVP on its own.

**Independent Test**: Invoke the program with all required values supplied up front and
request machine-readable output. Confirm it completes with no prompts, writes the invoice,
and emits a structured result containing the invoice number, the PDF path, and net/VAT/
gross amounts. No human keystrokes and no console parsing are required.

**Acceptance Scenarios**:

1. **Given** a known issuer, customer, period, and an hours value, **When** the program is
   run non-interactively requesting machine-readable output, **Then** it generates the
   invoice without any prompt and prints a single structured result with the invoice
   number, PDF path, customer, period, billed hours, and net/VAT/gross totals, and exits
   with a success status.
2. **Given** the program is run with no arguments, **When** it starts, **Then** it presents
   the existing interactive flow (customer → issuer → period → hours) exactly as it does
   today, with no behavioural change.
3. **Given** a non-interactive run that requests machine-readable output, **When** the
   program also performs desktop side effects, **Then** the structured result remains the
   only content on the primary output channel so it can be parsed unambiguously.
4. **Given** an unknown customer or issuer identifier in a non-interactive run, **When** the
   program runs, **Then** it fails with a non-zero status and a machine-readable error that
   names the problem, without producing a partial invoice.

---

### User Story 2 - Email draft pre-loaded with both attachments for manual review (Priority: P2)

After an invoice is generated, the operator wants an email draft to open already addressed,
already filled with the standard subject and body, and already carrying both required
attachments: the generated invoice PDF and the client-mandated official Harvest monthly
detailed report PDF. The operator always eyeballs the draft and sends it themselves; the
program must never send mail automatically.

**Why this priority**: This happens on every single invoice and is the operator's explicit
pain point. Today the draft opens with no attachments, forcing the operator to locate and
drag in two files by hand each month. It depends on an invoice existing (US1) but delivers
standalone value once attachments are wired in.

**Independent Test**: Run the program for a customer and supply the path to a Harvest report
PDF. Confirm an email draft opens, addressed to the customer with the templated subject and
body, containing exactly two attachments (the generated invoice PDF and the supplied Harvest
report PDF), and that no message is sent until the operator explicitly sends it.

**Acceptance Scenarios**:

1. **Given** a generated invoice and a valid Harvest report PDF path, **When** the draft is
   opened, **Then** it is addressed to the customer, contains the templated subject and body
   with the billing period filled in, and carries exactly two attachments: the invoice PDF
   and the Harvest report PDF.
2. **Given** the draft has been prepared, **When** the program finishes, **Then** the draft
   is left open and unsent, awaiting a human send action.
3. **Given** a non-interactive run that requests the draft be suppressed, **When** the
   program runs, **Then** no draft is opened and generation still succeeds (supporting fully
   unattended runs).
4. **Given** the draft step is requested but the supplied report PDF path does not exist,
   **When** the program runs, **Then** it reports the problem clearly and does not silently
   open a draft missing an attachment.

---

### User Story 3 - Pull billed hours from Harvest automatically (Priority: P3)

Instead of hand-entering the monthly hours, the operator or agent wants the program to
obtain the billable hours for the chosen customer and period directly from Harvest, while
still allowing a manually supplied number when desired.

**Why this priority**: It removes a manual lookup-and-type step and a transcription-error
risk, but the tool is already fully usable with a supplied hours value (US1), so this is the
lowest-priority enhancement. The hours source must be interchangeable so manual entry
remains available.

**Independent Test**: Run the program for a customer and period with the hours source set to
Harvest, supplying no hours value. Confirm the billed hours on the resulting invoice equal
the customer's billable total for that period as shown in Harvest. Re-run with a manual hours
value and confirm that value is used instead.

**Acceptance Scenarios**:

1. **Given** a customer mapped to a Harvest project and the hours source set to Harvest,
   **When** the program runs for a period, **Then** the invoice's billed hours equal the
   customer's billable hours for that period in Harvest.
2. **Given** the hours source is manual (or an explicit hours value is supplied), **When**
   the program runs, **Then** that value is used and Harvest is not consulted.
3. **Given** the hours source is Harvest but the customer has no Harvest project association,
   **When** the program runs, **Then** it fails with a clear, machine-readable error rather
   than guessing or defaulting to zero.
4. **Given** the hours source is Harvest but Harvest cannot be reached or credentials are
   rejected, **When** the program runs, **Then** it fails with a distinct, machine-readable
   error and does not produce an invoice with incorrect hours.

---

### Edge Cases

- **Manual hours selected but no value provided** in a non-interactive run → fail with a
  clear error; do not default to zero or prompt.
- **Period is malformed or in the future** → reject with a validation error before any work
  is done.
- **Requested issuer/customer identifier does not exist** → fail and report which identifiers
  are available, without writing a partial invoice.
- **Report PDF path missing or unreadable** while the draft is requested → report clearly and
  do not produce a draft with a missing attachment.
- **Desktop side effects fail** (draft cannot be opened, file manager cannot reveal the file)
  on an otherwise successful generation → the generated invoice and its result are still
  reported as successful, with the side-effect failure surfaced as a warning rather than
  failing the whole run.
- **Both an explicit hours value and the Harvest source are provided** → the explicit value
  wins (manual override), and this precedence is unambiguous.
- **Invoice number would collide** with one already issued for the same customer/period →
  numbering behaviour is unchanged from today (out of scope to redesign here).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST support a non-interactive mode in which the issuer, customer,
  billing period, and hours (or hours source) are all supplied up front, so a complete
  invoice is produced with zero interactive input.
- **FR-002**: The system MUST preserve the existing interactive mode unchanged when invoked
  with no supplied values, including the current prompt order and the previous-month default
  for the period.
- **FR-003**: The system MUST be able to emit a single machine-readable result describing the
  outcome, including at minimum: invoice number, generated invoice file path, customer,
  period, billed hours, net amount, VAT amount, gross total, and the list of attachment file
  paths.
- **FR-004**: The system MUST keep the machine-readable result free of unrelated console
  output on its primary output channel, so it can be parsed without ambiguity.
- **FR-005**: The system MUST return a success status on a completed generation and a
  distinct non-zero status accompanied by a machine-readable error description on failure.
- **FR-006**: The system MUST offer interchangeable hours sources — at least a manual source
  (value supplied directly) and a Harvest source (billable hours fetched for the customer and
  period) — selectable per run.
- **FR-007**: The system MUST treat an explicitly supplied hours value as an override that
  takes precedence over any configured automatic source.
- **FR-008**: The system MUST associate each customer with the information needed to fetch
  that customer's hours from Harvest, configured through external files alongside the existing
  customer configuration (no customer-specific data hardcoded).
- **FR-009**: The system MUST obtain any Harvest access credentials from the environment and
  MUST NOT hardcode or commit them.
- **FR-010**: The system MUST, after generating an invoice (unless the draft is explicitly
  suppressed), open an email draft addressed to the customer, pre-filled with the existing
  subject and body templates, and pre-attached with exactly two files: the generated invoice
  PDF and the supplied Harvest monthly report PDF.
- **FR-011**: The system MUST NOT send any email automatically; the draft is always left for a
  human to review and send.
- **FR-012**: The system MUST accept the official Harvest monthly detailed report PDF as an
  input file path and attach that exact document; it MUST NOT generate its own substitute
  report.
- **FR-013**: The system MUST allow the desktop side effects (opening the email draft and
  revealing the file in the file manager) to be individually suppressed, so a fully unattended
  run is possible.
- **FR-014**: The system MUST validate supplied inputs (identifiers, period, hours, report
  path) before producing output and fail clearly when an input is invalid or missing, without
  writing a partial or misleading invoice.
- **FR-015**: The system MUST keep monetary and tax math (net, VAT, gross) exact, and the
  invoice numbering, PDF layout, rates, and tax settings MUST remain identical to the current
  behaviour.
- **FR-016**: The system MUST treat a failure of a non-essential desktop side effect on an
  otherwise successful generation as a warning, still reporting the invoice as generated.

### Key Entities *(include if feature involves data)*

- **Invoice**: The produced document and its facts — number, issuer, customer, period, billed
  hours, net/VAT/gross totals, and the path to the generated PDF.
- **Customer configuration**: Per-customer data including contact details and the association
  needed to fetch that customer's hours from Harvest.
- **Issuer configuration**: Per-issuer (invoice sender) data.
- **Billing settings**: Hourly rate, tax rate, payment terms, and bank details applied to the
  invoice.
- **Hours source**: The selected origin of billed hours for a run — manual value or Harvest
  fetch — and the resulting hours figure.
- **Email draft**: The unsent message prepared for the operator — recipient, subject, body,
  and the two attachments (invoice PDF and Harvest report PDF).
- **Generation result**: The machine-readable summary of a run consumed by the automation
  agent.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The automation agent can generate a complete monthly invoice with zero
  interactive keystrokes and obtain the invoice number, monetary totals, and file paths from
  one machine-readable result — with no console-text scraping and no prompt-replay scripting
  in 100% of runs.
- **SC-002**: An operator running the tool with no arguments experiences the same interactive
  flow as before, with zero observable regressions.
- **SC-003**: When the Harvest hours source is used, the billed hours on the invoice equal the
  customer's billable total for the period as reported by Harvest in 100% of runs.
- **SC-004**: Every generated invoice yields an email draft containing exactly two attachments
  (the invoice PDF and the Harvest report PDF), and in no run is an email transmitted without
  an explicit human send action.
- **SC-005**: Every failure condition (invalid input, missing report file, unmapped customer,
  Harvest unreachable or unauthorized) results in a distinct non-zero exit status and a
  machine-readable error, with no partial or silently-skipped invoice, in 100% of failure
  cases.
- **SC-006**: A fully unattended run (draft and file-reveal suppressed) completes and produces
  the invoice and machine-readable result without opening any desktop application.

## Assumptions

- The tool runs on macOS only, consistent with the current project; the email draft opens in
  the system's macOS Mail application and the file-reveal step uses the macOS file manager.
- The official Harvest monthly detailed report PDF is produced outside this tool (downloaded
  from Harvest by the operator or agent) and provided to the tool as a file path; the tool
  never generates its own report, per client requirement.
- "Billed hours" for the Harvest source means the customer's billable hours for the selected
  calendar month.
- Each customer's configuration carries its Harvest project association; Harvest credentials
  are provided via the environment.
- The billing period is identified by calendar month and year; the previous-month default
  applies only to interactive mode.
- Exactly one invoice is produced per invocation.
- Invoice numbering, PDF layout, hourly rate, and tax math are reused unchanged from the
  current implementation.

## Dependencies

- Access to the Harvest account and its API for the Harvest hours source.
- The operator/agent is able to download the official Harvest monthly detailed report PDF for
  the period.

## Out of Scope

- Remote hosting or running on a VPS.
- Scheduled or cron-driven runs.
- Automatic sending of email (SMTP or otherwise).
- A web or graphical user interface.
- Containerized/Docker distribution.
- Any self-generated time-report or substitute for Harvest's official report.
- Redesigning invoice numbering, PDF layout, or the tax/rate model.
