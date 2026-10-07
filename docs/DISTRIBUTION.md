# Installing and distributing Jugcraft

Jugcraft has two artifacts:

- **Jugcraft JAR:** the original mod. Supporting launchers can install the required dependencies declared on its Modrinth release. Manual JAR installation still requires the dependencies.
- **Jugcraft Complete `.mrpack`:** the original mod plus every selected framework and presentation integration, with exact versions and client/server flags. Import this file in Modrinth App and confirm installation to install the complete set automatically.

Opening a website alone does not install software. Fabric dependency metadata checks requirements but does not download them. Jugcraft never downloads mods at game startup.

## Current delivery status

The Build workflow produces a downloadable **jugcraft-installers** artifact containing the mod, the complete pack, a SHA-512 checksum, and standalone Modrinth dependency metadata. This is a build artifact, not an automatic Modrinth publication. There is no Modrinth project ID or publishing credential configured in this repository yet.

After a successful workflow run, open its Actions page, download `jugcraft-installers`, extract it, and import the `.mrpack` into Modrinth App. A launcher import and actual game startup must be tested before describing a particular build as verified playable. Existing automated checks do not replace a two-client playtest.

## Reproduce the artifacts

Use JDK 25, Python 3.11+, and the checked-in Gradle wrapper. On Windows replace `./gradlew` with `gradlew.bat`.

```sh
python scripts/package_modrinth.py check
python -m unittest discover -s scripts/tests -v
./gradlew build
python scripts/package_modrinth.py verify
python scripts/package_modrinth.py build
```

`verify` downloads the reviewed files to `build/framework-cache` and checks their exact size, SHA-1, and SHA-512. It is an explicit development/release check. `build` itself is offline and requires the compiled Jugcraft JAR. It discovers the single non-sources JAR in `build/libs`; use `--jar path/to/jugcraft.jar` if there is more than one.

Outputs go into `build/distributions`:

- `jugcraft-complete-<version>.mrpack`
- `jugcraft-complete-<version>.mrpack.sha512`
- `modrinth-version.json`

The pack carries Jugcraft's own built JAR in `overrides/mods/jugcraft.jar`. Each third-party library is a download entry with its upstream URL, hashes, size, and environment flags. The exact upstream version IDs also select the Gradle artifacts. The installer does not select a floating newest version.

Additional original configuration/resource files can be placed under `distribution/overrides`, `distribution/client-overrides`, or `distribution/server-overrides`. Do not add third-party JARs, credentials, local options, world saves, or personal paths there. The first foundation does not force a shader pack or invent settings files for upstream libraries.

## Standalone development and optional integrations

Default development runs include the reviewed frameworks. To verify the optional integrations are genuinely optional:

```sh
./gradlew build -PjugcraftOptionalIntegrations=false
./gradlew runClientGameTest -PjugcraftOptionalIntegrations=false
```

The common required library set remains present. Jade, JEI, client presentation libraries, and GuiLib are omitted from runtime while their APIs remain available for compilation. The Build workflow exercises the server absence path in its separate `optional-absent` job. Use the normal client tests as well to exercise the complete presentation set.

## Publishing on Modrinth

Publishing is a maintainer action after testing the assembled candidate:

1. Create/select the **Jugcraft mod** project and upload the built mod JAR. Use `build/distributions/modrinth-version.json` as the exact release metadata/dependency fragment. It is not a complete upload request: the project ID, files, and release notes are supplied during publishing.
2. Add the fragment's required/optional version relationships to that release. Only dependencies actually required by standalone Jugcraft should be marked required. GuiLib has no selected Modrinth project; its optional standalone installation is documented through its upstream source, and its exact file is included as a download in the complete pack.
3. Create/select a separate **Jugcraft Complete modpack** project and upload the generated `.mrpack`. The pack installs all selected libraries, including optional standalone integrations such as Jade.
4. Describe the tested Minecraft/loader versions, what was actually played, known issues, and update/backup guidance. Link the two projects and this repository.
5. For future automated publication, store credentials only in protected GitHub Actions secrets and add a maintainer-triggered publication job. Never commit credentials or add a startup downloader to the mod.

No publishing token is needed to generate, share, or import the `.mrpack`. The repository workflow does not merge PRs, upload Modrinth releases, or deploy a server.

## Server installations

Use a `.mrpack` server installer that honors the format's environment flags. Client-only files are marked `server: unsupported`; shared gameplay libraries, Fabric API, Jugcraft, and Jade are included on the server. The server administrator controls EULA acceptance and world backups. Do not copy the client mods folder wholesale to a dedicated server.

## Sources

- [Modrinth modpack installation](https://support.modrinth.com/en/articles/8802250-modpacks-on-modrinth)
- [Official `.mrpack` specification](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack)
- [Modrinth dependency metadata](https://docs.modrinth.com/api/operations/getdependencies/)
- [Framework catalog](FRAMEWORKS.md)
