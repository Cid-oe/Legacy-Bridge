# Submission guide (deadline Sep 27, 8:30pm IST / 15:00 UTC; aim to submit by 6:30pm IST)

Do the steps in order. Tick each box. Nothing new gets built today: only packaging.

---

## Step 1 (15 min) · Read the rules and the form

- [ ] Open the IBM Bob 2.0 Hackathon page on lablab.ai (make sure it is **"IBM Bob 2.0 Hackathon"**, not a
      Dev Day or BeMyApp look-alike).
- [ ] Read the rules. Note: any rule about which AI tools may be used to build the project; how many
      submissions per team; required fields.
- [ ] Open the submission form and list every required field (video length/platform, slides, demo URL,
      cover image size, tech tags, track).
- [ ] Confirm your friend is on your lablab team, if they are part of it.
- [ ] If anything below doesn't match the form, the form wins.

## Step 2 (10 min) · Make the repo findable and safe

GitHub → your `garner` repo:

- [ ] **Settings → General → Repository name:** `legacy-bridge` → Rename.
- [ ] **Settings → General → Default branch:** switch to `legacy-bridge`.
- [ ] Locally: `git remote set-url origin https://github.com/Cid-oe/legacy-bridge` then `git pull`.
- [ ] The git history was scanned for your old API key: nothing found. Still, never type the key on screen.
- [ ] **Settings → General → Danger zone → Change visibility → Public** (do this now or right before submitting).
- [ ] Open `https://github.com/Cid-oe/legacy-bridge` in a private/incognito window: you should see the
      Legacy Bridge README with the results table.

## Step 3 (45 min) · Record the video (target 2:30, hard limit 3:00)

**Setup:** new terminal window, font size 18+, dark theme, 1080p screen recording, notifications off.
Load the key from `~/.zshrc`, never type it on camera. Run `clear` before recording.
Nothing needs live Bob: use replay (free, same real answers).

Record these clips separately (easier to edit):

| Clip | What to show | Command / action |
|---|---|---|
| A | The legacy code | Open `targets/base64/src/main/java/org/apache/commons/codec/binary/Base64.java` in your editor, scroll the 2005 header and some byte-twiddling code |
| B | Bob writes the tests | `./bob-bridge --target targets/base64 --mode replay generate-tests` |
| C | The risky ticket gets blocked | `./bob-bridge --target targets/base64 --mode replay modernize --step sec-118 --goal "replay"` |
| D | The report | `./bob-bridge --target targets/base64 report`, open `runs/<newest>/report.html` in the browser, scroll slowly: stat boxes → mutants table → SEC-118 BLOCKED → failing tests → "Bob repairs it: VERIFIED" |
| E | Proof it's real Bob | Open one file in `bob_sessions/` (e.g. `…-base64-sec-118-cli.md`), show "Bob cost" and Bob's answer |
| F | Real code, verified | Open `runs/20260926-122205/report.html` (Metaphone): 117 tests, 5/5 after hardening, VERIFIED; then run `targets/metaphone/differential/check.sh runs/20260926-122205` or show its README line "0 of 136,800" |

### Voiceover script (read it, about 150 words per minute)

**0:00 – 0:20 · Problem** (clip A)
"Every company has code like this: Base64 from Apache Commons Codec, written in 2005. Modernizing it
sounds easy. The risk is that nobody knows everything it does, and AI will happily change behaviour
while making it look cleaner."

**0:20 – 0:50 · Bob writes the safety net** (clip B)
"Legacy Bridge makes IBM Bob prove its work. First, Bob writes characterization tests that pin down what
the code does today: sixty of them. We check they're strong by breaking the code five ways on purpose.
Bob's tests catch all five."

**0:50 – 1:35 · The catch** (clips C + D)
"Then a real ticket: reimplement this on top of java.util.Base64. Bob does it, and its own tests fail.
The new version drops the line break at the end of chunked output, and handles bad input differently.
Systems exchanging this data would break. The change is blocked, not applied. The failures go back to
Bob, and Bob repairs it. In one run even Bob's repair missed an edge case at exactly 57 bytes, and the
gate refused that too. We ran it twice; it was blocked both times."

**1:35 – 2:05 · The pass** (clip F)
"A safety net also has to let good changes through. On Apache's Metaphone, Bob rewrote about 300 lines.
Bob's 117 tests passed, and an independent check on 136,800 inputs found zero differences."

