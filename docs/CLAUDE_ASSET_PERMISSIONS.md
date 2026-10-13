# Claude permissions for the owner asset library

The owner authorized this rule on 9 October 2026 so contributors can implement the supplied assets in Jugcraft. The standing content-use authorization is at the top of [CLAUDE.md](../CLAUDE.md); the [magic guide](../art/owner-library/MAGIC_ASSETS.md) explains how to preserve originals and integrate runtime copies. Permission to use a file is separate from permission to run a tool or executable.

The [owner's provenance clarification](../art/owner-library/PROVENANCE.md) addresses remaining original-mod names, credits and supporter text. Those labels alone should not halt implementation or restart the same permission discussion. The rule preserves source provenance and addresses concrete issues at the affected file; it does not instruct Claude to bypass a runtime denial.

## Shared repository configuration

[.claude/settings.json](../.claude/settings.json) permits `Read(/art/owner-library/**)` when Claude Code starts at the repository root and the workspace is trusted. Its scope is reading this library. The authoritative project authorization in `CLAUDE.md` also explains copying, adaptation and implementation.

Anthropic documents that the Auto-mode classifier reads loaded `CLAUDE.md` instructions, but ignores `autoMode` in both project settings files. Consequently, committing the proposed `autoMode` block would not activate it. See [configuration scopes](https://code.claude.com/docs/en/auto-mode-config#where-the-classifier-reads-configuration) and [permission path syntax](https://code.claude.com/docs/en/permissions#read-and-edit-rules).

## Personal configuration for local Claude Code

The [settings fragment](examples/claude-owner-library-auto-mode.json) contains the owner-library context and a bounded content-integration exception. The user can merge those entries into `~/.claude/settings.json` on the machine running Claude, preserving existing settings. Keep `$defaults` and existing rules. Verify with `claude auto-mode config`; see [Anthropic's configuration reference](https://code.claude.com/docs/en/auto-mode-config#inspect-the-defaults-and-your-effective-config).

This fragment is an example for human installation, not an active project setting. It grants no general workflow-launch permission. The shared configuration does not contain `permissions.allow: ["Workflow"]`: that shortcut was not verified in the official permission reference and would not express the requested asset-specific scope.

## Cloud sessions and an existing blocked session

Local user settings do not transfer to Claude's cloud sessions. Supported project settings and `CLAUDE.md` travel with the repository; organization server-managed settings are separate. See [settings in cloud sessions](https://code.claude.com/docs/en/settings#settings-in-cloud-sessions).

1. Bring the working branch up to date without overwriting unfinished work and ensure Claude loads the updated root `CLAUDE.md`.
2. Retry the authorized asset task. If it is denied again, capture the exact tool call and denial category.
3. Use the session's user-facing permission controls to review that action, or choose Accept edits if available and approve the requested operations. Organization restrictions still apply. Do not ask an agent to remove its own safety checks or install a permission change through an alternate execution route.

The repository update supplies authorization and a documented read rule. It cannot prove that a particular cloud session accepted the change; confirm by successfully retrying the actual asset operation.

## Verification

The JSON files and local documentation links are checked before publication. No gameplay resources, Java code or dependency pins change. This configuration has not been exercised in the owner's inaccessible Claude cloud session. Prepared with OpenAI Codex; permission behavior was checked against Anthropic's documentation on 9 October 2026.
