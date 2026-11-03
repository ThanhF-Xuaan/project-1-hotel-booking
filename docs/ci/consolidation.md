# CI consolidation delivery

Base: eedeaf9fda584c8d10629d09961d86e2821fe7b3.
Authorized scope: consolidate CI and replace whole-App historical comparisons with live route contracts.
Owner review: Member 2 for workflow/protection, Member 4 for route contracts.

## PR 1
Keep member1-ci.yml for transition. ci.yml runs five jobs: backend, frontend/browser,
contracts/tooling, synthetic PostgreSQL, Docker. Required backend/frontend check names stay stable.
Backend minimum stays 1478; PostgreSQL minimum stays 6. Existing exclusions and required suites stay intact.
Frontend lint/build/audit and the three existing demo browser suites run on Node 22.
Tooling uses Node 24 for historical compatibility.

Current artifact validation retains schema/semantic/negative checks and compares the 41 frozen
artifact files to immutable baseline Git blobs, normalizing only CRLF. It explicitly reports
liveSourceProvenance=NOT_CHECKED. Default historical CLI behavior is unchanged.
Current App route safety is checked independently through TypeScript AST: six route/component
contracts, three CHAIN_ADMIN demos, CUSTOMER personalization, public privacy and room details.
Unrelated additions/formatting are allowed. Missing/duplicate routes, wrong destinations/imports
and lost/changed guards are rejected. Application source is not edited.

Existing historical source audits and all their original assertions run in a separate detached
worktree at the base SHA. Their result verifies historical evidence, not current runtime behavior.
Current backend/browser tests remain the runtime regression checks. No gold, manifest or threshold edits.

## PR 2 and rollout
After PR 1 merges and all five jobs pass on develop, delete only member1-ci.yml in PR 2.
Member 2 must review required checks and add contracts/tooling, analytics-db and Docker after
their successful first runs. Do not bypass required human review or change protections using an AI token.
PR 2 must not merge before that transition is verified. Existing tools remain because historical
replay still uses them. Deployment workflow remains separately managed.

Rollback each PR independently through a revert PR; never rewrite develop history.

## Verification
PASS: 8 Node route regression tests; live six-route check.
PASS: 10 Python CI tests (4 new artifact mode checks and 6 existing report guards).
PASS: artifacts-only validator: 41 frozen files, 184 instances, 74 cases, 33 negative rejections.
PASS: 34 acceptance tooling tests outside Windows sandbox.
Initial sandbox tooling run failed metadata safety checks; no assertions or source were relaxed.
PASS: workflow YAML parses to five jobs with the two required names preserved.
PASS: 58 existing T12 tooling tests; frontend lint/build on local Node 24.
Node 22 and exact GitHub execution results are recorded in the PR.
Backend/PostgreSQL/browser/Docker execution is delegated to the exact PR GitHub checks;
local production services and databases are not used.
