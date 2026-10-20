# Underground bulk deposits and deep drilling

Status: implemented in PR #438; owner approved the two-block assembly and requested review readiness on 20 October 2026.
Owner: jimbozoomer-byte. Implementation: OpenAI Codex (GPT-6), under the owner's direct authorization.
Tier: optional steel workshop mining and logistics; extends the existing deposit drill.

## Player experience and approved generation

Four finite underground sources supplement ordinary ores and existing surface deposits. Each actual deposit block
contains 1,000 coal or raw-metal units, uses the existing processing routes, and drops nothing when mined by hand.
Stone and deepslate variants share a resource family and leave the corresponding host when exhausted.

| Family | Centre Y (min / peak / max) | Attempts per eligible chunk | Envelope; actual deposit blocks |
| --- | --- | --- | --- |
| Coal | 16 / 48 / 80 | 1/48 | Tilted seam 12–20 × 6–10 × 1–3; 64–128 blocks |
| Iron | -48 / -16 / 32 | 1/64 | Irregular lens 8–14 × 6–10 × 3–6; 48–96 blocks |
| Copper | -16 / 16 / 48 | 1/64 | Sloping branching vein 10–18 × 4–8 × 2–5; 40–80 blocks |
| Tin | -40 / -8 / 24 | 1/96 | Narrow inclined vein with pockets 8–14 × 3–6 × 2–4; 24–64 blocks |

Eligibility uses the surface biome, including beneath cave biomes:
- Coal: woodland, coniferous forest, highland, basin; vanilla forest, taiga, windswept hills/forest.
- Iron: shield, highland, basin, tundra; vanilla stony/jagged peaks and windswept gravelly hills.
- Copper: canyon, outback, dryland, volcano; vanilla badlands, wooded badlands and savanna plateau.
- Tin: shield, basin, highland, temperate rainforest; vanilla windswept forest, old-growth pine/spruce taiga, stony peaks.

Natural host rock only, at least 12 blocks of cover. Preserve caves, fluids, existing ores, structures and block
entities. Random seeded shapes have connected cores and uneven edges. Attempts can fail the placement checks.
Overworld/new chunks only, no retrogeneration. Existing feature switches continue to govern generation.

## Connections and balance

The basic drill retains its 5×5×3 reach, 16 JE/t and one item per resource family per 300 powered ticks.
A craftable steel-tier assembly adds a two-block-tall mast to that same rig and reaches a 9×9 column 128 blocks below it.
Its lower support collar extends into the rig's recessed frame so it visibly rests on the gantry. Both mast
sections need clear, permitted space; either section can be broken to remove the upgrade, dropping one assembly
in survival and none in creative. The completed rig is four blocks tall.
It bores downward one layer per 20 powered ticks at 32 JE/t, without carving a quarry or generating spoil.
At each working depth it extracts one item per family every 300 powered ticks; shared speed/efficiency cards apply
to extraction. The upgrade changes access, not reserve capacity or base output rate.

Inputs: four steel plates, three steel gears, a basic circuit and a steel pickaxe as the cutting head;
no circular dependence on underground deposits.
Outputs: existing coal/raw iron/raw copper/raw tin, consumed by existing generators, furnaces and ore processing.
Item pipes, chests and conveyors retain their roles. No magic or new dependency is required.
The Prospector distinguishes bulk deposits from ordinary ore and indicates approximate depth and signal.

## Multiplayer and persistence

Server-authoritative extraction consumes the shared per-dimension reserve. Multiple drills cannot independently
duplicate it. Save boring depth and partial progress; power loss and full outputs pause work. Respect protected
targets, bedrock and loaded chunks. Deposits cannot be moved by pistons to refresh their reserve. Rebuilding a
machine does not reset the deposit. Existing registrations remain available when generation is disabled.
The rig records its placing player (legacy rigs record the first operator). The shared Authority helper supplies
a permission-only stand-in for an offline operator or an unprivileged identity for an unclaimed legacy rig.
Current spawn, town, border and Fabric break-permission checks still apply. This does not alter the magic branch's
absent-owner option. Work does not force chunks to load; an unloaded corner pauses the whole deep working layer.
Removing the assembly returns to basic mode; reinstalling it resumes the saved depth. Boring stops at bedrock.