**2:05 – 2:30 · Proof and close** (clip E, then report stat boxes)
"Every step is a real Bob call, recorded with its prompt, answer and cost: 19 calls, two and a half
coins. Legacy Bridge: Bob modernizes, and Bob's own tests decide what ships."

### On-screen captions (large, 2 to 4 seconds each)

1. "Apache Commons Codec · Base64 · 2005"
2. "Bob writes 60 characterization tests"
3. "5/5 behaviour traps caught"
4. "Ticket SEC-118: use java.util.Base64"
5. "BLOCKED · 8 of Bob's tests fail" (hold on `expected: <6> but was: <4>`)
6. "Bob's repair missed a 57-byte edge case · BLOCKED again"
7. "Metaphone: 0 of 136,800 outputs changed"
8. "19 real Bob calls · 2.54 coins · all recorded"

**Edit:** cut all waiting (Maven, Bob thinking). Upload to YouTube as **Unlisted** (or wherever the form
asks) and copy the link.

## Step 4 (30 min) · Slides (6 slides, export as PDF)

1. **Legacy Bridge**: "IBM Bob modernizes legacy Java. Bob's own tests decide what ships." Your name(s).
2. **The problem**: legacy code has behaviour nobody documented; AI refactors change it silently;
   nobody can review 300-line diffs for every edge case.
3. **How it works**: the diagram from the README (Bob writes tests → mutation check → Bob modernizes
   → gate → Bob repairs).
4. **Results**: the README results table (Base64 blocked 2/2, Metaphone verified 0/136,800,
   Invoice blocked then repaired).
5. **Why it's Bob**: Bob writes the tests, fixes its own wrong ones, hardens the suite, modernizes and
   repairs; used its own subagents on real code; every call recorded (19 calls, 2.54 coins).
6. **What's next**: run the gate inside Bob as a skill on any repo; add more languages (COBOL, RPG on
   IBM i); CI integration so every AI change comes with proof.

**Cover image:** dark background, big text "Legacy Bridge", subtitle "Bob's changes ship only with
proof", and the line "BLOCKED · 8 tests failed" in red next to "VERIFIED · 0 of 136,800 changed" in green.

## Step 5 (20 min) · Fill the form

**Project title:** Legacy Bridge

**Short description (one line):**
IBM Bob modernizes legacy Java, and Bob's own characterization tests decide what ships, blocking silent behaviour changes and sending them back to Bob to repair.

**Long description:**
Legacy code is risky to modernize because nobody knows everything it does, and AI refactors can change
behaviour while making code look cleaner. Legacy Bridge makes IBM Bob prove its work. Before touching a
class, Bob writes characterization tests that pin down what the code does today; the harness checks
those tests are strong by breaking the code on purpose (mutation testing), and Bob hardens its own suite
when something slips through. Bob then modernizes the class from a ticket, and the change is applied only
if every one of Bob's tests still passes. If not, it is blocked, the exact differences are reported, and
the failures go back to Bob to repair, and the repair must pass the same gate.

We ran it on real open-source legacy code from Apache Commons Codec 1.3 (2005). Asked to reimplement
Base64 on top of java.util.Base64, Bob's version changed behaviour and was blocked in 2 of 2 runs; in one
run even Bob's repair missed a 57-byte edge case, and the gate refused it too. On Metaphone, Bob's
300-line refactor passed 117 tests and an independent check found 0 of 136,800 outputs changed. Every
Bob call (19 calls, 2.54 coins) is recorded in the repo with its prompt, answer and cost, and the whole
pipeline can be replayed without a Bob account.

**Tech / tags:** IBM Bob, Bob Shell, Java 21, JUnit 5, Maven, Python, mutation testing, characterization testing, legacy modernization

**Links:** GitHub `https://github.com/Cid-oe/legacy-bridge` · video link · slides PDF ·
demo URL: if a demo URL is required, use the repo link or a committed report:
`https://github.com/Cid-oe/legacy-bridge/blob/legacy-bridge/runs/20260926-151915/report.html`
(or enable GitHub Pages on the repo to serve the reports as web pages).

- [ ] Submit by **6:30pm IST**. Then open the submission page logged out and click every link.

## If something breaks

- Replay fails → `git pull`, then check you're on branch `legacy-bridge`; send the error to Claude.
- No time for slides → submit without them only if the form allows; the video and README matter most.
- Stuck at 7:30pm IST → submit what you have. An incomplete entry beats a missed deadline.
