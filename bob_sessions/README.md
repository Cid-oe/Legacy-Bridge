# IBM Bob sessions

Every IBM Bob call made by Legacy Bridge, recorded by the harness: prompt, Bob's answer, Bob Shell's raw
output and the cost Bob reported. The **Task ID** is Bob Shell's own ID for the task (printed in its Task
Summary), so each row can be matched against `bob --list-tasks`.

Task summary screenshots from Bob Shell: [`screenshots/`](screenshots/). `bob-live-task-summary-base64.png` is a live Bob Shell run inside this repo (Sep 27) reading the real Base64 target, with Bob's Task Summary (cost 0.062, Task ID `a320c14b03b23cc5990719025306542e`). `bob-recorded-task-summary-sec-118.png` shows the Task Summary recorded during the real Base64 SEC-118 run 2 (cost 0.046, 32.3s), matching its row below.

The pipeline calls Bob Shell headless (`bob -p`) from a temporary working folder, so those runs do not appear in `bob --list-tasks`; their Task Summaries are recorded in each session file below.

| Started (UTC) | Step | Bob Task ID | Bob cost | Duration | |
|---|---|---|---|---|---|
| 20260926T060722Z | `probe` | `c0f66873673deadff7a33b261130da91` | not reported | 2.4s | [transcript](20260926T060722Z-probe-cli.md) |
| 20260926T095007Z | `probe` | `8bcf22ae17ba68444ab196675f098422` | 0.028 | 3.0s | [transcript](20260926T095007Z-probe-cli.md) |
| 20260926T095012Z | `generate-tests` | `6e4d92a016b8435380e8f1142ab417d2` | 0.533 | 4m | [transcript](20260926T095012Z-generate-tests-cli.md) |
| 20260926T095428Z | `benign` | `a6374195f708a0768542db5f21e403cb` | 0.038 | 24.7s | [transcript](20260926T095428Z-benign-cli.md) |
| 20260926T095457Z | `fin-212` | `eef64938632ab398c7eef330472de14d` | 0.038 | 28.5s | [transcript](20260926T095457Z-fin-212-cli.md) |
| 20260926T095529Z | `fin-212-repair` | `4be7d7fc779b7137a94f256d50f643a3` | 0.042 | 22.0s | [transcript](20260926T095529Z-fin-212-repair-cli.md) |
| 20260926T113400Z | `metaphone-generate-tests` | `f9d0afd1a4d0183f5619c82752c7352b` | 0.422 | 5m | [transcript](20260926T113400Z-metaphone-generate-tests-cli.md) |
| 20260926T113911Z | `metaphone-fix-tests` | `d733be7ac0396eee27b3293ad32b48b4` | 0.149 | 1m | [transcript](20260926T113911Z-metaphone-fix-tests-cli.md) |
| 20260926T122214Z | `metaphone-harden-tests` | `05bfa1cc676c6e661db0b32d4816fcdc` | 0.145 | 2m | [transcript](20260926T122214Z-metaphone-harden-tests-cli.md) |
| 20260926T122518Z | `metaphone-benign` | `c87f1d070f039336b41e58ff5dc25ee8` | 0.044 | 39.3s | [transcript](20260926T122518Z-metaphone-benign-cli.md) |
| 20260926T122601Z | `metaphone-srch-431` | `11b95668c87873b9f8fa1f213e9c69f4` | 0.044 | 40.9s | [transcript](20260926T122601Z-metaphone-srch-431-cli.md) |
| 20260926T151915Z | `base64-generate-tests` | `c60c6ce9e876d8bb70fd7cfb49ac7a64` | 0.365 | 4m | [transcript](20260926T151915Z-base64-generate-tests-cli.md) |
| 20260926T152353Z | `base64-benign` | `bb3015991ea49be35bc1ba5cf1bef793` | 0.05 | 46.0s | [transcript](20260926T152353Z-base64-benign-cli.md) |
| 20260926T152443Z | `base64-sec-118` | `6412134ae0a58828af2470c7bfd91fa0` | 0.046 | 30.5s | [transcript](20260926T152443Z-base64-sec-118-cli.md) |
| 20260926T152518Z | `base64-sec-118-repair` | `b656214d0834f9358ed6ff22e841391a` | 0.055 | 52.0s | [transcript](20260926T152518Z-base64-sec-118-repair-cli.md) |
| 20260926T192119Z | `base64-generate-tests` | `27e4eeebde064d10e306a18636373f42` | 0.381 | 4m | [transcript](20260926T192119Z-base64-generate-tests-cli.md) |
| 20260926T192617Z | `base64-benign` | `572ccc9c96787b881fdcf8d3b68d4cf1` | 0.051 | 48.9s | [transcript](20260926T192617Z-base64-benign-cli.md) |
| 20260926T192710Z | `base64-sec-118` | `e1111fd1862a0a2e3b91226136faeb8e` | 0.046 | 32.3s | [transcript](20260926T192710Z-base64-sec-118-cli.md) |
| 20260926T192747Z | `base64-sec-118-repair` | `e9854f1552128aefa733e64b5b5daeed` | 0.058 | 53.7s | [transcript](20260926T192747Z-base64-sec-118-repair-cli.md) |

Total: 19 calls, 2.54 Bob coins reported.
