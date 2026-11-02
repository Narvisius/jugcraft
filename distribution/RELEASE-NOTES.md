JUGCRAFT companions, harvest and server tools — 0.1.0-alpha.20261102.1

Changes since 0.1.0-alpha.20261031.2:
- Persistent pet roster, recall/resummon, knockout recovery, Follow/Stay/Defend/Passive modes, bond levels and abilities; ownership and stale-entity safeguards.
- Repaired 27 placed meals, added placement/serving geometry for the remaining audited foods and connected linen Feast Tables.
- Four harvest dishes, a 24-portion giant turkey feast, player Pilgrim costumes, seasonal citizen/raider outfits and a Thanksgiving Day raid truce.
- Craftable two-player chess/checkers, live in-world pieces, legal move controls, saved matches and optional mutually agreed Jugs wagers with escrow/refunds.
- Public warp/spawn and nearby held-stack trades; operator warp management, persistent god mode and read-only inventory inspection.
- Persistent operator protection cuboids, visible surveyor tool and shared town/automation protection.
- Custom NPCs and guards: names, dialogue, player-name skins, posts and patrols; seasonal overlays preserve custom appearances.
- 111 survival crafting recipes, 25 paid forge upgrades, restored Ribbit loot tables, staff behaviors, stripping and acquisition/audit corrections.
- Source-preserving repairs to 112 model templates, lunch-basket/quartz-furniture fixes and expanded missing-texture/aircraft banking checks.

Held work: the larger town-square cornucopia/decor replacement and November joke events are NOT activated. Existing worlds, owner builds, settings and player data must be preserved. No world reset or town regeneration.

Limits: /trade exchanges one held stack per player; /invsee is read-only; /setspawn changes the command destination, not vanilla death respawn. Temporary summons retain their own lifecycle. Real username skin retrieval, classic/slim appearance and human multiplayer acceptance remain playtest follow-ups. Existing missing source assets and 48 possible open-surface findings remain documented; this is not a claim that every imported asset is perfect. The harvest equipment empty-layer warning is also retained for follow-up.

Validation: focused server, resource and rendered checks are recorded in docs/features/{visual-cleanup-nov1,thanksgiving-festival-nov1,pet-inventory-nov2,server-utilities-nov2,custom-npcs}.md and the newest collection audit record. Current release-wide/CI results will be recorded separately; do not treat older passing tests as this exact release's full-suite result.

Deployment: update the server first after owner restart confirmation and a full 30-second in-game warning. Verify startup before promoting the client channel. Existing players use JUGCRAFT Updates > Download > Apply update & quit, then relaunch the same Modrinth profile. No public Modrinth listing or new launcher profile is required.

Implementation: OpenAI Codex, with owner-authorized Claude Code assistance on pets and custom NPCs. Supplied originals and notices retained.

Release exception: the owner requested skipping the full GitHub suite. Local package/asset/recipe checks and corrected Foundation groups passed; full CI is not certified. Previously documented visual backlog remains. Deployment still requires the owner's separate restart confirmation and a 30-second player warning.
