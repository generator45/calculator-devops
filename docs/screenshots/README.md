# Screenshots

Evidence required by the mini-project deliverables. Capture each of these from the
working setup and commit them here before the Class 17 review.

| File | What it must show |
|---|---|
| `01-jenkins-pipeline-green.png` | Jenkins Stage View with all 7 stages green (Checkout → Verify Health) |
| `02-jenkins-test-report.png` | JUnit report page: 24 tests, 0 failures |
| `03-email-notification.png` | The `SUCCESS - calculator-devops #<n>` email in the inbox |
| `04-docker-ps.png` | `docker ps` with both `calculator` replicas and `calculator-proxy` as `(healthy)` |
| `05-dockerhub-tags.png` | Docker Hub repository page showing the build-number tags |
| `06-api-responses.png` | `curl` output for add/subtract/multiply/divide plus the 400 divide-by-zero body |
| `07-github-webhook.png` | GitHub webhook delivery with a green tick |

Reproduce the API output with `./scripts/smoke-test.sh`.
