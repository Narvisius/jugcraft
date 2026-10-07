# Explosive weapons: guncotton, grenades and the grenade launcher

Status: implemented on `feature/chemistry-18` (batch 18). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 1 October 2026: "don't want to make explosives that grief the world but maybe explosives could be used for weaponry that does damage." This replaces the backlog's explosives line (dynamite and mining charges), which could grief.
Owner: jimbozoomer-byte
Target milestone and tier: industrial chemistry, after nitric acid (batch 12)
Primary specialty and supported player role: combat

## Player experience
- **Guncotton:** two cotton nitrated in 250 mB of nitric acid in the chemical reactor make two guncotton (nitrocellulose).
- **Grenade:** a guncotton in a steel case: two steel plates, a guncotton and an iron nugget for the pin, four at a time.
  - Right-click to throw it like a snowball, one a second. It goes off when it hits a block or a mob.
  - **The blast hurts living things and nothing else.** Up to 16 damage (eight hearts) at the centre, falling to none at 4 blocks. Walls shield from it, and blast protection armour reduces it. Targets are knocked back from the grenade.
  - It **never breaks, moves or burns a block**, and leaves armor stands, item frames, paintings and dropped items alone. It is not a Minecraft explosion, so nothing can turn it into one.
  - The thrower is hurt too if they stand in the blast.
- **Grenade launcher:** steel plates, a steel gear, a rubber grip, a basic circuit and a steel ingot.
  - Right-click to fire a grenade from your inventory (the other hand first), two and a half times as fast as a throw, so it flies flatter and much further. One every 1.5 seconds.
  - With no grenades it says so. In creative it needs none.

## Connections
- Input producer: cotton (farming, batch 9), nitric acid (synthesis converter, batch 12), steel, rubber (batch 14).
- Output consumer: the player; combat against mobs.
- Technology connection: chemistry, farming.
- Magic connection: none.
- Reachable entry path: every input comes from earlier tiers.
- Required vs optional: optional.

## Balance and automation
- Cost per grenade: half a guncotton (one cotton and 62.5 mB of nitric acid), half a steel plate and a quarter of an iron nugget. Nitric acid needs the air separation unit and the synthesis converter, so grenades are a late-game consumable.
- 16 damage at the centre is close to a charged power-V bow shot; an armoured target in the open takes less.
- Cooldowns: 1 second thrown, 1.5 seconds launched, so the launcher trades rate for range.

## Multiplayer and persistence
- Server-authoritative: only the server spawns grenades and applies damage.
- **PvP:** grenades hurt players only where the server allows players to hurt each other; the damage is the owner's, through vanilla's normal player-damage checks.
- **Griefing:** none possible; blocks and decoration entities are never touched. Grenades are not saved in flight (projectiles save as vanilla snowballs do).
- **Switch:** `explosives.enabled=false` in `config/jugcraft.properties` removes the guncotton, grenade and launcher recipes. Items already made still work.

## Dependencies and assets
No new dependencies. Item textures are original (`tools/petro_textures.py`). The flying grenade uses vanilla's thrown-item renderer with the grenade item's texture.

## Verification
- `tools/check_mod_data.py` passes; the `explosives` switch is checked against `JugcraftConfig.FEATURES`.
- Game test `grenadeBlastHurtsButBreaksNothing`:
  - a zombie in the open is hurt, a zombie behind a stone wall is not;
  - the blast hurts exactly one thing;
  - glass, grass and stone next to it, an armor stand and a dropped diamond are all untouched;
  - two cotton and 250 mB of nitric acid make two guncotton.
- Not run: throwing or launching in a client, flight and aim, two players (PvP), many grenades at once.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- More warheads: chlorine, smoke and thermite grenades and the flashbang came in batch 31 ([field-chemistry.md](field-chemistry.md)); the launcher fires them all.
- Mining charges stay out: the owner does not want world-breaking explosives.
