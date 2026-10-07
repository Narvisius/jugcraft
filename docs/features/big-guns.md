# Big guns (batch 51)

Status: implemented (pending CI and review)
Proposal issue: none. The owner picked "4 and 5" from the kaiserpunk ideas list ("make a few big guns"); this is 5. Trench works (4) is batch 50.
- They asked for one gun like each of two pictures: a heavy mortar on a turntable mount, and a tracked self-propelled gun. Both pictures are third-party art and were used only as mood; all models and textures are original.
- The flak gun and observation balloon were on the same list item.

Owner: jimbozoomer-byte
Target milestone and tier: petrochemical tier (steel, gears, diesel engines, hydrogen lift cells)
Primary specialty and supported player role: combat (artillery) and scouting

## Player experience
Four new machines, each placed from its item (it faces the way you face) and knocked down into its item by players:

| Machine | What it is | Crew |
| --- | --- | --- |
| **Siege Mortar** | A fixed emplacement: a concrete ring, a railed turntable deck with hazard edging, a yellow cradle and a fat black barrel. It lobs Heavy Shells on the high arc (45° to 85°), 17 to about 110 blocks | Gunner, plus one more on the deck |
| **Self-Propelled Howitzer** | A tracked carriage with an armoured cab, a low grille engine deck and a long gun with a muzzle brake. The gun turns 30° either side of the hull and fires Heavy Shells flat or high. It is driven like the landship, slower, and burns diesel or kerosene | Driver-gunner in the cab, plus one on the deck |
| **Flak Gun** | Twin anti-aircraft cannon on a cross mount. It swings fast and fires a Flak Shell every 8 ticks while attack is held. The shells burst beside anything flying | Gunner |
| **Observation Balloon** | A kite balloon on a winch cable. Climb into its basket and it rises 32 blocks above where it was placed; climb out and it winches down. It never drifts | Spotter |

### Aiming and firing
- **Range Finder:** use it to mark the block you look at (up to 256 blocks away) as a target; it tells you how far away it is. Sneak and use it to clear the mark. Marks last 5 minutes.
- **With a target mark:** a gun turns to the gunner's own mark or, failing that, the nearest mark made within 256 blocks of it. So a spotter in a balloon can direct a battery below. The gun works out the elevation that lands the shell on the mark. If the mark is out of range, the gunner is told.
- **Without a mark:** the mortar lobs at the block its gunner looks at. The howitzer and flak gun fire where the gunner looks.
- **Firing:** attack fires as soon as the gun is on target and reloaded. The flak gun keeps firing while attack is held. Each shot uses one shell from the gunner's inventory (none in creative).
- **Gunner's hands:** while crewing a gun the gunner's own attack and use do nothing. Guns turn at a limited rate: the mortar 2° a tick, the howitzer 3° and the flak gun 12°.

### Shells
| Shell | Recipe (makes) | Burst |
| --- | --- | --- |
| Heavy Shell | steel plate + gunpowder + brass plate (2) | 32 damage at the centre, none at 5 blocks |
| Flak Shell | 2 brass nuggets + gunpowder (4) | 10 damage at the centre, none at 3 blocks. It bursts within 2.5 blocks of anything airborne, or after 30 ticks |

Every burst is the grenades' and rockets' `Blast`. It hurts living things only, walls shield it and blast protection counts. It **never breaks, moves or burns a block**.

## Connections
- Recipes:
  - Siege Mortar: `BGB / PEP / CCC`: steel plates, a steel gear, 2 pistons, a dispenser and smooth stone.
  - Self-Propelled Howitzer: `PBP / EDE / TGT`: steel plates, a dispenser, 2 diesel engines, a piston, 2 belts and a steel gear.
  - Flak Gun: `D D / PGP / I I`: 2 dispensers, steel plates, a steel gear and iron.
  - Observation Balloon: `LLL / LCL / _W_`: 5 hydrogen lift cells, a lead and a barrel.
  - Range Finder: `G G / BCB`: glass panes, brass plates and a compass.
