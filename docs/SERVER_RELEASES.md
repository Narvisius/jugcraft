# JUGCRAFT installation and updates

The player-facing pack name is **JUGCRAFT**. Minecraft 26.3, Fabric Loader 0.19.5
and Java 25 remain pinned. No downloader runs inside Minecraft.

## Prepared files

`scripts/package_release.py` assembles matching files from a tested JAR:

| File | Purpose |
|---|---|
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

## Recommended update flow

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
5. Publish the GitHub prerelease, upload/select its exact `.mrpack` in the Server
   Project Compatibility section, and reopen the server. Announce its version
   and changelog. Neither source merges nor this workflow deploy automatically.

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

## Deferred until the owner is awake

- Server address, Modrinth Server Project identity/access, and published pack vs
  custom upload (including any publication review needed).
- Live settings, staging capacity, backup location and maintenance window.
- Custom JUGCRAFT title screen and main server Play button, explicitly deferred.
- Actual launcher import/update and two-account hosted-server testing.

No credentials in chat, live deployment or existing player installation changes
are required to prepare these files. AI assistance: OpenAI Codex (GPT-6).
