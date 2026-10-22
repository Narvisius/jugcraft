# JUGCRAFT installation and updates

The player-facing pack name is **JUGCRAFT**. Minecraft 26.3, Fabric Loader 0.19.5
and Java 25 remain pinned. Player updates are offered in the Minecraft title menu.

## Current distribution: one pack import, in-game updates

Share the versioned `.mrpack` directly; see the [in-game updater guide](features/direct-player-updater.md).
Players import once in Modrinth App. The title-menu updater finds that same
instance automatically, offers verified downloads, then applies after the player
chooses to quit. Settings and saves stay in place. No EXE, manual folder binding,
launcher hook or public pack listing is needed. The older Windows helper is legacy.

The Modrinth Server Project below remains an unpublished draft; do not submit it
for review or distribute its old `JUGCRAFT-Join.zip`. `enabled: false` in
`distribution/server-project.json` stops future release assembly from generating
that shortcut. Server hosting and direct mod-file updates continue independently.

After testing/deploying the server, explicitly promote the candidate channel:

```sh
python scripts/promote_player_update.py --candidate build/release/stable.json --server-version VERSION --publish
```

Omit `--publish` for a dry run. The server version is the operator's verified
deployment declaration. The tool validates public release metadata; it does not
probe server health. Publishing a release or merging main never promotes the
channel automatically. Clients verify the channel's manifest and each payload.

## Coordinated release batches

Owner direction, 21 October 2026: accumulate larger updates locally and hold
deployment/channel promotion until the owner says the batch is ready. Keep the
current player channel and live server unchanged while preparing individual
features. Track the pending batch in [TODO](TODO.md).

Build the client pack, update manifest and changed-files server bundle from the
same reviewed source commit and release version. Test the combined batch before
rollout. At rollout, back up and update the server first, preserving world and
configuration files, then verify startup and a client join. Only then promote
that release's candidate manifest. This is one coordinated release, with the
server ready before clients are prompted; it is not an atomic simultaneous
deployment across independent player computers. If server verification fails,
restore the server backup and leave the player channel unchanged.

## Initial deployment and superseded linked-project setup (21 October 2026)