- Input producers: the metal press, diesel engines, kinetic belts, the drones' hydrogen lift cells and refinery fuel.
- Output consumer: fighting mobs at range and scouting; it pairs with trench works (batch 50) and the landship.

## Balance and automation
- Shells and fuel are only used up, never made by the guns.
- All damage is to living things and counts as an explosion caused by the gunner, so PvP settings apply.
- The guns need a gunner; nothing fires by itself.

## Multiplayer and persistence
- **Server authority:** the server aims and fires. The client only sends the gunner's keys (`ArtilleryInputPayload`, three small numbers). Aim comes from the gunner's own synced view and the server's marks.
- **Saving:**
  - The guns save their aim, and the howitzer its fuel.
  - The balloon saves its anchor height.
  - Target marks are server memory only: they are fire orders, not world state.
- **Animation:** the guns' aim and recoil are synced numbers. The howitzer's tracks animate on each client from its motion.

## Dependencies and assets
- No dependencies. All art is original.
  - The new textures (`ar_*`) are drawn in `tools/artillery.py`, the item icons are 16×16 maps in `tools/item_icons/` (`tools/item_icons.py`; redrawn from the old 32×32 icons on 7 October 2026, [ITEM_ICONS.md](../ITEM_ICONS.md)), and the barrels use the tower guns' `tg_*` steel. The 5 October 2026 art fixes (closed barrels, whole decals, seamless paint, the smooth balloon and its smooth rise) are recorded in [big-guns-art-fixes.md](big-guns-art-fixes.md).
  - The models reuse the giants', Kaiserworks, powered-tools and zeppelin textures.
  - The howitzer's tracks reuse the landship's track link.
- The models are exported to `assets/jugcraft/artillery_quads.json` and drawn by `client/ArtilleryRenderers`. Each gun's cradle (`mortar_cradle`, `howitzer_gun`, `flak_head`) elevates with its barrel but stays put when it fires, while the barrel (`mortar_barrel`, `howitzer_barrel`, `flak_barrels`) recoils back through it; the balloon's basket is `observation_basket`, apart from the hot-air balloons' `balloon_basket`.
- Code is in `artillery/`: the entities, the shell, `Ballistics`, `Spotting`, the items and `JugcraftArtillery`.

## Verification
- Planned in CI:
  - `ballisticsLandWhereAimed`: the solver's elevations land within a block of the aim, on both arcs, for several distances and heights; 400 blocks is out of reach.
  - `siegeMortarShellsTheMarkedTarget`: a crewed mortar with a mark about 30 blocks away turns, fires (using a shell) and hurts the pig at the mark, and the floor under it is unbroken.
  - `flakBurstsBesideFlyers`: a flak shell fired up past a hovering bat bursts and hurts it.
  - `observationBalloonRisesWithItsSpotter`: a ridden balloon rises and keeps its anchor.
  - `howitzerKeepsFuelAndAim`: fuel and aim survive a save and load.
  - The client screenshot `jugcraft_big_guns`.
- Done locally:
  - `check_mod_data.py` passes. It also checks every Java constant, the renderers' pivots and the howitzer's track against the tool.
  - `check_repository.py` passes.
  - I reviewed offline renders of all four models.
- Not done: a real client, firing at a moving target and a two-player server.

## World and event applicability
Not applicable: crafted and placed by players only.

## Rollout and open questions
- Shells don't lead moving targets, and the flak gun has no predictive sight.
- Ideas for later:
  - A railway gun on booster rails.
  - Smoke and star shells.
  - Telephones carrying fire orders.
  - Block-breaking shells, only if the owner wants them.
- **Fixed 5 October 2026 (shared render fixes):** the guns and the observation balloon's envelope are drawn closed (no see-through gaps), and the quads no longer z-fight. Record: [see-through-and-flicker-fixes.md](see-through-and-flicker-fixes.md).
