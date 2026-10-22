# Windows updater shutdown handoff

Updater-only hotfix for the reported `20261021.3` → `20261022.1` failure.
AI assistance: OpenAI Codex (GPT-6). Gameplay, registries, dependencies and worlds
are unchanged. The separate unreleased biome/aircraft/furniture batch is excluded.

## Failure and fix

The player's supplied diagnostic archive records a verified download followed by
`Another game is using this instance; no files changed` in `UpdateApplier.main`.
The released helper waited for the parent PID to exit, then tried the game lock once.
[Microsoft documents that termination can precede lock cleanup](https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-lockfileex).
The player's separate reproduction supports this diagnosis; the old helper log
cannot establish historical lock ownership with certainty.

- After parent exit, retry a null lock result every 100 ms for at most 30 seconds,
  using a monotonic elapsed-time deadline. IO failures and interruption escape.
- Retain the helper lock throughout parent wait, game-lock wait and transaction.
  Do not verify/replace/recover managed files until the exclusive game lock is held.
- Log helper/parent PIDs, parent wait, lock wait and acquisition/timeout outcomes.
  A timeout reports the failed wait rather than asserting a second game exists.
- Both startup and helper acquire helper.lock before game.lock. A rejected relaunch
  or recovery launch does not retain game.lock while the helper is waiting for it.
- Verification, backups, journal, atomic replacement and rollback stay unchanged.
  Lock files are never deleted to force access.

## Recovery for clients with the old helper

Publishing a fixed JAR does not guarantee a broken old helper can install it.
`scripts/package_updater_recovery.py` therefore makes an offline plain ZIP for
explicit supported prior inventories. It contains the union of changed managed
JARs and the exact new `JUGCRAFT-UPDATE.json`. It contains no executable installer,
player settings, saves or launcher configuration. Hashes and platform pins are
verified before the ZIP is written. Existing extra mods are not removed.

For `20261021.3` and `20261022.1`, the intended recovery is six managed files:
Jugcraft plus Resourcify, Lithium, FerriteCore, ImmediatelyFast and Krypton. The
five libraries are unchanged from the already released `20261022.1` pack. No old
managed filenames need removal. The archive's own manifest is authoritative.

Player steps:

1. Close Minecraft and wait one minute for any helper to finish.
2. Use Modrinth's open-folder action on the existing JUGCRAFT instance.
3. If `.jugcraft-game-update/journal.properties` exists, keep everything intact and
   request recovery support. This manual repair targets the reported pre-write
   failure, not an interrupted transaction.
4. Copy the contents of `COPY-INTO-JUGCRAFT` into that instance, merging `mods/`
   and replacing the supplied files. Never delete the existing mods folder.
5. With no journal present, remove stale `plan.properties`, `ready.txt` and
   `result.txt` from `.jugcraft-game-update` if present. Keep lock files and backups.
6. Launch the same profile. Future updates use the fixed helper; no new profile,
   settings reset or repeated manual repair is required.

## Validation record

The updater transaction suite adds real standalone-helper subprocess checks for
delayed lock release, a persistent conflicting process and the full production
30-second timeout. It checks old managed files remain unchanged while waiting,
helper-lock ownership, startup exclusion, exact inventory commit and preservation
of keybinds/settings/saves/extra mods. Existing corruption, rollback and recovery
tests remain. The delayed-lock case deliberately retains a lock after a fake
parent exits; it is deterministic coverage, not a claim to reproduce every Windows
kernel timing sequence.

The recovery packaging suite verifies corrupt-payload rejection, platform mismatch
rejection, same-instance replacement, inventory hashes and personal-file preservation.
On October 22, `gameUpdaterTest` passed **79 assertions on Windows with Java 25**,
including standalone packaged-helper subprocesses and the real 30-second timeout.
The recovery packaging tests passed all three cases described above.
Repository and dependency-pin checks passed. The full Python suite ran 194 tests:
193 passed initially; the Candy Cavity repeatability check differed only because
its Windows generator rewrote LF as CRLF. Its four focused tests passed on rerun,
and generated resources were restored without including any biome changes.

The full Minecraft/Modrinth apply/quit/relaunch cycle has **not** been performed
for this hotfix. Desktop control was stopped by the owner; their launcher and
existing profiles were left alone. The reported failure was on another player's
computer. Automated subprocess coverage is not presented as that missing manual
acceptance test. Release and deployment evidence must be recorded separately.