The owner selected the new town/church/bird build for the first player release
and authorized installing it on the hosted server. Version
`0.1.0-alpha.20261021.2` replaced the earlier hosted `.1` build through a direct
`mods/jugcraft.jar` upload after backup and shutdown. Startup completed at
19:42:31 UTC; the town upgrade request was applied and the world saved at
19:45:18 UTC. Existing worlds and owner configuration were preserved. See the
[deployment record](features/living-town-upgrades.md#validation) for migration limits.

- Server: `jugcraft.modrinth.gg`.
- Stable linked-server page: <https://modrinth.com/server/jugcraft-server>.
- [Checked-in server settings](../distribution/server-project.json) keep future
  release bundles pointing at the same project. Do not create a new project per release.
- `JUGCRAFT-Join.zip` is the small Discord handout. Its Internet shortcut opens
  the official server page; players click Play and accept installation in Modrinth.
  macOS/Linux users open the URL directly. The shortcut contains no downloader.
- A linked server instance is required for automatic pre-launch update prompts.
  The downloadable `.mrpack` and versioned installer remain manual fallback tools.
- The new Server Project requires Modrinth moderation before non-members can use
  it. Creation/upload does not mean approval or a verified end-to-end update.
  Do not announce its link as live until approval and a linked installation are verified.

The older `modrinth.com/modpack/jugcraft` listing is the separate 1.21.1/NeoForge
Summer pack. Do not point current players at that pack or change its old versions.

### Sending the next update

Use the direct GitHub channel promotion described above and in the
[in-game updater guide](features/direct-player-updater.md). The earlier linked
Server Project procedure is superseded; do not submit that draft for review.
Verify a fresh import, a join and an in-place update before a wider launch.
Preserve the previous payload and matching world backup for rollback.

### Routine updates preserve the live world

Use **Files > mods > Upload file** with the replacement `jugcraft.jar` while
the server is stopped. Never use Reset server / reinstall for a routine update.
Keep `world/`, player data, `server.properties`, access lists and owner configs.
The hosting node rejected **Upload from .zip URL** on October 21 with
"required functionality is not supported by this server"; use direct uploads.

For updates involving dependencies, compare the release inventories automatically:

```sh
python scripts/package_server_update.py --previous OLD-server.zip --current NEW-server.zip --output build/server-patch
```

`JUGCRAFT-changed-mods.zip` contains only changed/new managed mod JARs. Extract it
locally, then upload its `mods/` files into the host's `mods/` folder. Check
`UPDATE-PLAN.json` for obsolete managed mods to remove before restarting and
changed default configs to merge manually. The tool never puts configs, worlds,
delete commands or player files in the patch. It rejects corrupt inventory files,
unsafe paths and changed Minecraft/loader/Java pins. Do not overwrite a different
server's files or assume a manifest reflects manual changes made outside releases.

Replacing the JAR still uploads that whole JAR; this is file-level updating, not
a binary-delta protocol. The large initial player pack is separate from the host
update. The in-game updater offers client changes with consent; it never modifies the running game.

## Prepared files

`scripts/package_release.py` assembles matching files from a tested JAR:

| File | Purpose |
|---|---|
| `JUGCRAFT-Join.zip` | Recommended Discord handout: stable server Play link and instructions for linked updates. Requires approved Server Project. |
| `jugcraft-complete-VERSION.mrpack` | Direct Modrinth import; internal name JUGCRAFT. Filename retained for tooling compatibility. |
| `JUGCRAFT-player-VERSION.zip` | Pack, Windows `Install-JUGCRAFT.cmd` and instructions. |
| `Install-JUGCRAFT-VERSION.cmd` | Single-file Windows installer: downloads the exact published GitHub pack, verifies its embedded hash, then opens Modrinth import. |
| `JUGCRAFT-server-VERSION.zip` | Server-compatible mods, config defaults, managed-file inventory and host instructions. Client-only libraries are excluded. |
| `release-manifest.json`, `SHA512SUMS.txt` | Source commit, separate pack/mod versions, platform and integrity checksums. |
| `modrinth-pack-version.json` | Pack-version fields for publication. Project ID is deliberately not invented. |

On Windows, run the standalone versioned installer after the GitHub release is
published, or extract the player ZIP and run its offline helper. The standalone
file downloads only its pinned pack into a new temporary folder, checks its hash
before opening it, and cannot fetch unpublished drafts for ordinary players.
It never follows a moving `latest` release. The offline helper verifies the included
pack's SHA-512 and asks Windows to open its registered `.mrpack` handler. Choose
Modrinth App and confirm the import. If association is missing, install
[Modrinth App](https://modrinth.com/app) and use Create instance > From file.
macOS/Linux can import the pack directly. Microsoft/Minecraft sign-in stays in
the launcher. This helper does not silently install the launcher, change security
settings, replace existing profiles/worlds or launch Minecraft. It prefers the
registered Modrinth installation, with the `.mrpack` association as fallback.
The import behavior is documented in [Modrinth's app source](https://github.com/modrinth/code/blob/main/apps/app-frontend/src/App.vue).
Actual launcher
import needs a separate human check; it is not a zero-click updater.

## Historical linked-project alternative (not selected)

Create a **Modrinth Server Project** and configure Compatibility to use JUGCRAFT
(a published pack version or a custom pack upload). Give viewers its Play link.
Modrinth creates a linked instance, prompts before installing required content,
and requires the selected pack version before launching into the server.
Changing the linked version in Compatibility prompts linked players to update
on their next launch. See [Modrinth's Server Projects announcement](https://modrinth.com/news/article/introducing-server-projects/).

A loose `.mrpack` import does **not** subscribe to future releases. A GitHub commit
or release alone does not change the Server Project's required version. See
[Modrinth's sharing guide](https://support.modrinth.com/en/articles/8797522-sharing-modpacks).
The initial download is a preview/fallback; use the linked Server Project instance
for ongoing play. Once configured, verify a fresh install and an old-version
update end to end. The launcher prompt handles update notification; social/Discord
release announcements remain manual.

## Owner release procedure

1. Merge reviewed changes. Wait for **Build** and **Foundation** on that exact main
   commit. Historical PR results or another source revision are insufficient.
2. GitHub Actions > **Prepare JUGCRAFT release** > branch **main** > unique version
   such as `0.1.0-alpha.20261021.1`. The workflow checks that source's results,
   reuses its tested JAR, verifies libraries and creates a **draft prerelease**.
3. Download the files. Stage them on a **copy** of the live world; test two clients
   joining, machines/inventories, disconnect/rejoin and a server restart.
4. Schedule maintenance, back up world/player data/configs and old managed mods,
   then stop the server. Select the exact Minecraft/Fabric/Java pins in the host
   panel and apply the tested files. Handle EULA acceptance yourself if required.
5. Publish the GitHub prerelease and verify its direct updater payloads. After the
   matching server starts successfully, promote `stable.json` using the command
   above. Announce its version and changelog. Neither source merges nor this
   workflow deploy or promote automatically.

For first installation upload `mods/`, `config/` and `defaultconfigs/` through the
host file manager/SFTP, or use a compatible pack importer. The ZIP is an upload
bundle, **not a Fabric launcher**. See [Modrinth's server installation options](https://support.modrinth.com/en/articles/8802250-modpacks-on-modrinth).
For updates compare old/new `JUGCRAFT-SERVER-FILES.json`: replace managed mod JARs
as a set so removed dependencies do not linger. Review separately managed mods
for compatibility. Merge config changes deliberately; world `serverconfig` may
override defaults. Never overwrite live worlds or owner settings with test data.

Rollback restores the **matching world/config backup and previous release files**;
changing only the JAR may leave incompatible saved data. Keep old releases immutable.

## Local assembly

After validating the exact source, run:

```sh
python scripts/package_modrinth.py verify
python scripts/package_release.py --jar build/libs/jugcraft-0.1.0-alpha.jar --version 0.1.0-alpha.20261021.1 --commit FULL_40_CHARACTER_TESTED_COMMIT --output build/release-candidate
```

Assembly is offline and needs the verified dependency cache and an empty output
directory. Local assembly does not prove the supplied commit passed CI; the
GitHub workflow enforces that gate. Changed source needs new validation/version.

## Remaining release checks

- Fresh player installation and a native Modrinth pre-launch update check with
  the JUGCRAFT helper. Modrinth listing review is no longer part of this plan.
- Deployment/startup/migration results are recorded with the particular release.
- Custom JUGCRAFT title screen and main server Play button, explicitly deferred.
- Two-account hosted-server testing.

No credentials in chat, live deployment or existing player installation changes
are required to prepare these files. AI assistance: OpenAI Codex (GPT-6).