## Dependencies and assets

No dependency changes. Extend Jugcraft's existing procedural deposit textures and dieselpunk machine models.
Keep the owner asset library unchanged; record final asset provenance and runtime inspection evidence here.
The library catalog's core-sample/drill textures were considered. Existing `tools/deposits.py` rubble and ore
palettes provide the family reference; the new dark host is authored in the same generator. The mast reuses
Jugcraft's `dp_gunmetal`, `dp_chrome`, `dp_olive` and `dp_hazard` textures and shared model-writing helpers.

## Verification

- 20 October: all **13 focused server game tests passed**, using `runGameTest -I build/focused.gradle
  -PgameTestFilter=jugcraft-test:*deposit*`. Includes the three existing surface-deposit regressions and ten
  new shape, cover, host preservation, basic/deep reach, power/full-output pause, saved-payload round-trip,
  protection, shared-reserve, speed/efficiency, Prospector, recipe and configuration cases.
- Shape coverage: 160 deterministic seeds per family, checking connected cores, count bounds, variation,
  dimensions and depth distribution.
- Repository structure/documentation links and the full material/data/recipe audit passed. Shared art checks
  accepted the mast model. Deposit JSON/translations and the handbook match their generators.
- Five focused survival-recipe graph tests passed, including circular-unlock prevention.
- The actual runtime item-registry export passed. The refreshed acquisition audit has zero ingredient
  diagnostics, a reachable assembly recipe and exactly five new registered items (assembly plus four dark-host
  deposits). Natural deposit block items are explicitly noncraftable; their extracted resources retain existing
  survival routes. The preview JAR built successfully.
- `UndergroundDepositClientGameTests` passed in a normal Overworld with seed
  `jugcraft-underground-deposits-1`. Ordinary generation produced connected coal/iron/copper/tin bodies of
  111/92/55/41 blocks. Surface/cave biome feature attachments, the rendered drill menu, and real save/reopen
  preserving all four bodies, a 777-unit reserve, depth 8 and 123 extraction ticks passed.
- That client check exposed the shared menu's assumption that every processor has an input slot. Input labels
  now require a real input slot; the drill's output-only menu opens and renders.
- Inspection screenshots show natural bodies in lit glass cutaways and a separate artificial shape/rig gallery.
  The source world is preserved; only unused development datapack/registry metadata is removed from delivery
  copies, with all other saved values and region bytes checked.
- Two-human-client playtesting remains separate from automated coverage.
- Owner's two-block mast revision: all 14 focused deposit/drill server tests passed, including upper-space
  obstruction, rig-click and ordinary block-item placement, breaking either half for one survival drop,
  and creative removal without a drop. Both mast halves and their fit against the existing rig passed
  the shared geometry/seam checks. The focused client inspection rendered the full mast and support collar,
  opened the drill screen, and saved/reopened both halves with depth and partial progress intact.

## Inspection world

The gallery starts at `516 249 511`. The upgraded rig at `520 247 520` is paused at depth 8 with partial
progress; place the supplied coal generator at `521 247 520` and fuel it to resume. Its chest is at
`520 247 519`. Use the supplied assembly on the shorter basic rig to try upgrading it.
Natural viewing-room coordinates: coal `-80 59 336`, iron `11856 -34 -643`, copper `4027 -2 1118`,
tin `11883 7 -742`. Their original deposit blocks remain intact.

## Rollout

Publish as a draft PR, provide a separate inspection world, and await owner visual review before readiness.
Back up worlds before removing content; no rewriting of existing terrain is planned.
