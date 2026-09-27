# SEC-118 repeat study

Re-runs the SEC-118 ticket (reimplement Base64 on top of java.util.Base64) several more times with live Bob,
against the same gate: Bob's 60-test characterization suite from run `20260926-192119`
(tests and baseline copied from that run; Bob was not asked to write new tests). Each attempt starts from the
untouched Apache 1.3 source. Steps are named `sec-118-r3`, `sec-118-r4`, ...
