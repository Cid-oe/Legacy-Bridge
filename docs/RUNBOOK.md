# Runbook until Claude is back (Sep 26, 11pm IST)

Deadline: **Sep 27, 8:30pm IST** (15:00 UTC). Aim to submit by **3pm IST** that day.
Rule from the premortem: no new ideas, no new branches. Anything new goes in `PARKED.md`.

## 1. Connect Bob (timebox: 60 minutes)

```bash
git clone -b legacy-bridge https://github.com/Cid-oe/garner legacy-bridge && cd legacy-bridge
export BOB_CMD='bob -p {prompt}'
./bob-bridge probe                 # Windows: cd harness && python -m bridge.cli probe
```

- "Phase 1 passes" -> go to step 2.
- Fails or hangs -> try `export BOB_CMD='bob run {prompt}'`, then check `bob run --help` for an
  auto-approve or non-interactive flag and add it to BOB_CMD.
- Still stuck after 60 minutes -> fallback: in Bob's chat, paste the contents of the prompts in
  `harness/bridge/prompts.py` by hand, save Bob's replies, and keep the chat exports in `bob_sessions/`.
  Claude will wire them in after the reset.

## 2. First real run

```bash
./bob-bridge demo
```

Open `runs/<id>/report.html` and write down:
- Bob's mutation score before and after hardening (e.g. 2/4 -> 4/4)
- whether the benign modernization was VERIFIED
- whether the risky one was BLOCKED, and whether Bob's repair was VERIFIED
- total run time (the demo target is 120 seconds)

Then commit the evidence:

```bash
git add bob_sessions && git commit -m "chore: real Bob sessions from first demo run" && git push
```

## 3. The "does Bob fall in on its own?" experiment (premortem: most dangerous failure)

Run a vague request three times. If any run is BLOCKED, that is the honest demo moment.

```bash
for i in 1 2 3; do
  ./bob-bridge modernize --step vague-$i --goal "Modernize this class to idiomatic Java 21 and remove deprecated APIs."
done
./bob-bridge report
```

Note which runs were blocked and by which tests. Commit `bob_sessions/` again.

## 4. Logistics (do these now, not at the end)

- GitHub: rename the repo `garner` -> `legacy-bridge` (Settings -> General), and set the default branch
  to `legacy-bridge` (Settings -> Branches). Keep it private until submission day.
- lablab.ai: confirm you are registered for **"IBM Bob 2.0 Hackathon"** (not the Dev Day or BeMyApp
  lookalikes), and check the submission form's required fields and video length.

## 5. Rough video (phone or screen recorder is fine)

1. (15s) "Modernizing legacy code is risky because nobody knows everything it does."
2. (60s) Run `./bob-bridge demo`, cut the waits: Bob writes tests -> hardens them -> modernizes -> verified.
3. (30s) The risky request -> BLOCKED with the exact failing values -> Bob repairs -> VERIFIED.
4. (15s) The report: "Bob's changes ship only when Bob's own tests prove nothing changed."

## When Claude is back (Sep 26, 11pm IST)

Bring: the report numbers from step 2, the experiment result from step 3, and anything that broke.
Claude then: fixes issues, picks the honest regression beat, builds the hosted replay page, and
drafts the README, slides and submission text.
