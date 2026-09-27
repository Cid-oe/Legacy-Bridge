# Legacy Bridge

**IBM Bob modernizes legacy Java. Bob's own tests decide what ships.**

Modernizing old code is risky because nobody knows everything it does. Legacy Bridge makes IBM Bob
prove it: before touching a legacy class, Bob writes characterization tests that pin down what the
code does *today*. Bob then modernizes it, and the change ships only if every one of those tests still
passes. When a "cleanup" silently changes behaviour, the gate blocks it, shows the exact difference,
and sends the failures back to Bob to repair.

## Results (real Bob runs, all recorded in `bob_sessions/`)

| Code | Bob's tests | Traps caught | Realistic ticket | Outcome |
|---|---|---|---|---|
| **Base64**, Apache Commons Codec 1.3 (2005, real) | 58 and 60 | 5/5 | SEC-118: *"reimplement on top of `java.util.Base64`"* | **Blocked in 2 of 2 runs.** Bob's swap dropped the trailing CRLF on chunked output and changed error handling. In run 1 even Bob's *repair* missed a 57-byte edge case, and the gate refused it too. Run 2's repair was verified. |
| **Metaphone**, Apache Commons Codec 1.3 (2005, real) | 117 | 4/5, then **5/5** after Bob hardened its own suite | SRCH-431: *"refactor the 170-line switch for readability"* | **Verified.** A ~300-line refactor; an independent differential check found **0 of 136,800** outputs changed, including identical exceptions on the 44 inputs where the original crashes. |
| **InvoiceCalculator** (fabricated teaching example with 4 planted quirks) | 61 | 4/4 | FIN-212: *"simplify the branching logic"* (no quirk named) | **Blocked**, then repaired. Bob changed a discount rule from `> 10` to `>= 10`: 10 widgets would have cost $23.75 instead of $25.00. |

19 real Bob calls in total, **2.54 Bob coins**.

A safety net has to do two things: stop bad changes and let good ones through with proof. The Base64
and invoice runs show the first; the Metaphone run shows the second.

## How it works

```
legacy class ─▶ Bob writes characterization tests ─▶ must pass on the untouched code
                    │                                  (if not, Bob fixes its own wrong tests)
                    ▼
            scored against behaviour mutants ─▶ a mutant survived? Bob hardens its suite
                    │
                    ▼
Bob modernizes (from a ticket) ─▶ re-run Bob's tests ─▶ all pass: VERIFIED, applied
                                                   └─▶ any fail: BLOCKED, not applied,
                                                        failing tests go back to Bob to repair,
                                                        and the repair must pass the same gate
```

**Where Bob does the work:** writing the tests, correcting its own wrong expectations, hardening the
suite when a mutant survives, the modernization itself, and the repair. On real code Bob also used its
own subagents to trace the algorithm by hand before writing tests. Every call is recorded in
`bob_sessions/` with the prompt, Bob's answer, the raw output and its cost.

**Where the harness does the work:** it builds and runs the tests (Maven + JUnit 5), applies
single-quirk mutants to measure how strong Bob's tests are, decides VERIFIED or BLOCKED from the test
results, and writes the report. It never edits code itself.

**Mutation scoring** answers "are Bob's tests any good?": each target has mutants that break one real
behaviour at a time (`mutants.json`), and a good suite must catch every one. Hand-written control tests
catch all of them too and serve as an independent reference.

## Run it yourself (no Bob account needed)

Requires Java 21, Maven and Python 3.11. Replay mode re-runs the real pipeline (Maven, tests, gate) on
Bob's recorded answers:

```bash
./bob-bridge --target targets/base64 --mode replay generate-tests
./bob-bridge --target targets/base64 --mode replay modernize --step benign
./bob-bridge --target targets/base64 --mode replay modernize --step sec-118 --goal "replay"
./bob-bridge --target targets/base64 report        # writes runs/<id>/report.html
```

Use `sample` (the default, steps `benign` and `fin-212`) or `targets/metaphone` (steps `benign` and
`srch-431`) the same way. The committed reports for the real runs are in `runs/`.

The Metaphone differential check: `targets/metaphone/differential/check.sh runs/20260926-122205`.

### With live Bob

```bash
export BOB_API_KEY=...                # from your IBM Bob account; never commit it
export BOB_CMD='bob -p {prompt}'      # Bob Shell, headless; {prompt} is filled in by the harness
./bob-bridge probe                    # one tiny call: checks Bob answers with parseable Java
./bob-bridge --target targets/base64 generate-tests
./bob-bridge --target targets/base64 modernize --step my-ticket --goal "your ticket text"
./bob-bridge --target targets/base64 report
```

Modes: `cli` (live Bob), `replay` (recorded answers), `resume` (recorded answers where they exist,
live Bob for new steps), `fixture` (hand-written stand-ins for developing the harness offline; not Bob,
and the report says so).

## Targets

A target is a Maven project plus a `target.json` (source file, class, control tests, mutants).

| Target | What it is |
|---|---|
| `targets/base64` | `Base64` from Apache Commons Codec 1.3, unmodified (Apache License 2.0). See its README for how it differs from `java.util.Base64`. |
| `targets/metaphone` | `Metaphone` from Apache Commons Codec 1.3, unmodified (Apache License 2.0), plus the differential check. |
| `sample` | Fabricated 2004-era `InvoiceCalculator` with four documented quirks ([QUIRKS.md](sample/QUIRKS.md)). |

## Honest notes

- Bob's tests pin down what the code does today, **including old bugs** (for example, Base64 1.3
  throws on a `null` input and decodes unpadded input to trailing NUL bytes). That is the right default
  for legacy code; the report lists each pinned behaviour so a person can decide to change it on purpose.
- Bob does not always fix its own regression in one try (Base64 run 1). The gate is what makes that safe.
- The invoice target is a teaching example with planted quirks; the two Apache targets are real code.
- The harness in this repo was written with the help of an AI coding assistant; all reasoning inside
  the pipeline (tests, modernizations, repairs) is done by IBM Bob, and is recorded in `bob_sessions/`.

## Layout

```
harness/bridge/     pipeline: Bob client, test runner, mutation scoring, gate, report, CLI
targets/, sample/   the code under modernization, control tests, mutants
bob_sessions/       every real Bob call (prompt, answer, raw output, cost)
runs/<id>/          committed reports and state for the real runs
app.py              Streamlit page (optional)
```
