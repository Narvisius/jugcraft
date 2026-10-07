# Start contributing to Jugcraft

You may start a focused prototype and submit a draft pull request now. The Fabric scaffold and MIT license exist. Prior issue approval and upstream write access are not prerequisites for proposing code. Maintainers still review fit, quality and tests before merging.

If you currently have only a plan, implement the smallest useful part on your branch, then commit those files and open a draft PR. A PR needs an actual file change; do not create empty or meaningless commits. A useful design-document change can also be proposed as a documentation PR. Do not stop at an issue when the contributor asked you to implement a feature.

## 1. Make your own fork

Sign in to your own GitHub account, open https://github.com/jimbozoomer-byte/jugcraft and click Fork. Your copy should appear as YOUR-USERNAME/jugcraft. Sync it with upstream main before starting. Read CONTRIBUTING.md, CLAUDE.md, docs/WHAT_EXISTS.md and docs/PLATFORM.md.

## 2. Connect your coding tool to YOUR fork

Use a coding environment that can edit files, run commands and push Git branches, such as Claude Code. Select Opus 5.5 for AI-assisted feature implementation under the existing project policy. A chat that only reads GitHub cannot publish changes just because it can see the repository.

Authorize the coding integration for your own fork under your GitHub account. The Jugcraft owner's Claude app connection applies to their account, not yours. Use normal sign-in or integration setup; never paste access tokens into a prompt. If your tool cannot publish, you can still develop locally and push through authenticated Git or GitHub Desktop.

## 3. Build on a feature branch

Clone your fork using the URL from its Code menu. In that checkout, use:

```sh
git remote add upstream https://github.com/jimbozoomer-byte/jugcraft.git
git fetch upstream
git switch -c feature/my-addition upstream/main
```

Add upstream only if it does not already exist. Use a distinct branch name. origin must point to your fork. Implement one feature, follow pinned versions, run relevant checks, and record what you could not test. See docs/PLATFORM.md for the current toolchain.

```sh
git add path/to/your/changed-file
git commit -m "feat: describe your addition"
git push -u origin feature/my-addition
```

Replace the example file path with your actual files. Never push directly to upstream main.

## 4. Open the PR against Jugcraft

On your fork choose Contribute / Open pull request, or use GitHub's compare page and compare across forks. Confirm the base repository is jimbozoomer-byte/jugcraft, base branch main, head repository YOUR-USERNAME/jugcraft, and compare branch your feature branch. Fill the PR template; choose Draft if unfinished or awaiting playtests. You can describe the proposal directly in the PR without an issue number.

Commits pushed to a fork do not automatically create a PR. Copy the resulting upstream PR URL to prove submission; a local commit or AI claim is not evidence it was published.

## If you get stuck

| Symptom | Meaning and next step |
| --- | --- |
| AI says it must await an approved issue | Read current main: focused fork prototypes and draft PRs are allowed |
| Permission denied pushing | Check origin points to your fork and authenticate as its owner |
| Repository unavailable in Claude | Grant your coding integration access to your fork, then reconnect/select it |
| Tool can read but has no write/terminal capabilities | Use a coding environment with Git write access, or push local files with authenticated Git |
| Nothing to compare | Commit and push your changed branch; choose the correct fork and branch |
| You see a PR only in your fork | Reopen it with jimbozoomer-byte/jugcraft as the base repository |
| Workflow says approval required | The PR exists. A maintainer must review the outside contribution before approving its CI run; this does not prevent submitting PRs |
| Build failed or branch is behind | Update the branch, inspect the actual error, fix it and rerun checks |

Ask for help with the exact error text, your fork URL, branch name, and upstream PR URL if one exists. Never share credentials.

## Prompt for your coding agent

> Work in my fork of jimbozoomer-byte/jugcraft. Read the current main instructions and existing feature map. Implement one focused prototype of my idea, using the pinned Minecraft/Fabric versions and shared systems. You may begin without an approved issue. Run relevant checks, commit and push to my fork's feature branch, then open a draft PR against jimbozoomer-byte/jugcraft:main. I authorize those actions for this feature. Do not merge or deploy. If GitHub access fails, report the exact error and needed connection step while continuing useful local work. My idea is: [describe it].
