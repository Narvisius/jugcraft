# Gun controls and keyboard crash repair — 18 October 2026

Owner request: investigate [crash S0wc2rx](https://mclo.gs/S0wc2rx), make R the
reload default, move conflicting R actions, and remove gun ammunition bars from
inventory icons. Implementation: OpenAI Codex (GPT-6), on the existing local
Jug Nouveau branch. No publication or main merge is part of this change.

## Crash

The report fails in `InputConstants.isKeyDown` from `KeyMapping.setAll` when a
screen closes or the world finishes loading. The four new Nouveau shortcuts
were constructed with keyboard code -1, and the affected profile saved those
as `key.keyboard.-1`. Minecraft 26.3 reads SDL keyboard state by index and uses
`InputConstants.UNKNOWN` (0) for unbound; a negative index throws.

Nouveau defaults now use the platform's UNKNOWN value. A narrow KeyMapping
adapter repairs saved negative Jugcraft keyboard bindings during `setKey`, the
same path used by options loading. Other mods' keys and mouse bindings are not
normalized. This fixes existing profiles without replacing options.txt or
discarding unrelated player settings.

## Controls and appearance

| Action | Default |
| --- | --- |
| Gun reload | R |
| Gun inspection | H |
| Bayonet | V |
| Iris shader reload, when installed | F8 |
| JEI show recipe, when installed | Y |
| Encyclopedia/journal refresh, inside its screen | F6 |

The optional integrations are selected by binding name, with no linkage to
optional classes. Their defaults are changed at construction, so Controls > Reset
also uses the new keys. A one-time upgrade changes the exact old G/R defaults
in existing profiles. Its marker is `config/jugcraft-gun-controls-v2.txt`; later
custom mappings are preserved. Custom keyboard and mouse assignments unrelated
to those old defaults are retained during migration.

Every GunItem now hides its item bar, regardless of rounds loaded. Icons in
inventory and hotbar use the normal item presentation. The held-gun ammunition
HUD and tooltip remain; weapon ammo, reload costs, timing, attachments and server
authority are unchanged. Energy Cell items keep their separate charge indicator.

## Validation

Validated locally on Minecraft 26.3, Fabric 0.19.5 and Java 25.0.4.1:

- `assemble runClientGameTest -PclientTests=GunControlsClientGameTests,EncyclopediaClientGameTests`: passed with the pinned optional libraries present.
- `runClientGameTest -PclientTests=GunControlsClientGameTests -PjugcraftOptionalIntegrations=false`: passed.
- Repository/documentation and whitespace checks passed. The local package verifies its embedded JAR hash and repaired class files against the tested build outputs.
- Inspected the inventory capture: magazine-fed, shell and energy guns show ordinary icons at empty, partial and full capacity.

The focused `GunControlsClientGameTests` covers the
saved-key repair, default/reset mappings, conflict scan, one-time migration,
custom/mouse preservation, menu-close focus recovery, physical R reloads for
magazine/shell/energy weapons with server inventory conservation, and bars at
empty/partial/full capacity across the gun registry. It uses its own disposable
world; it never controls the owner's Modrinth game.

`EncyclopediaClientGameTests` also passed its updated F6 refresh, synchronized
progression, inventory entry points and Keys page checks. During development,
a constructor-hook initialization error and an item assertion that ran before
world registry loading were corrected before these final successful runs.

The repaired `.mrpack` and replacement Jugcraft JAR are in the task workspace’s
`work/playtests/jugcraft-gun-fixes-oct18/` folder, with instructions, checksums,
logs and BUILD-INFO.json. They include the complete preceding local core Nouveau
port. The owner’s Modrinth profile was not operated or modified. No full server
suite or independent multiplayer session was rerun for this presentation/input
change; the targeted reload test checks real server ammo/charge conservation.
