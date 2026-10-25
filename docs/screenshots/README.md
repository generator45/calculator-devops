# Screenshots

Evidence captured from build #4 (the first fully green run) on 30 Oct 2026.

The trigger is Poll SCM rather than a webhook: Jenkins runs on localhost, which
GitHub cannot reach, and the brief allows Poll SCM as an alternative. Screenshot 07
is the polling log rather than a webhook delivery for that reason.

| File | What it must show |
|---|---|
| `01-jenkins-pipeline-green.png` | Jenkins Stage View with all 7 stages green (Checkout → Verify Health) |
| `02-jenkins-test-report.png` | JUnit report page: 35 tests, 0 failures |
| `03-email-notification.png` | The `SUCCESS - calculator-devops #<n>` email in the inbox |
| `04-docker-ps.png` | `docker ps` with both `calculator` replicas and `calculator-proxy` as `(healthy)` |
| `05-dockerhub-tags.png` | Docker Hub repository page showing the build-number tags |
| `06-api-responses.png` | `curl` output for add/subtract/multiply/divide plus the 400 divide-by-zero body, the 404 on an unknown path and the 405 (with its `Allow` header) |
| `07-git-polling-log.png` | Git Polling Log showing automated SCM polling against the GitHub remote |

Screenshot 06 is the output of `./scripts/smoke-test.sh`, which can be re-run at any time
against the live stack.
