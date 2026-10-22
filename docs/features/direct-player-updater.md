# Direct player distribution and in-game updates

Owner requested 21 October 2026. Implemented using OpenAI Codex (GPT-6).

## Player flow

Share the versioned **.mrpack** directly through Discord/GitHub. Import it once
into Modrinth App as JUGCRAFT. No public Modrinth listing, Windows EXE, launcher
hook, folder picker or separate account is needed. Players sign into Minecraft
through their launcher as usual.

The installed mod checks the owner-promoted GitHub channel asynchronously when
Minecraft reaches its title screen. A newer release opens the update menu once
per session. The **JUGCRAFT Updates** button reopens it. Choose **Download update**,
then **Apply update & quit**. A bundled Java helper waits for that Minecraft
process to exit, installs the verified files, and the player presses Play again.
Do not click Play immediately while the helper is working; a startup guard stops
a rushed launch and attempts recovery if installation was interrupted.

The actual Fabric game directory determines the instance. Custom Modrinth storage
locations work without guessing a path or storing a separate binding. Updates
change this instance in place; they do not import another copy of the pack.
The launcher may retain its original import-version label; the update menu shows
the installed version. We deliberately do not edit Modrinth's private database.

The earlier Windows EXE/hook implementation is retained as legacy source only.
If an early tester configured that hook, remove it from this instance's Pre-launch
setting before using the new pack. Existing profiles without the new mod and
`JUGCRAFT-UPDATE.json` inventory do not acquire the updater retroactively.

## Preserved state and limits

Only managed `mods/*.jar` files are added/replaced/removed. Configs, options,
keybinds, saves, screenshots, resource packs, servers and unrelated mods remain
owned by the player. Modified/missing managed files produce a conflict; restore
them before retrying. New files cannot overwrite an unmanaged mod. A platform
change (Minecraft, Fabric or Java) requires a reviewed manual import/migration.
An older public channel never silently downgrades a newer test build.

The pack's client-only `JUGCRAFT-UPDATE.json` opts into checking; standalone mod
installs, development runs and dedicated servers never check/download updates.
The owner explicitly authorized this bounded exception to the repository's older
no-startup-downloader rule. Checks occur before joining a world, after Minecraft
has started. Downloading/installing requires player consent. Offline/check errors
are shown in the update screen; the player can return to the installed version.
This is an update prompt, not a server compatibility enforcement system.

HTTPS, fixed approved sources, bounded metadata/downloads and SHA-512 verification
protect integrity. The GitHub repository/release publisher is the trust root;
hashes are not a substitute for protecting that account. The updater never runs
scripts supplied by a release. Its Java helper is compiled from this repository
and bundled in the installed mod, extracted to a private instance subfolder.
The helper holds an exclusive update lock, waits for the originating game to
exit, checks the game lock, verifies the old inventory and staged files, backs
up changed files and journals the transaction. Interrupted commits restore the
previous inventory before another launch. Only the previous transaction's changed
mod backups are retained after success. No full profile copies accumulate.

Java 25 is already supplied by the launcher. The design uses portable Java APIs;
Windows is the local tested platform. macOS/Linux launcher restart and filesystem
behavior still need end-to-end validation. No gameplay is launched or controlled
by the updater; it only quits after the player's Apply confirmation.

## Owner publishing

1. Build/test the release. `scripts/package_release.py` makes the `.mrpack`,
   `player-update.json`, `jugcraft-client.jar` and candidate `stable.json` from the
   exact source commit. Publish immutable versioned payloads on GitHub.
2. Back up and directly update the matching server mod files; verify startup.
   Preserve the world. Do not promote an untested or mismatched server version.
3. Run `scripts/promote_player_update.py --candidate PATH/stable.json
   --server-version VERSION --publish`. Omit `--publish` for a dry run. The tool
   verifies the public payload metadata, not server health; the operator must
   verify deployment first. Only this step changes the player-channel manifest.
4. Players see the promotion at their next title-screen check. A merge, draft PR
   or unrelated GitHub release does not trigger an update.

## Validation

`./gradlew gameUpdaterTest` exercises real disposable-file transactions, corrupt
payload rejection, managed-file conflicts, rollback after partial replacement,
process-death recovery, path/source restrictions, version ordering, preservation
of personal files, and a real helper subprocess waiting for a fake game process
to exit before applying. `UpdateMenuClientGameTests` renders fixture states and
checks controls without downloading, installing or touching a player instance.
Python packaging tests validate the initial managed inventory and immutable
release payloads. The helper carries only Jugcraft's own Java code; no third-party
nested mod JAR is permitted. A real Modrinth import/update/relaunch cycle remains
a separate player acceptance check; automated fixtures are not a played test.

Local validation, 21 October 2026: compilation and the complete 2,233-test server
suite passed. The final packaged helper passed 52 offline assertions (including
paths with spaces, post-commit status-write failure and a real standalone-JAR
subprocess). The disposable client passed UpdateMenuClientGameTests; available,
downloading, ready and error screens were captured, and available/ready screenshots
were visually inspected. All 182 Python tests passed, with release-packaging tests
rerun after removing script handouts. Library hashes, mixin JAR and repository
checks passed. Comparing the new JAR with the deployed .2 artifact found no changes
to gameplay assets/data or gameplay classes outside the client updater registration.
The .3 updater preview is not a server deployment or public-channel promotion.
