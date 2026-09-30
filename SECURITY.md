# Security reports

Do not publish exploitable duplication instructions, server compromise details, tokens, or personal information in public issues. Use GitHub's private vulnerability reporting if enabled (Security → Report a vulnerability). If unavailable, contact the repository owner privately through their published channel and request a reporting route before sharing details.

No playable version exists yet. Supported versions and response contacts will be listed before alpha. Reports should include affected version/commit, impact, reproducible steps, and a minimal redacted example. Do not test attacks against the live viewer server.

PR builds are untrusted code: use isolated machines/runners and test worlds without production credentials. Never expose secrets to fork PRs or run contributor code via privileged `pull_request_target` workflows.
