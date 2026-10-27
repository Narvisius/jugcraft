# Concordance Chunk B: winter and stone (contract and handoff)

Status: contract written; units 1 to 5 implemented, their evidence recorded; waiting for the owner's checkpoint. Units
1 to 5 are on branch `claude/chunk-b-winter-stone`, based on main `feb6d04fb`
([#352](https://github.com/jimbozoomer-byte/jugcraft/pull/352), which carries Chunk A); not merged. Unit 5's candidate,
`25644502a` (units 1 to 5), passed every job of run 38591108210 on its second attempt; the first attempt failed two
tests outside Chunk B, a nuclear plant's radiation reading and a Peepo delivery test, which passed when re-run
([Unit 5: evidence and handoff](#unit-5-evidence-and-handoff-b-09-b-11)). Units 1 to 4 passed every job of run
38549876709 on `b012875ab`, as did run 38514144531 on `579ab5f6f` (unit 1), run 38519234934 on `06dc5ea5f` (units 1
and 3) and run 38543250503 on `1266c905a` (units 1 to 3) (see the Validation plan). The first run, 38509414542, failed only the two
four-player tests, which expected more health than the game's 1024 cap allows; the tests now expect the cap. Unit 2's
first run (38534127130 on `0e7de6bf1`) failed the charge test its review had predicted and found the King's ice left
over after an unload; both are fixed (`215a1c1e0`, `1266c905a`). Unit 4's first run, 38549876709, passed.
Records: unit 2's [yeti-king.md](yeti-king.md#verification) and
[glacier-hall.md](glacier-hall.md#verification), [unit 3's](arcane-concordance-rime-strata.md) and
[unit 4's](arcane-concordance-winter.md); unit 5's B-09 in
[yeti-king.md](yeti-king.md#optional-counters-concordance-chunk-b-b-09), and its fixes and evidence here (B-09 and B-11
as built, and [Unit 5: evidence and handoff](#unit-5-evidence-and-handoff-b-09-b-11)).
The contract was written at `d4c07bf56`, the head of Chunk A
([#337](https://github.com/jimbozoomer-byte/jugcraft/pull/337)), when main was
`b73f51c8a` ([#321](https://github.com/jimbozoomer-byte/jugcraft/pull/321)).
Proposal: the owner chose Chunk B on 13 October 2026 ("Ok lets begin Chunk B"), from
[the expansion plan's delivery chunks](../plans/concordance-expansion-2026-10-10/09-claude-delivery-chunks.md) (section 4,
B1 to B4), with [spellcraft](../plans/concordance-expansion-2026-10-10/02-magic-progression-and-spellcraft.md),
[workshops](../plans/concordance-expansion-2026-10-10/03-workshops-ecology-and-pacts.md) and
[the existing bosses](../plans/concordance-expansion-2026-10-10/04-existing-bosses-and-shared-encounters.md). The same day
the owner put Chunk B's commits on the stacked branch `claude/chunk-b-winter-stone` (on Chunk A) and answered three
decisions (B-D1 to B-D3, under Decisions).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.

This page is the chunk's contract (chapter 9 §1 step 7 of the plan) and, as the units land, its technical handoff
(chapter 10 §7). Plan chapters are cited as `NN:line`. Code is cited as `file:line` at `d4c07bf56`: Java paths are
under `src/main/java/io/github/jimbozoomer/jugcraft/`, data under `src/main/resources/data/jugcraft/`, tests under
`src/gametest/java/io/github/jimbozoomer/jugcraft/test/`, and `mc263src` is the decompiled 26.3 reference. A fact
marked *(survey)* comes from this chunk's survey reports or the plan's review and was not re-read for this page.

## Player outcome

The plan's outcome is to practise Rime and Strata in ordinary play, use existing Thallite, prepare for cold exploration,
reach a Rime/Strata workshop and fight the Yeti in his accepted hall (09:27, 09:86). Corrected for what is already built
(see the status delta), a survival player can:

- reach the Glacier Hall in an ordinary world and in a designed world (B-03);
- share the Yeti King's reward, or the Cinder Tyrant's, by damage, healing, warding or, against the Yeti King, an
  objective; keep it if they fall or leave the game in the fight; and be paid exactly once, a killing blow included;
- read both keepers' dossiers and JEI's pages for their loot;
- learn Rime and Strata through typed evidence, and cast Rimebind, Brace and Preserve;
- brew Winter Brine at the Alembic Crucible, the winter workshop: a timed Frostward that the Yeti's frost respects, while
  the Yeti Mitten stays the permanent prize;
- make and wear the owner's water sets, foci and bangle as Rime's travel kit, from ordinary materials;
- use Brace and Rimebind against the Yeti if they choose; neither is needed to win.

Already built, and not rebuilt here: the Glacier Hall, the Frost Horn, the Yeti King, the Cinder Kiln, the Kiln Seal and
the Cinder Tyrant (#320); Thallite ore and gear (slices 1 and 2); the Alembic Crucible; Chunk A's encounter credit and
kept rewards.

## Units

Each unit is a reviewable boundary: its own run of commits on `claude/chunk-b-winter-stone`, stacked on #337 until #337
(or a pull request that carries it, see #352 below) merges, with its own record and green checks before the next unit
starts.

| Unit | Tickets | What it delivers | Record (proposed) | State |
|---|---|---|---|---|
| 1 | B-01, B-02 | The Yeti King on Chunk A's encounter credit, kept rewards and exactly-once payment; then the Cinder Tyrant in a separate commit (B-D1); both keepers' dossiers, route step and JEI pages | `lair-encounter-credit.md`, `yeti-king.md`, `cinder-tyrant.md` | Implemented, not merged; CI passed (runs 38514144531 and 38519234934): B-01 and B-02 committed for the Yeti King and the Cinder Tyrant, each keeper in his own commits (B-D1) |
| 2 | B-03, B-04 | Glacier Hall access (ordinary and designed worlds) and lifecycle; the Yeti fight held to the completion evidence (charge and columns, footing, cleanup, frost) | [glacier-hall.md](glacier-hall.md), [yeti-king.md](yeti-king.md) | Implemented, not merged; CI passed (run 38543250503, every job): B-03's tests (`98c550f16`), B-04's (`dca611f85`), the review's fixes (`0e7de6bf1`) and three fixes to the fight (`1dec9f9ef`, `215a1c1e0`, `1266c905a`). The first run, 38534127130, found the two faults the last two fix |
| 3 | B-05, B-06, B-07 | Rime and Strata research and evidence; Rimebind and Brace; Preserve on the crucible, with Jade's hold. The crafted upgrade (09:94) is unit 4's water foci (B-P14). One unit, because research may unlock only invocations that exist and each invocation must be unlocked by its research (`tools/check_mod_data.py:10321-10323,10688-10693`) | [arcane-concordance-rime-strata.md](arcane-concordance-rime-strata.md) | Implemented, not merged; CI passed (run 38519234934; with the status shots' toast fix, `7d6d0b0d3`, runs 38543250503 and 38549876709). The fixed shots show the status icons (runs 38534127130 and 38549876709) |
| 4 | B-08, B-10 | The winter workshop and gear: Winter Brine and Frostward at the crucible (B-D3); the Rime travel kit from the owner's water sets (B-D2); Thallite and the trophies linked without stat changes | [arcane-concordance-winter.md](arcane-concordance-winter.md); `arcane-concordance-alchemy.md` | Implemented, not merged; CI passed (run 38549876709, every job, its first run): B-08 (`158717e6c`), the kit (`4be454f83`), the links (`eb6099144`), the Glacier Hall's preparation fact (`7e1efb5f9`, on unit 1's dossiers), the records and the review's fixes (`7cb7a87c9`) |
| 5 | B-09, B-11 | Brace and Rimebind as optional counters in the Yeti fight; combined fixes, evidence and the owner checkpoint | [yeti-king.md](yeti-king.md#optional-counters-concordance-chunk-b-b-09); this page | Implemented, not merged; CI passed (run 38591108210 on `25644502a`, every job on its second attempt; the first failed only two tests outside Chunk B): the counters (`25be6e227`), the column elective (`9cb934ec7`), the encyclopedia home's route cards (`d22a55830`), the client evidence (`fc9b404dd`), the records and the review's fixes (`25644502a`); evidence in [Unit 5](#unit-5-evidence-and-handoff-b-09-b-11); the owner's checkpoint is awaited |

## Baseline (B-00)

- Base: `d4c07bf56` (worktree branch `wip/chunk-b`, to be pushed as `claude/chunk-b-winter-stone`); main `b73f51c8a`,
  unmoved when checked by REST on 14 October 2026. The working tree was clean.
- Framework lock: no pin or dependency change is planned.
- Base checks, run on 14 October 2026 at `d4c07bf56`: `python3 tools/check_mod_data.py` passed (2189 IDs; data and recipe
  audit, no build or game test) and `python3 scripts/check_repository.py` passed (structure and links). #337's CI on
  `d4c07bf56` passed every job: 10 check runs, all successful (`mod`, `optional integrations absent`, `repository`,
  `choose client tests`, five client shards and `client`), read by REST on 14 October 2026.
- Chunk A changed nothing under `lair/yeti/`, `lair/tyrant/`, `concordance/alchemy/`, `concordance/CrucibleBlockEntity.java`
  or `tools/concordance_alchemy.py`: `git diff --stat b73f51c8a d4c07bf56` on those paths is empty.

### Status delta: the plan's text against the code at `d4c07bf56`

| The plan says | What is true | Evidence |
|---|---|---|
| B3: "Implement the existing planned behavior in chapter 04" (09:100); the Yeti is an "existing plan, not yet an entity" (04:3, 04:120) | Built and on main through #320: wake, hunt, charge and column stun, roar and blizzard, whelps, fury, reset, loot and the Grey Mist. B3 becomes integration, hardening and counters | `lair/yeti/YetiKingEntity.java:463-479` (wake), `:975-1016` (charge), `:1001-1003`, `:1047-1059` (column stun), `:1306-1325` (reset), `:1345-1370` (death); `docs/branches/BOSSES.md:22` |
| B1: "Inspect PR300 or its merged successor" (09:90) | The successor is `c095d57d3` (#320): hall, horn, dimension and lair blocks are merged, and Chunk A left them alone | the `git diff --stat` above; `concordance-chunk-a.md:40-47` |
| Rewards: a "Glacier Maul/Rimeclaw trophy choice" (04:135) | One of the two at random, as the accepted brief says | `loot_table/entities/yeti_king.json` (one pool at 0.15 holding both); `loot_table/bosses/yeti_king.json`; `lair/yeti/YetiKingLoot.java:59-65`; `BOSSES.md:109` |
| Fair contribution (04:149-174) | The Yeti and the Tyrant still use the old path: a private `damageBy`; `participants()` counts players who are alive, in the level and within 64 blocks; direct payment; no `claim()`; a tamed creature's blow counts for no one | `YetiKingEntity.java:242,1282-1296,1359,1406`; `lair/yeti/YetiWhelpEntity.java:103-104`; `lair/tyrant/CinderTyrantEntity.java:270,1533-1547,1618,1687` |
| Preserve, Rimebind and Brace (02:77-79) | Absent: ten invocations, none of them Rime or Strata | `concordance/invocation/` (aegis, cinderbolt, flashstep, hearthflare, hearthguard, hearthspark, kindle, lance, lanternward, revelation) |
| Strata research (taken as missing) | Strata already has two entries: `assay` (Balancewrights; examines `#jugcraft:assay_specimens`) and `runesmithing` (Runesmiths). Rime has none. The Rimekeepers' and Stratawrights' rows give only their roadmap step | `concordance/research/assay.json`, `runesmithing.json`; `concordance/practice/assay.json`; `docs/ARCANE_CONCORDANCE.md:121,123` (the Hearthbinders' row 120 says "implemented") |
| "Use existing Thallite" (09:86; 01:38) | Ore and gear (slices 1 and 2) are merged and tested; slices 3 to 5 (arms; arrow and horse armour; the Focus, Earthen Steps and Bulwark) are not built. The record's status lines are stale. No Concordance data mentions Thallite | `docs/features/thallite.md:5-8,136-141,194`; `ThalliteGearGameTests.java:60-222`; no match for "thallite" under `concordance/` data |
| "A boss drop offers a distinctive variant, not the first and only cold protection" (04:141) | The Yeti Mitten is the only full counter to his frost: `chill()` checks the Mitten and not `canFreeze()`. Jugcraft adds only knitwear to `freeze_immune_wearables`. **Shown by `YetiKingGameTests.woolAndLeatherStopTheFreezeButNotHisFrost`** (unit 2; passed in run 38543250503): a knitted sweater or a leather tunic stops the freeze's damage through vanilla's gate, but not his frost, its slowdown or its thaw, which are the same as on bare skin; only the Mitten stops both (vanilla's freeze damage needs `canFreeze()`, which one listed piece turns off; the frost and its slowdown do not ask it). *Since unit 4:* Frostward (Winter Brine) is the timed counter beside the Mitten, and the same is shown for the kit by `YetiKingGameTests.aKitWearerIsSlowedButNotHurtByHisFrost` and the client test (both passed in run 38549876709) | `YetiKingEntity.java:960-967` (the Mitten at `:961`); `lair/yeti/YetiMitten.java:23-35`; `data/minecraft/tags/item/freeze_immune_wearables.json`; mc263src `LivingEntity.java:534-543` (slowdown), `:2938` (damage), `:3756-3760` (`canFreeze`) |
| Winter Brine: "Too much heat consumes the temporary cold property" (03:36) | Heat never consumes Rime: a searing stir damages only Radiance and Verdance. An unheated crucible sits at 20, inside the cold band, and nothing cools one below that. The Rime property gives Resistance. *Resolved by B-08 (unit 4):* every stir above the cold band keeps only part of the dissolved Rime, and Rime gives Frostward | `concordance/alchemy/Mixture.java:26,82-85`; `Band.java:12-15`; `Heat.java:12-20`; `tools/concordance_alchemy.py:95-103,121-122` |
| A late-joining client receives phase and elapsed state (08:127) | Shown for one client leaving and returning (unit 2, `YetiKingClientGameTests`, passed in run 38543250503): back in his sight, his bar shows again and his fury reaches the client. The elapsed time stays unsynced. A second client joining mid-fight is not run | `YetiKingEntity.java:194-195,1457-1460` |
| Designed-world access (09:90; 07:13) | Determined (unit 2): a world created with the designer's generator in place of the Overworld's has every lair's dimension; the Frost Horn opens a Glacier Hall there and the Kiln Seal a Cinder Kiln, in the created world and again after reopening it (`WorldDesignerClientGameTests`, passed in run 38543250503). The exported preset names only the three vanilla dimensions and writes no dimension files (`model.test.cjs`), and the game adds data-pack dimensions to any new or reopened world. Nothing in `world/design/` mentions a lair. A world made from an actual exported ZIP is not loaded (B-03, the deliberate deviation) | `WorldDesignerClientGameTests.java:33-61,64,89` at `d4c07bf56`; mc263src `CreateWorldScreen.java:249`, `WorldOpenFlows.java:165-166`; `docs/WORLD_DESIGNER.md:55` |
| Open PRs #299, #300 and #301 (09:11) | 34 open (#322 to #355) at 00:30 UTC on 14 October 2026; #354 and #355 opened after midnight that day | the table below |

### Two payout defects (both bosses)

1. **A player whose only hit is the killing blow is not paid.** Vanilla calls `die()` inside `hurtServer` (mc263src
   `LivingEntity.java:1145,1218`). The Yeti credits a blow only after `super.hurtServer` returns
   (`YetiKingEntity.java:1405-1407`), and the Tyrant likewise (`CinderTyrantEntity.java:1686-1688`), so the payout comes
   first. Chunk A fixed this for Vesperine (`lair/vesperine/VesperineEntity.java:1271-1281`).
2. **A second `die()` pays a second time.** `defeated()` has no guard (`YetiKingEntity.java:1356-1359`;
   `CinderTyrantEntity.java:1614-1618`). Vesperine asks `encounter.claim()` first (`VesperineEntity.java:1289-1291`;
   `lair/Encounter.java:390-397`).

B-01 fixes both, for both bosses (B-D1).

### Open pull requests (REST, 14 October 2026; refresh before each unit, 09:11)

| PR | What it is | Bearing on Chunk B |
|---|---|---|
| [#337](https://github.com/jimbozoomer-byte/jugcraft/pull/337) | Chunk A, head `d4c07bf56`, not merged | The base. A review change means a rebase |
| [#352](https://github.com/jimbozoomer-byte/jugcraft/pull/352) | "Integrate October 13 community content", base main, 5,840 files, head `c692e2943`; combines 31 pull requests, #322 to #351 and #353, #337 at `d4c07bf56` among them (its record `docs/features/oct13-community-integration.md`, on that branch). It carries #351 at `b7f0ec1` and #353 at `7ed1804`; both have moved on since (`c619e89b5`, `9cfe0fdab`). Integration and fixes by OpenAI Codex, per its body; open until its combined checks pass | If it merges, Chunk B rebases onto main. At its head, `LairDossiers`, `Encounter`, `LairRewards`, `Lairs`, `YetiKingEntity`, `YetiKingLoot`, `CinderTyrantEntity`, `ConcordanceEffects`, `PageWriter`, `FirstLightRoute`, `tools/acquisition.py`, `tools/encyclopedia.py` and `tools/concordance_alchemy.py` equal the base; `CrucibleBlockEntity.java` differs (from #323). It keeps #337's encyclopedia as the live one, with #322's guides as a child screen |
| #323 | Peepo companion jobs, 154 files | Changes `CrucibleBlockEntity.java` (+13 −4: a companion field, an extracted `matchesIngredient`, and filter save and load in `saveAdditional` and `loadAdditional`), the methods B-07 and B-08 touch |
| #322 | The fork's encyclopedia | Add/add conflicts with #337 *(survey)*; reconciled inside #352 |
| #331, #336 (Mire Hag); #350, #353 (Crypt Lich) | New lairs and bosses | Shared hot spots: `BOSSES.md`, `CHANGELOG.md`, the gametest `fabric.mod.json`, `check_mod_data.py`, lair tools and tags. Both bosses keep the old damage-only path (`damageBy` at `MireHagEntity.java:249` and `CryptLichEntity.java:258`, read at #352's head), so B-02's checks must not require JEI pages of every boss with a loot table. #336's `FenLampBlock` has its own relit listener *(survey)* |
| #330 | Cinder Kiln light, 37 files | No Java change to the Tyrant: art, the kiln's structure, its tools and records. It shares `CHANGELOG.md`, `tools/check_mod_data.py` and `docs/features/cinder-tyrant.md`, which B-01 and B-02 update. Its light-flood audit could serve the hall *(survey)* |
| #332, #338, #344, #355 | Guns 12A to 12C and 13A | #332 gives Levitation to every creature an End gun hurts *(survey)*; B-04 tests the Yeti and his whelps against it if #332 or #352 merges first. #344 (29 files, on #338) adds no on-hit effect *(survey)*. #355 (30 files, on #344) shares `CHANGELOG.md` and the gametest `fabric.mod.json` |
| #343 | Snowy-region fluorite, 59 files at `6256dd864` | A new cold ore. It also changes `tools/owner_art.py` (which units 3 and 4 import through), `recipe_view.json` (B-02, B-08), `check_mod_data.py`, the gametest `fabric.mod.json`, `JugcraftMaterials`, `OreSurvey` and a `has_surface_ore` biome tag. Chunk B must not create or depend on an ore (09:94) |
| #351, #354 | Spodumene deposits (91 files at `c619e89b5`); regional mineral sands (draft, 92 files) | Both change `tools/owner_art.py`, `recipe_view.json`, `check_mod_data.py` and the gametest `fabric.mod.json`, as #343 does; no lair, Concordance or Thallite file |
| #324 to #329, #333 to #335, #339 to #342, #345 to #349 | Industrial, power and nuclear work | No Chunk B surface by title. From #333 on they add Thallite chloride leaching *(survey)*, so Chunk B must not rename Thallite's ids or features |

### Checks before each unit lands

- `python3 tools/check_mod_data.py` and `python3 scripts/check_repository.py`.
- Refresh the open pull requests; rebase if #337 or #352 has merged.
- The unit's own tests (see the Validation plan).

## Tickets

### B-01. The Yeti King, then the Cinder Tyrant, on Chunk A's encounter credit

**What the player sees.** A player who only heals or wards others against the Yeti qualifies for his reward. One who
fell, or left the game, in the fight is paid later. A killing blow alone qualifies. A tamed creature's blow credits its
owner. A player who already has a part in his fight and baits his charge into a column qualifies by that deed. Every
reward is paid once.

**Changes** (the pattern is Vesperine's: `VesperineEntity.java:236,1271-1296`; `JugcraftVesperine.java:58`;
`VesperineLoot.java:42,66,78,83`):
- `YetiKingEntity.java`:
  - `damageBy` (`:242`) becomes an `Encounter` round `centre()`, with an `encounter()` accessor for the tests;
  - `wake` (`:463-479`) calls `begin()`, and the fight begins again after a load if he is awake;
  - `took` (`:1282-1284`) feeds `encounter.damage`; `participants` (`:1287-1296`) reads `encounter.qualified()` and
    keeps its signature;
  - `reset` (`:1316`) calls `abandon()`;
  - `die` (`:1345-1350`) credits `Encounter.striker(source)` first; `defeated` (`:1356-1370`) asks `claim()` before
    anything else and pays through `LairRewards.pay(level, YetiKingLoot.BOSS, encounter, position(), centre(), EXPERIENCE)`;
  - `hurtServer` (`:1406`) and the whelp (`YetiWhelpEntity.java:103`) credit `Encounter.striker(source)`, so pets count.
- `YetiKingLoot.java`: `BOSS = "yeti_king"`; `pay(ServerLevel, Vec3, ServerPlayer, int)`; `roll` by origin beside the
  entity overload, which `YetiKingGameTests.java:371` calls; `table` takes the origin instead of `king.position()`
  (`:72-77`); `reward(...)` delegates.
- `JugcraftYeti.register` (`:42`): `LairRewards.payer(YetiKingLoot.BOSS, YetiKingLoot::pay)`.
- **The column deed** (B-P3, revised 14 October 2026). On the last tick of the charge's wind-up, where its line is fixed
  (`YetiKingEntity.java:983-990`), he notes whom it is aimed at, but only if that player already has a part in his
  fight: a share (`Encounter.share(player) != null`, `Encounter.java:350-353`), which any blow, credited healing or a
  ward cast on another player in the fight creates, even short of qualifying (`:274-279`, `:316-322`, `:326-330`). When
  the charge ends in a column stun (`YetiKingEntity.java:1001-1003`), that player, if still eligible and in his fight,
  earns `encounter.deed(player, "column " + their uuid)`:
  - one key per player, so two baiters each earn theirs; a constant key would credit only the first, because a fight
    keeps one set of deed keys (`Encounter.java:282-288`);
  - being struck by his fight is not a part: the hurt map (`Encounter.java:72,292-296`) is not read. `target()` falls
    back to the nearest eligible player (`YetiKingEntity.java:604-623`), and one deed qualifies
    (`Encounter.java:101-103`), so an idle late arrival he turns on earns nothing by where the charge ends (04:170;
    10:58), and one trampled by a Fury charge cannot earn the chained one that follows (`YetiKingEntity.java:1037-1044`);
  - a stun called any other way (`stun()`, `:1047`) credits no one.

  The earlier rule, "only a foe who moved 2 blocks or more during the wind-up", was dropped: a late arrival who walks in
  during a wind-up would pass it, and it needs each player's position tracked every tick. A pure dodger with no blow,
  healing or ward earns nothing from the deed; it mostly helps support players short of the threshold.
- **The Tyrant, in a separate commit** (B-D1, done): the same changes at
  `CinderTyrantEntity.java:270,492-497,577,1533-1547,1570,1602-1618,1686-1688`, `CinderlingEntity.java:137-140`,
  `CinderTyrantLoot.java` (`BOSS = "cinder_tyrant"`; `pay`; `roll` by origin beside the entity overload; `:74` uses
  `tyrant.position()`) and `JugcraftTyrant.java:41`. His fight begins again after a load once he has landed: a load
  turns waking into the Kiln and erupting into the Eruption (`CinderTyrantEntity.java:1807-1811`). Waking on a blow
  still needs a player (`:1677`), so a pet's bite does not wake him.
  - No deed (B-P4): nothing records who turned a sluice. `SluiceGateBlock` is a plain block with no block entity
    (`SluiceGateBlock.java:46`); turning it writes only its flow and its trough's flood (`:243-259`), and the quench
    reads only the flood (`CinderTyrantEntity.java:1050-1062`).
  - Of his harm, his blows and his own fire are the fight's hurt (the source's entity is he or a Cinderling: the sweep
    and slam, `mobAttack`; the breath, wave and fire patches, his flame `DamageSource`, `:474-476`; the gobs and
    cinders, `mobProjectile` with him as owner, whose entity is the owner in vanilla's build, not asserted by a test; a
    Cinderling's bite, vanilla's `mobAttack`, likewise). The slag's heat (`hotFloor()`, `MoltenSlagBlock.java:56`) and
    burning (`onFire()`) have no entity, so healing them earns nothing (B-P5). Giving them him as their cause was
    rejected: the slag would scale with difficulty, knock players away from him, turn their pets on him and count the
    crucible's permanent slag, and burning would need a mixin.

**Reuses, unchanged:** `Encounter` and `LairRewards`; the effect observer at `lair/Lairs.java:82-83`; the saved formats
of `jugcraft:lair_rewards`.

**Tests (server):**
- `YetiKingGameTests.supportSharesHisLootOnce`, modelled on `VesperineGameTests.supportSharesHerHarvestOnce` (`:342`): a
  healer qualifies; a fallen striker is kept and paid on respawn; a player who left the game in the fight is paid on
  joining; a bystander and a wanderer are not paid; the experience is shared among those paid; a second `die()` pays
  nobody.
- `aKillingBlowAloneIsPaid`; `aPetsBlowIsItsOwners` (on him and on a whelp).
- `twoBaitersEachEarnTheirDeed`: two players, each with a part short of qualifying, charged into the column in turn,
  earn one deed each and qualify by it; the key is `column <player>`, once a fight. `anIdleLateArrivalEarnsNothing`: a
  player his fight has struck but who has no part, charged into a column, gets no deed and no reward.
- `fourPlayersAreEachPaidOnce`, the party cap (04:194): his health scales, and four shares are paid once each.
- `aFightLoadedAwakeIsRecordedAfresh`: a real entity save and load (`saveWithoutId`/`load`); loaded awake he holds no
  one's part and begins a fresh record.
- The records state how each lifecycle event is produced:
  - *death:* the Yeti's support test performs one real `ServerPlayer.die` on a mock player, so Fabric's `AFTER_DEATH`
    (fabric-api 0.161.0 `ServerPlayerMixin.java:76-79`) reaches `Encounter.java:148-153` and `Lairs.java:93-97`; the
    blow that would kill is not real. The Tyrant's marks death with `encounter.excuse`, as Chunk A's tests do: the death
    path is shared code, so proving it once is enough;
  - *leaving:* marked with `encounter.left`, as Chunk A's tests mark it (`concordance-chunk-a.md:134-135`), on a Fabric
    `FakePlayer` the player list never held (it has a connection, so `LairRewards.welcome` can run); the real `LEAVE`
    path (`Encounter.java:156-162`) stays not run;
  - *respawn and join:* `LairRewards.handOver` and `welcome` are called directly (their events are at
    `LairRewards.java:105-106`);
  - *save:* in-process entity and attachment round trips, not a restart;
  - *party:* four mock players, not four people;
  - unchanged from Chunk A: no real disconnect and no two clients.
- `LairEncounterGameTests.aKeptYetiKingRewardIsSavedAndPaid`: a kept `yeti_king` Due survives a save and is paid by the
  Yeti's payer (the pattern of `keptRewardsSurviveASave`, `:322`).
- `CinderTyrantGameTests`, the same for the Tyrant without the column cases: `supportSharesHisHoardOnce` (a healer of
  his Tail Sweep qualifies; one still down as he falls is kept and paid on their feet; one who left is kept in the
  world; a bystander, a wanderer and the one he only hurt are not paid; a second `die()` pays nobody),
  `aKillingBlowAloneIsPaid`, `aPetsBlowIsItsOwners` (on a Cinderling, and the felling bite on him; a wild wolf's bite
  is no one's), `healingHisSlagAndBurningEarnsNothing` (B-P5: mending slag and burning earns nothing, mending his
  Kiln Breath qualifies), `fourPlayersAreEachPaidOnce` and `aFightLoadedAwakeIsRecordedAfresh` (loaded waking, he
  lands in the Kiln). `Encounter.REACH` (64) is wider than his arena's spacing from other tests (at least 43.5 blocks
  from his centre), so exact counts and experience shares are asserted only for a fight begun and ended in one
  synchronous block. `LairEncounterGameTests.aKeptCinderTyrantRewardIsSavedAndPaid`: a kept `cinder_tyrant` Due
  survives a save and is paid by his payer.
- Unchanged, and passed in CI with B-01 (runs 38509414542, 38514144531 and 38519234934):
  `YetiKingGameTests.hisLootIsEachParticipantsOwn` (`:416`), the Tyrant's test of the same name
  (`CinderTyrantGameTests.java:673`), and the kill in `YetiKingClientGameTests` (`took` then `genericKill`,
  `:204-205`).

**Acceptance:** "Reward and return survive relevant lifecycle events" (09:110) for death, disconnect and a repeated fall;
exactly-once rewards (04:20); no reward for a late arrival who did nothing (04:170).

**Save effects:**
- New boss ids `yeti_king` and `cinder_tyrant` inside the existing `jugcraft:lair_rewards` attachment and world record,
  stable once released.
- Downgrade: an older build has no payer for them, takes the kept reward off the player and skips it
  (`LairRewards.java:184-189`).
- The bosses' own saved data does not change: `damageBy` was never saved, and `Encounter` is not saved by design
  (`Encounter.java:47-48`).

**Review boundary:** reward code and its tests. The only change in the fight is the column deed, which adds credit.

### B-02. Dossiers, the route step and JEI pages for the Glacier Hall and the Cinder Kiln

**What the player sees.**
- Carrying a Frost Horn shows the Glacier Hall's rite: all year, at night, standing on snow or ice, and the party size.
  A Kiln Seal shows the Kiln's: any hour, on magma in the Nether or a volcanic land of the Overworld
  (`cinder-kiln.md:14`).
- Meeting or beating a keeper shows how support earns a share, his loot and the recipe a win opens
  (`frost_horn_from_fur`, `kiln_seal_from_scales`).
- JEI has pages for Yeti Fur, the Yeti Mitten, the Yeti King's Crown, the Glacier Maul and the Rimeclaw, and the Tyrant's
  loot, with exact chances "with the default data" (A-D15).
- A first win over either keeper counts on the First Light route's lair step.

**Files:**

| File | Change |
|---|---|
| `lair/LairDossiers.java:44-52` | `YETI_KING` and `CINDER_TYRANT` entries |
| `lair/LairDossiers.java:58-59,163-169` | The trophy becomes one of a pair |
| `lair/LairDossiers.java:134-139` | The exhaustive `rite()` switch gains both cases and their lang keys |
| `lair/LairDossiers.java:151-153` | The season fact assumes the Halloween gate; an "all year" variant for rites that ignore it |
| `concordance/encyclopedia/FirstLightRoute.java:92-93,126-127,142` | `FIRST_WINS` removed: the lair step is done by `LairDossiers.anyBeaten` (every keeper's first-win advancement) and shown when `LairDossiers.canOpen` (any token's recipe) |
| `tools/encyclopedia.py` (dossiers; shared credit and route strings) | New entries; pronoun-neutral shared strings (B-P2) |
| `tools/acquisition.py:41-50,96-117` | `event` optional; a trophy pair as one pool ("a 15% chance of one of the two"), not 15% each; new rows |
| `tools/check_mod_data.py:9697-9733` (`check_acquisition`) | An explicit boss-to-loot-class map (today the path is built from the boss name, `:9698`, which finds no `lair/yeti_king/Yeti_kingLoot.java`); the Halloween chance optional (`:9725-9728`); a first-kill rule that accepts the `TROPHIES` table shape (`:9729-9733`); bosses without pages (the Mire Hag, the Crypt Lich) allowed |
| Generated | `lang/en_us.json`; `recipe_view.json` (`acquisition_info`) |
| Stale records | `concordance-first-loadout.md:117,269-270`; `lair-encounter-credit.md:167-170`; `BOSSES.md:25`; the loot sections of `yeti-king.md` and `cinder-tyrant.md` |

**Reuses:** `LairDossiers`, `PageWriter`, the `acquisition_info` pipeline, `check_encyclopedia`.

**Tests:**
- `EncyclopediaGameTests` (pattern `aBossIsHiddenUntilDiscovered`, `:351`): a horn carried shows the rite; a visit to the
  hall marks the Yeti met; Abominable marks him beaten; the after-win recipe is optional; the same for the Kiln.
- Route completion by `abominable` and by `tempered`.
- `theExpeditionsPageHoldsEveryKeeper`: with all four keepers beaten, every recipe grid is drawn and no row is dropped.
  The four need 12 grids (Vesperine 3 + 1 + 1, Madame Tatterlace 2 + 1, the Yeti 1 + 1, the Tyrant 1 + 1; the review's
  count from `LairDossiers.java:45-52` and the new entries), exactly `PageWriter.MAX_GRIDS`
  (`guide/encyclopedia/PageWriter.java:54`).
  Past it a grid keeps only its text, and rows past the page's maximum are dropped (`:48-49,129,256`). The test warns
  the next keeper; B-08's preparation fact is text and icons, with no grid (B-P29).
  Exact counts, every keeper found and beaten, with no town, kept share or visit: 71 rows (Vesperine 20, Madame
  Tatterlace 17, the Yeti 17, the Tyrant 17) and 12 grids; at worst about 79 rows (a town's three sales, a kept share
  for each keeper, one fight line) against the 95 shown before the "more" row (`EncyclopediaPayload.Page.MAX_ROWS` 96).
  The test asserts the grids exactly and that nothing is dropped, not a row count, which B-P29's fact would move.
  `check_encyclopedia` counts the grids statically and refuses a table past `MAX_GRIDS`. A fifth keeper needs
  `MAX_GRIDS` and `MAX_ROWS` raised as a recorded bound change, not a page per lair (`MAX_PAGES` is 6,
  `EncyclopediaPayload.java:27`, with four taken). After the Glacier Hall's commit, before the Kiln's: 54 rows and 10
  grids.
- `check_mod_data.py` mutation checks, as in Chunk A.

**As built (the Glacier Hall's commit; passed in CI runs 38509414542, 38514144531 and 38519234934):**
`EncyclopediaGameTests.theGlacierHallsDossier`, `eachRiteSaysWhenItWorks`, `aWinOverAnyKeeperIsTheLairStep` and
`theExpeditionsPageHoldsEveryKeeper`; `GlacierHallGameTests.theHornIsNoHalloweenRite`;
`YetiKingGameTests.hisLootIsWhatHisPagesSay` (400 shares, each count within five standard deviations of its page's
chance; never both trophies). Run here: `check_mod_data.py`, `check_repository.py` and `concordance_journey.py` pass; on
a scratch copy, 30 faults against `check_acquisition` and 8 against `check_encyclopedia` (the Yeti's trophies in the
tool, his mitten moved into another keeper's entry, his rite made a Halloween one, the grid bound lowered to 9, his rite
recipe needing his fur, a missing lang key, his snow sale dropped from the entry, the Hollow Acre's season words
dropped) were each reported, and #336's Mire Hag table with no pages was not. Adding `white_wool_from_yeti_fur` to his
`made_from` passes at this commit (11 grids of 12); with the Kiln's two it would be 13, which the audit refuses.

**As built (the Cinder Kiln's commit; passed in the same three runs):** `EncyclopediaGameTests.theCinderKilnsDossier`,
the Kiln Seal and the Cinder Kiln added to `eachRiteSaysWhenItWorks`; `CinderKilnGameTests.theSealIsNoHalloweenRite`;
`CinderTyrantGameTests.hisLootIsWhatHisPagesSay` (400 shares, as the Yeti King's). `aWinOverAnyKeeperIsTheLairStep` and
`theExpeditionsPageHoldsEveryKeeper` read `LairDossiers.Boss.values()`, so they now take in the Tyrant: four first wins,
and 12 grids of 12 in 71 rows (79 at most). Run here: `check_mod_data.py`, `check_repository.py` and
`concordance_journey.py` pass. On a scratch copy, 13 faults were each reported: 5 against `check_acquisition` (his row
naming an objective; a deed credited in `lair/tyrant`; his trophies split into two pools; the Salamander Charm's chance
moved; his payer dropped) and 8 against `check_encyclopedia` (one trophy in the tool; the Salamander Charm dropped from
his Java entry; his rite made a Halloween one in the tool; `KilnSealRite` reading `Lairs.offSeason()`;
`magma_cream_from_tyrant_scale` added to his `made_from`, 13 grids; his lair's leave words missing; his Java entry
removed; a sale named in it).

**Acceptance:** B4's encyclopedia guidance (09:106); closes Chunk A's deferral (`concordance-chunk-a.md:157-160`).

**Save effects:** none; the encyclopedia keeps no saved state (A-D9).

**Review boundary:** pages, tools and checks. #352 keeps #337's encyclopedia as the live one, so this ticket stands
whichever merges.

### B-03. Glacier Hall access and lifecycle (plan B1)

**What the player sees.** Nothing new if all works; otherwise the defects found are fixed.

- **Designed world** (undetermined today): a client test exports a design, creates a world from the exported preset and
  checks that `jugcraft:glacier_hall` and `jugcraft:cinder_kiln` exist and a horn opens a slot (07:13, 07:147-148). If the
  datapack dimensions are missing, the fix belongs in the designer's export, not in the lair.
- **Horn rules:** the refusals are listed in `glacier-hall.md:17-22`; `GlacierHallGameTests` covers night and snow
  (`:113`) and a full slot (`:141`). Add any missing case, such as Peaceful.
- **Restart:** instances clear when the server starts and stops (`Lairs.java:88-89`). Extend step 7 of
  `LairClientGameTests` (`:253`, which already calls `Lairs.reset()`) to the Glacier Hall instead of writing a second
  restart test. With B-01, a Due kept before the restart is paid after it (07:155). This is not "persistent lairs" (04:9).

**As built (unit 2: `98c550f16`, with `0e7de6bf1`'s wait before the designed world's horn; not merged).** Tests only;
no product code changed and no defect was found. All passed in CI run 38543250503 on `1266c905a` (see the Validation
plan); the server tests are required and registered, so each passed as one of the 1658.
- **Designed world:** `WorldDesignerClientGameTests` creates its world through the create-world screen with the
  designer's generator in place of the Overworld's, the shape of a designed world. There, and again after reopening it,
  every lair's dimension exists, the Frost Horn blown on placed snow opens a Glacier Hall and takes the player in, the
  Kiln Seal pressed into the Nether's magma opens a Cinder Kiln, and closing each sends the player back to where they
  stood. `tools/world-designer/model.test.cjs` adds that the exported preset names only the three vanilla dimensions and
  writes no dimension files, so the lairs come from the installed mod as in any world (the game adds data-pack
  dimensions at creation and at load: mc263src `CreateWorldScreen.java:249`, `WorldOpenFlows.java:165-166`). No fix was
  needed in the export.
- **Deliberate deviation:** no world is made from an actual exported ZIP. The exporter is JavaScript, and adding a
  user's data pack while creating a world is private to the game and opens the experimental-settings screen (the unit's
  review). The Node test and the designed-shape world cover it; a world from an exported ZIP stays a check by hand
  (07:150).
- **Horn rules:** `GlacierHallGameTests.everyRefusalKeepsTheHorn` (by day, with the lairs switched off, and through the
  item off the snow, which still rests it; nothing opens, moves or frosts) and
  `theHornIsAnsweredOnAnyOfItsGroundAndOnPeaceful` (at night on every block of `#jugcraft:frost_horn_ground`, and on
  Peaceful, where the hall opens empty: B-P37). `GlacierHallClientGameTests` adds a horn blown in the hall (refused and
  kept); straying sideways past the hall's bounds, under Resistance IV and a Brace, for the mist's whole toll, as falling
  does; the arch's Grey Mist used as a click uses it; forty ticks in the whirl, which take nobody in; by day through the
  item and the world's clock; every instance taken; a whirl into a full party, which takes nobody in and frosts nobody;
  and the whirl used as a click uses it, back into the same hall.
- **Restart:** `LairClientGameTests` step 7b: a Glacier Hall closed by `Lairs.reset()` (a restart simulated, as step 7's)
  with its King asleep; he goes with it, the player inside is sent home, and a `yeti_king` reward kept from before is
  not paid in the closed hall but at home, once. A new step 9 is a real restart: the world is saved with the player
  inside a Glacier Hall and a reward kept in the world's record, and opened again. The game's own server stops and
  starts (Fabric's `SERVER_STOPPED` and `SERVER_STARTED`, which clear the instances, `Lairs.java:88-89`), and on the
  real join the player is home first and then paid once. No disconnect or leave event is checked, and there is no second
  client.

**Reuses:** `Lairs`, `LairVisit`, `MistGateEntity`, `GlacierHallGameTests`, `GlacierHallClientGameTests`,
`LairClientGameTests`, `WorldDesignerClientGameTests`.

**Save effects:** none.

**Review boundary:** tests, and fixes only where a test fails. The hall's structure, blocks and recipe stay (04:118;
09:90).

### B-04. The Yeti fight held to the completion evidence (plan B3)

- **Edge determinism** (09:102). The charge moves by a fixed step and stops at leash − 1, 29 blocks from the centre
  (`YetiKingEntity.java:1006-1011`); a column stuns when the next step comes within 5.2 of one (`:1001-1003`). Server
  tests, each giving the same result every run: a charge that grazes a column; a charge toward the dais or a den wall; a
  charge that starts at the leash; a chained charge in his fury. The dais and wall geometry live in
  `tools/glacier_hall.py`, not yet read.
- **Safe footing** (09:110): trampled routes survive the slam, the fury ring, the drift back, `driftAll` on reset and on
  death, and the drift on load (`bare` at `:1133`, `drift` at `:1168`, `driftAll` at `:1182-1187`).
- **Cleanup** (09:110): whelps, boulders, icicles and spikes are not saved (`lair/yeti/JugcraftYeti.java:46-54`). Test
  reset, death and an unload mid-fight: all gone, and the glare ice drifted back.
- **Frost facts:** knitwear and leather against `chill()`: freeze damage stopped or not, slowdown present or not. This
  turns the inference in the status delta into evidence for B-08 and B-10.
- **Late joiners:** a test that a player arriving mid-fight joins the boss bar (`:1457-1460`). The elapsed time stays
  unsynced (Deferrals).
- **Levitation:** if #332 or #352 merges first, End-gun Levitation does not lift the king or his whelps.

**As built (unit 2: `dca611f85`, `0e7de6bf1`, and three fixes; not merged).** Seventeen new server tests in
`YetiKingGameTests` and four new steps in `YetiKingClientGameTests`, all passed in CI run 38543250503 on `1266c905a`.
Every server test is required and registered, so each passed as one of the 1658. The first run, 38534127130 on
`0e7de6bf1`, failed one server test and the client test's away-and-back step (the Validation plan); each failure was a
fault in the fight, fixed:
- **Fixes:** a glancing charge that met a column's flared foot slid round it unstunned; now a step that would bring his
  body against the foot stuns him as a straight run does (`215a1c1e0`, D1; B-P38). His blocks of ice, icicles, spikes
  and whelps left in chunks that stopped without unloading came back with a player who returned mid-fight; now each lasts
  only while he counts it his (`YetiKingEntity.owning`), and his count is not saved (`1266c905a`). End-gun Levitation
  lifted his whelps, and him as he died; neither he nor they take any Levitation now (`1dec9f9ef`; #352 carried #332's
  float).
- **Edge determinism** (in the test arena of air the hall's size; the tests at the dais, the ramp, the dens and a
  column's foot place the hall's own template over it): `aGrazingChargeStunsHimOnlyWithinAColumnsReach` (passing 5.6
  from a column's centre he runs his full line, the same both runs; 4.8 from it he stops stunned),
  `aChargeThatMeetsAColumnSouthOfItStunsHim` (D1), `aChargeThatClearsAColumnsFootRunsOnUnstunned`,
  `inTheHallHisChargeStopsAtTheDaisTheRampAndTheDensWall` (he never climbs and is never inside a block),
  `aChargeEndsAtHisLeashTheSameEveryRun`, `inHisFuryAMissedChargeIsFollowedByExactlyOneMore` and
  `inHisFuryABaitLeadsTheChainedChargeIntoAColumn` (a stun never chains).
- **Solo melee, solo caster and a party:** `soloMeleeBaitsAChargeIntoAColumnAndStrikesHimReeling` (a third more before
  armour, `Benchmark.afterArmour`), `soloCasterBaitsAChargeIntoAColumnAndLancesHimReeling` (the Lance, cast through
  Spell Engine, bypasses armour) and `aPartyOfTwoSolvesTheColumnLoop` (the bait side-steps, the charge tramples the
  caster on its line once, both are credited). Test players, not people.
- **Footing and cleanup:** `leftAloneAfterHisSlamHisLakeIsAsItWas`, `leftAloneHisIceIsGoneAtOnce`,
  `whenHeFallsHisSnowDriftsBackAndHisKinAndIceAreGone` (his slams bare only drift snow, beside trampled routes; on a
  reset and as he falls the lake is exactly as laid, and his kin and ice are gone),
  `loadedAgainMidFightHeDriftsHisSnowBackAndBringsNoKin` (an emulated unload: only he is written; loaded, he hunts on
  and the snow drifts back) and `loadedAgainHeCountsNoKinOrIceFromBefore` (his kin and ice from before, in chunks that
  stayed, are gone on their next tick). The client test adds the real thing: his lake snapshot as placed, each slam
  changing only drift snow to glare ice, and away and back mid-fight (the player leaves, the hall's chunks unload with
  him in his fury, and coming back the player finds him loaded again, still furious, his bar shown again, the snow
  drifted back and none of his kin or ice); as he falls the whole lake is as placed.
- **Frost:** `woolAndLeatherStopTheFreezeButNotHisFrost` (the status delta).
- **Levitation:** `endGunLevitationLiftsNeitherHimNorHisKin` (the spell refused by him, his whelps and him as an End
  gun's shot fells him; none rises).
- **Late joiners:** one client leaving and returning, above. A second client, a dedicated server and a real disconnect
  are not run.

**Acceptance:** 09:108-110. "Solo melee, solo caster and a mixed party" is automated: one or more test players in a
server game test, the caster's spell cast through Spell Engine. A party of people and two clients stay not run (09:20).

**Save effects:** none. A fix that changes his saved data needs a versioned read. As built, none of the three fixes
changes what Jugcraft saves with him (his instance, slot, origin, phase, fury and bared cells); his count of his whelps
and things was never saved.

**Review boundary:** the Yeti and the hall. No tuning change without a failing test.

### B-05. Rime and Strata research and evidence (plan B2, part 1)

Built with B-06 and B-07 as unit 3 ([record](arcane-concordance-rime-strata.md)): a research entry may unlock only
invocations that exist, and each invocation must be unlocked by its research (`check_invocations`). B-05 to B-07 were
rewritten on 14 October 2026 to what the unit built, on main `feb6d04fb`; the choices that changed are in the revised
B-P12, B-P15 and B-P16, the new B-P30 to B-P35, and the record's "Changes from the plan".

**What the player sees.** Two research entries with typed evidence, each piece counted once by its key (02:129, 02:153),
and the next step named in the journal and the codex.

| Research | Tradition, stage | Encountered | Observed | Understood (and what it unlocks) | Mastered |
|---|---|---|---|---|---|
| `jugcraft:rimekeeping` (Rime) | Rimekeepers, practitioner; requires First Light understood | Examine an item in the new `#jugcraft:rime_specimens` (snowball, snow block, ice, packed ice, blue ice) | Examine something in `#jugcraft:ember_specimens`, something that holds fire (02:132's warm surface); either may come first | Any of: study snow or ice at the Lampwright's Bench; notes; the **winter survey** `jugcraft:winter_survey`, gated by First Light alone so either route advances understanding (02:136): `glacier_hall`, arriving in the Glacier Hall by any route (Fabric's `AFTER_PLAYER_CHANGE_LEVEL`; no lair code), or `crucible`, a dose carrying Rime bottled by hand (`CrucibleBlockEntity.bottle`). Unlocks Rimebind and Preserve | `jugcraft:rimework`, all three kinds: `preserve:cold` and `preserve:heated` (Preserve on a crucible holding a mixture, in the cold band or above it: 02:133-134's preserve and compare), `restraint` (a Rimebound taking on a hostile creature at normal tolerance, 02:135). The Yeti column elective (02:137) came with B-09 (unit 5), as the key `elective` it shares with unit 4's Frost boon slow: one kind between them, so a Preserve key is always needed (B-P56) |
| `jugcraft:stratawrighting` (Strata) | Stratawrights, practitioner; requires First Light understood | Examine an item in the new `#jugcraft:strata_specimens` (granite, diorite, andesite, tuff, raw Thallite; 02:140) | Three distinct | Study at the bench; notes. Unlocks Brace | `jugcraft:anchoring`, three of: `ground:living` and `ground:earthen` (a Brace taking on someone standing on that ground, read with Thallite's ground tags; 02:141-142), `held` (a Brace the caster put on meets a push); and the electives `ally` (a party member braced), `foundation` (a ritual completed at a Circle Anchor, 02:143) and `rooted` (struck in Earthbound Thallite on natural ground, B-P26) |

**Reconciled with `assay`** (09:13; revised B-P15):
- `#jugcraft:assay_specimens` holds `#jugcraft:strata_specimens`, so one examination serves Assay and Stratawrighting
  (the research engine applies each piece of evidence against every entry's rules); a test confirms it. Assay becomes
  easier to begin; its mastery is unchanged.
- Nothing new reaches the Assayer's Scale, which weighs against the equivalence catalogue: stone and raw Thallite are
  examined, never weighed.
- Cobblestone, stone, cobbled deepslate and deepslate are never specimens: sneak-use in the air examines, and players
  bridge with them (`check_strata`). Nor is the Thallite ingot.
- Strata can be completed with the `thallite` feature off: the four vanilla stones suffice.

**Progression** (revised by the unit review): the Glacier Hall route understands Rimekeeping without a study (02:136),
but Rimekeeping's mastery needs the Alembic Arts (Preserve counts a crucible holding a mixture, and only an alembist
fills one by hand), which are understood at the bench or from notes. The graph's `rimework` route asks for them, so the
bench stays mandatory at Adept; there is no bench-free Adept.

**Not in this ticket:** Strata's civic-project or structural-boss-opening step (02:144), see Deferrals; the Yeti column
elective (02:137), built with B-09 in unit 5 (B-P56).

**Files:** new generators `tools/concordance_rime.py` and `tools/concordance_strata.py`, merged in
`tools/concordance.py`; `tools/concordance_progression.py` (the three practices, the winter survey's two routes in
`graph()`, sources for snow, ice, stone, goat horns, raw Thallite and Yeti Fur); `tools/concordance_conclave.py`
(`TRADITIONS`); `tools/concordance_equivalence.py` (Assay's texts); Java `concordance/rime/Rime.java`, `RimeSurvey.java`
and `concordance/strata/Strata.java`, registered beside `Ember.register()`; `Examination`'s messages; the guide block,
the journey routes and the mandatory table, regenerated; `check_rime` and `check_strata`, mutation-tested.

**Tests:** `ConcordanceRimeStrataGameTests`; `ConcordanceProgressionGameTests` (the live graph, First Light alone gating
winter and stone, one examination serving both entries, an Adept by the Glacier Hall);
`ThalliteGameTests.thalliteIsAStrataSpecimen` (09:112); `GlacierHallClientGameTests` (arriving is the field study).

**Save effects:** new research ids, evidence keys `practice:jugcraft:{winter_survey,rimework,anchoring}:<key>` in the
existing knowledge record, and advancements `concordance_<research>_<state>`, stable once released.
`rimeAndStrataResearchSurviveASave` round-trips them; an older build keeps the unknown entries and relearns nothing from
them.

### B-06. Rimebind and Brace (plan B2, part 2)

**Budget.** Ten invocations before and thirteen after, against a cap of 16 (an instrument's `Tunings.MAX`,
`check_invocations`); signatures unique; the six roles still covered. Roles (B-P33): Rimebind **defense**
(`Benchmark.choose` never casts a utility), Brace **support**.
- **Rimebind:** `ray struck rimegrip`, signature `ray/struck:status/harmful`; work 2 (the ray 1, a status on one target
  1); persists 100 (the binding and its thaw).
- **Brace:** `here allies bracing`, signature `here/allies:status/helpful`. `allies` is the caster and their party only
  (`concordance/ComposedSpells.java`), so the party is the consent (B-P7). Work 4; persists 100.
- Both are authored: no word a player composes is added (B-P6).

**Rimebind** (B-P9, B-P10, B-P34):
- `jugcraft:rimebound` slows by 20% a level for 60 ticks through an attribute modifier on the effect,
  `jugcraft:rimebound` (the pattern of `agriculture/Hexes.java`).
- **Grace:** every Rimebound that takes starts a hidden `jugcraft:rime_thaw` for its own time plus 40 ticks
  (`Rimebind.bound`, from the boundary's STATUS branch), and a `ConcordanceEffects` Guard (the pattern of
  `concordance/sympathy/Sympathy.java`) refuses Rimebound while it lasts: IMMUNE, nothing spent, the cast fizzles at no
  cost. At most 3 s bound in every 5, shared by every caster. The grace covers Rimebind's slow only (B-P10).
- **Targets** (B-P34): the shared boundary's (monsters; creatures the caster could strike by hand; players only under
  PvP and outside the party), not the plan's "hostile ordinary creature" only. Only a hostile creature (`Enemy`) at
  normal tolerance counts as restraint.
- **Tolerance** (B-P8): only the Yeti King and the Cinder Tyrant join `#jugcraft:concordance/resistant` (beside the iron
  golem, ravager and piglin brute). That halves every harmful Concordance status and push on them, not only Rimebind's:
  Rimebound lasts 30 ticks and its thaw 70; Smoulder on the Yeti (the Tyrant is immune to fire), Dazzle and the Ice
  Breaker wave's slow (`concordance/trinket/Wayfaring.java`) are halved; pushes already did nothing (knockback
  resistance 1.0). The Ember, trinket and keepers' records say so, and `onlyTheYetiAndTheTyrantResist` covers it.
  Vesperine, Madame Tatterlace, the Mire Hag and the Crypt Lich are unchanged (`check_rime`).
- The Yeti moves by fixed steps, not by his movement-speed attribute (`YetiKingEntity.move`; the charge), so a slow
  alone does nothing to him (an inference until tested); B-09 gives him a visible change instead of a silent one
  (04:188). *Since unit 5:* a binding holds his next wind-up 4 ticks (B-P54), and `aRimeboundChargeKeepsItsPace` tests
  that the slow leaves his charge's pace alone (passed in CI, run 38591108210).

**Brace** (B-P11, B-P12, B-P30): `jugcraft:braced` for 100 ticks, +0.5 knockback resistance (the modifier
`jugcraft:braced`), exclusive. It is spent by the first push it meets, braces the rest of that server tick and goes at
its end (`ServerTickEvents.END_SERVER_TICK`); spent between ticks, by a push in a cast's packet, it goes when the next
tick starts (`START_SERVER_TICK`). Vanilla's knockback respects knockback resistance (mc263src
`LivingEntity.java:1543-1544`).
- **A blow vanilla knocks back** spends it (`ServerLivingEntityEvents.AFTER_DAMAGE`: full damage this tick, not
  `#minecraft:no_knockback`, not wholly stopped by a shield). Vanilla's knockback inside `hurtServer` and the extra
  knockback of sprints, the Knockback enchantment and mob attacks that follows it (mc263src `LivingEntity.java:1205`)
  fall in the same tick, so the whole blow is braced (B-P30).
- **A harmful Concordance push** reads the resistance with the Brace and spends it. **A helpful one** (one's own
  Flashstep, a friend's helpful push) reads it without (`Brace.resistanceWithout`: the effect's own modifiers off for
  the reading and back at once) and leaves it (B-P12).
- **A scripted push** asks through `Brace.spend(target)`, which spends it and returns the share to let through, 1 − 0.5
  a level, and is safe to call more than once a blow. B-09's Yeti calls it inside his `push`, before or after
  `hurtServer`: the order no longer matters.
- **The Leaf Blower's stream** (`agriculture/LeafBlowerItem.java`), which reaches another player only under PvP, spends
  it with `Brace.spend` before it reads knockback resistance.
- **Accepted exception** (B-P30): vanilla's knockback that comes without a damaging blow (the review names a mace
  smash's bystanders) reads the Brace's resistance but does not spend it.
- Teleports are not pushes: `Lairs.edges`, the Grey Mist, portals and the whirl move the braced as anyone.
- Who put it on is kept in a non-saved attachment, `jugcraft:brace_caster` (`Brace.caster`, for B-09's deed). The
  caster's practice: the target's ground (`ground:living`, `ground:earthen`), an ally braced, and `held` when it is
  spent.
- Its texts keep it apart from the war fork's BRACE trait and the tower shield's brace (`weapons/JugcraftArms.java`);
  "Stonebrace" is offered at the checkpoint as a display name.

**Costs** (hypotheses, 02:73): Rimebind 4 Focus (3 mastered), 2 s cooldown; Brace 3 (2), 8 s; on the scale of today's 3
to 7, settled once when Spell Engine consumes the cast's cost (`concordance/ConcordanceSpells.java`), however many it
braces.

**Benchmark** (B-P32). `Benchmark` models Rimebound (a fifth of a foe's steps a level for the binding, then the thaw
refusing Rimebind; every other harmful status keeps the 15% rule), starts no cast that would change nothing (a status
already held by its stacking rule; Rimebind on a thawing foe), casts a defense with a harmful step at any foe in reach,
and measures knockback taken without moving anyone (`Baselines`' "Knockback (blocks/min)" column). New acceptance
checks: 5, Rimebind's control at most 7.8 foe-seconds a minute; 6, Brace takes knockback off beside the unkillable
brute. The baselines record holds CI's tables (run 38519234934): Rimebind alone 6.6 foe-seconds a minute, Brace alone
5.2 blocks of knockback.

**Assets** (B-P31). The owner's files byte for byte, at their own sizes: Rimebind
`ars_jimbaux/textures/item/freeze.png`; Rimebound `ars_jimbaux/textures/mob_effect/freezing.png`; the Thaw
`ars_nouveau/textures/mob_effect/soaked.png`; Brace and Braced `ars_jimbaux/textures/item/gravity.png` and
`mob_effect/gravity.png` (the owner's own pair, byte-identical). Spell Engine draws a spell icon into 16×16 from the
whole texture, so nothing is scaled. Brace's release is the owner's `gaia_1.ogg` to `gaia_4.ogg`; Rimebind's release and
the gathering cues are synthesised, as Ember's were. The earlier proposals (`shield.png`, `shielding.png`, `frozen.png`,
scaled to 32) are checkpoint alternatives: shielding reads as "reduces damage", which Brace never does, and frozen as
"cannot heal".

**Tests:**
- `ConcordanceEffectGameTests` (09:112): `rimebindThawsBeforeItBindsAgain`; `onlyTheYetiAndTheTyrantResist`;
  `braceHoldsTheWholeBlow`; `braceIsTakenBeforeTheBlow`; `harmfulPushesSpendTheBraceAndHelpfulOnesDoNot`;
  `rimeAndBraceSurviveASave`.
- `repeatedSlowDoesNotPermanentlyImmobilize` (09:96), scoped to Rimebind (B-P10): three casters recasting on one target
  every tick for 15 s; no bound run longer than 3 s, no gap shorter than 2 s. The trophies' Frost boon, `dazzle`, DAZE
  and the Ice Breaker wave stay outside it.
- `braceDoesNotCancelBoundaryEnforcement` (09:96): the Grey Mist sends a braced player home and the Brace is untouched;
  in `GlacierHallClientGameTests`, `Lairs.edges` returns a braced player past the bounds without spending it, and
  leaving and following a whirl still move them.
- `ConcordanceInvocationGameTests`: `focusIsChargedOnce` (09:96) and `braceDoesNotShortenFlashstep`.
- `ConcordanceRimeStrataClientGameTests`: the icons in the client's resources; spell bar, HUD and inventory screenshots.

**Save effects:** new mob-effect, attribute-modifier, component, invocation and spell ids, stable once released. The
three effects save in `active_effects`, which vanilla puts back without re-adding their modifiers; those come back from
the saved `attributes` (mc263src `LivingEntity.java:780-786`; `rimeAndBraceSurviveASave` checks it). `brace_caster` is
not saved. An older build can drop all of a creature's effects on meeting an unknown one and keep their modifiers (a
fifth off speed, +0.5 knockback resistance) until `/attribute ... modifier remove` takes them off (the record's
downgrade guidance).

### B-07. Preserve (plan B2, part 3)

- **Shape:** `touch struck rimehold`, a helpful alteration; signature `touch/struck:alteration/helpful` (Hearthspark
  holds `ray/struck:alteration/helpful`). `touch struck` reaches a block. Role **utility** (B-P33); 3 Focus (2
  mastered), 0.6 s cast, 3 s cooldown, no tunings; work 4, persists 1200.
- **Dispatch:** on Rime's school, `spell_power:frost`, as Ember's alteration is on fire: `Rime.preserve` in the
  boundary's block path, before the default alteration (which removes fire, and only fire), and `Rime.preservable` in
  `accepts`. `check_rime` holds the order and that only the boundary calls `Rime.preserve`.
- **Target** (B-P13): `#jugcraft:concordance/preservable`, holding only the Alembic Crucible. Preserve holds its
  temperature band for 60 s: the heat still moves a degree a tick toward its source, kept inside the band (`Band.keep`),
  whatever burns or goes out beneath. That is Rime's reliable workstation loop (02:54).
- **The hold** (B-P35): `Rime.HOLD_TICKS = 1200`, a Java constant (an alteration takes no duration, so Spell Power does
  not scale it). A recast restarts it and never adds. It runs on the world clock (`held_until`, game time). The crucible
  accepts holds up to `Rime.MAX_HOLD_TICKS` (2400, `EffectSpec.MAX_DURATION`) and cuts anything further ahead, so
  B-P24's water foci need one line in `Rime.preserve`.
- **Refused:** anything else returns `Result.NOTHING` and changes nothing: chests, barrels, hoppers, furnaces, machines
  and every other inventory (02:77; 06:96; 09:96). A crucible the caster may not change returns `NOT_ALLOWED`
  (`Authority.mayChange`: claims, protected towns, spawn protection, adventure mode, an absent caster).
- **Practice:** a Preserve on a crucible holding a mixture is Rimekeeping's cold work, `preserve:cold` or
  `preserve:heated` by its band.
- **Guidance:** Jade's crucible lines and the crucible's empty-hand reading show the seconds held (moved here from
  B-08).
- **No Thallite Focus and no crafted upgrade here** (B-P14). With the Initiate's Wand's limits and no upgrade a Thallite
  Focus would add nothing; the approved record pairs it with Earthen Steps and Bulwark in slice 5 (`thallite.md`). The
  chunk's crafted upgrade from existing materials (09:94) is unit 4's water foci (B-10, B-P24). `thallite.md` records
  this.

**Tests:** `preservationCannotWrapArbitraryInventories` (09:96); `preserveHoldsOneBandAndExpires` (with Jade's seconds
when Jade is loaded); `preserveCountsTheCrucibleBand`; `preserveIsChargedOnceAndCountsOnce`; in
`ConcordanceEffectGameTests`, `preserveNeverRemovesABlock` and `preserveIsRefusedWhereItsCasterMayNotBuild` (renamed
from `preserveIsRefusedUnderTownProtection`: it covers a neighbour's claim, an absent caster and adventure mode).

**Save effects:** the crucible's optional `held_until` (game time), written only while held and read with a default of
0; it still stamps version 1. `ConcordancePersistenceGameTests.theCrucibleKeepsPreservesHoldThroughASave`: a round trip
keeps the hold; a save without the field, or also without the version, reads 0 and keeps the temperature; an ended hold
writes nothing; a forged hold far ahead is cut to 2400 ticks. #323's crucible change reached main with #352, and unit 3
is built on it.

**Review boundary (unit 3):** magic data, effects, the crucible's hold, Jade and tests; no lair code (the hall is heard
through Fabric's level-change event).

### B-08. Winter Brine and Frostward at the Alembic Crucible (plan B4; B-D3)

Built as unit 4 with B-10 ([record](arcane-concordance-winter.md)), on main `feb6d04fb` with unit 1 and unit 3 beneath.
This ticket was rewritten on 14 October 2026 to what the unit built; its new choices are B-P40 to B-P45.

The winter workshop is the existing Alembic Crucible: Rime's brew and Preserve's hold are its loop.

- **Frostward,** a new status `jugcraft:frostward` (`concordance/rime/Frostward`, helpful, the owner's `frozen.png`,
  B-P43). Its own effect tick clears frost every tick on whatever creature carries it (the Mitten's check, done by the
  creature under it; B-P40), so powder snow, the Frost Horn's arrival chill and a snow werewolf's frostbite chill leave
  none; never Slowness, never sinking. The Yeti's frost does not land (`YetiKingEntity.chill` checks
  `Frostward.warded` beside the Mitten, in one hunk with a fully qualified name). It is timed (B-P19: 1200 ticks a
  unit, at most 2400, as Ember's Fire Resistance brew). The Mitten, which works for as long as it is held, stays the
  permanent prize (04:141).
- **From the generator,** not hand-edited data: `PROPERTIES["rime"]` (`tools/concordance_alchemy.py`) gives
  `jugcraft:frostward` (`FROSTWARD`), and `property/rime.json` is regenerated. Every Rime brew (snowball, amethyst,
  gloamcap) gives Frostward; no brew gives Resistance any more (B-P42).
- **The heat rule** ("Too much heat consumes the temporary cold property", 03:36; B-P18): after each stir the dissolved
  Rime is scaled by `Mixture.rimeKeep(band)` (cold all, warm 600, hot 300, searing 0 per thousand, `RIME_KEEP_*`),
  mirrored in `tools/concordance_alchemy.py` (`RIME_KEEP`, `simulate`, `winter_brine`) and held by `check_alchemy`'s
  constant table and `check_winter_brine`. It acts on stirs only: an addition records no band (the formula format is
  unchanged), so a ground ingredient keeps its ready share until stirred (B-P41). It also changes amethyst and
  gloamcap brews stirred warm; `arcane-concordance-alchemy.md` says so. `Outcome` names the warmer stirs
  (`alchemy.thawed`).
- **Winter Brine** names the cold-stirred Rime brew in JEI (the snowball's, amethyst shard's and gloamcap's pages: a
  property fact, B-P44), the codex (`alembic/winter_brine`, at the Alembic Arts observed) and the dossier. Three
  snowballs in one part, stirred cold three times, hold 0.813 Rime a part: Frostward I for 975 ticks. It needs only the
  Alembic Arts, not Rimekeeping (B-P44), and no salt (B-P17).
- **Bottles already made** keep the status they were bottled with (`concordance/Brew`); a written formula replays with
  the new rule and effect. Knitwear is unchanged.

**Guidance:** JEI's alchemy ingredient pages name Frostward; Jade's crucible lines show Preserve's hold (done by unit 3,
B-07); the Glacier Hall dossier's rite gains a preparation fact naming Winter Brine and the Mitten, text and icons with
no grid, and a codex link at the Alembic Arts observed; with the Concordance off, or a data pack whose Rime no longer
brews Frostward, the Mitten only (`LairDossiers.prepare`, B-P29, B-P45; made on unit 1's dossiers).

**Tests:** `ConcordanceAlchemyGameTests.winterBrineNeedsTheColdBand` (stirred cold it gives Frostward, stirred warm it
does nothing, searing gives Nausea; the kept Rime is the generator's), `oldRimeBottlesStillGiveResistance` (first
named `aRimeBottleFromBeforeStillGivesResistance`), `aRimeFormulaWrittenBeforeBrewsFrostward` and
`frostwardIsNeverCheaperThanItsSnow` (must-fail economy loops, 06:146-158: Yeti Fur reaches no Rime ingredient, though
fur already cheapens the horn, `recipe/frost_horn_from_fur.json`; dilution and two draughts give no more Frostward);
`ConcordanceWinterGameTests.frostwardClearsFrostAndKeepsItsTime`; `YetiKingGameTests.aFrostwardDrinkerIsNotChilledByTheYeti`;
`EncyclopediaGameTests.theGlacierHallsRiteNamesWinterBrine`; `ConcordanceWinterClientGameTests` (the icon, a HUD
screenshot). No existing alchemy test expected Resistance from Rime, so none changed. All passed in CI run 38549876709
(not merged).

**Acceptance:** "one useful cold preparation route and a non-boss resistance option" (09:106); "Every required cold
preparation has a non-boss route" (09:110).

**Save effects:** a new effect id, saved in `active_effects`; the property change reaches only new bottles; one more
distinct Alembic practice outcome (`jugcraft:frostward@0`). An older build ignores Frostward draughts and may drop a
creature's saved effects on meeting the unknown id (the record's downgrade guidance).

**Review boundary (unit 4, with B-10):** alchemy, Frostward, the dossier's fact and tests. In lair code, only
`chill()` checking Frostward and `LairDossiers.prepare`, which only reads.

### B-09. Optional school counters in the Yeti fight (plan B3; only after B-04 is green, 09:102)

- **Brace** (B-P20, B-P12, B-P30): `Brace.spend` inside `YetiKingEntity.push` (`:829-832`), before or after the blow's
  `hurtServer` (a spent Brace braces the rest of that tick), halves that blow's push, every push through it, the spike's
  lift included. Other knockback resistance stays
  ignored, so netherite, Rooted, shields and the Ice Breaker change nothing: this is the plan's "bracable knockback"
  (04:186).
- **The Brace caster's deed** (B-P22; 02:187; 04:139, 04:156): when a Brace that one player put on another absorbs his
  push, the caster earns `encounter.deed(caster, "brace " + braced player + " " + game time)`, one key per occurrence.
  The caster is `Brace.caster`, kept in the non-saved attachment `jugcraft:brace_caster` (unit 3), so a Brace carried
  through a relog keeps its time but credits no one.
  A self-Brace earns nothing, as healing oneself does. Lair support credit sees only restoration and protection
  (`ConcordanceEffects.java:223-228`), so without this deed a Brace caster could never qualify.
- **Rimebind transforms, and is never silently dropped** (B-P21; 04:188): once per thaw window, a rimebound Yeti's next
  wind-up is a few ticks longer. The number is set by test and called untuned until people play it (04:194).
- **The column elective** (02:137; B-P16): a Yeti charge broken on a column becomes Rimekeeping evidence in
  `jugcraft:rimework`, an elective, with its recorder and its codex line, added here rather than in unit 3 because it
  needs B-01's deed and lair code. This ticket first named the key `column`; as built (unit 5, B-P56) it records the
  key `elective` that a Frost boon arm's slow records too, so the two electives stand for one kind between them and
  every mastery still needs a Preserve, so the Alembic Arts.
- **Not here:** Rime's "preserved snow" footing (authored anchors only, 04:101; the trampled routes already do this job);
  Ember and Tempest counters.

**Tests** (`YetiKingGameTests`): a braced player is pushed less, exactly once; the caster qualifies by it and a
self-braced player does not; a rimebound Yeti telegraphs longer; with no counters the B-04 suite is unchanged.

**As built (unit 5):** `25be6e227` (the counters) and `9cb934ec7` (the column), with the records; not merged; its
tests pass in CI (run 38591108210). It refines B-P16, B-P21 and B-P22 by B-P54 to B-P56, and builds
B-P20 and B-P57.
- **Brace:** `YetiKingEntity.push` is now an instance method, every push of his through it: his swipe's, his slam's,
  his charge's and the spike's lift (`GlacialSpikeEntity` calls `king.push`). It multiplies the push by
  `Brace.spend(player)`, which spends the Brace and returns 0.5 while one holds and 1.0 without, so an unbraced push is
  multiplied by exactly 1.0 and the B-04 suite's pushes are unchanged. Knockback resistance from anything else is not
  read. A blow's own small vanilla knockback, which comes from its `hurtServer`, and his boulders' and icicles' (a
  `mobProjectile` hit), are the game's as before, and those do read knockback resistance; only his scripted push
  ignores everything but a Brace.
- **The Brace deed** (B-P55): when the share is below 1, `braced(player)` reads `Brace.caster`; a caster who is not the
  braced player, is online, can fight him (`LairBosses.eligible`) and is in his fight earns
  `encounter.deed(caster, "brace " + braced player + " " + game time)`. One key a braced player and tick, so two pushes
  in one tick are one deed; no earlier part is needed, as a ward's soak needs none. A self-Brace, the Water Bangle's
  (nobody's cast) and one carried through a relog (its caster is not saved) credit no one.
- **Rimebind** (B-P54): `RIMEBOUND_WINDUP` = 4 ticks (`tools/yeti_king.py`; untuned, 04:194). `rimebound(level)`, each
  tick in `customServerAiStep`, sees a binding as his Thaw growing (`Rimebind.RIME_THAW`, which only counts down) and
  sets `rimeHold` to `RIMEBOUND_WINDUP` (set, never added), with snowflakes and the sound of glass; never while he waits
  on his throne. `fight()` then holds a wind-up while it is before its last two ticks: the hold counts down, he stands
  still on his floor, frost-locked, and the attack does not advance. A binding after that point waits for the next
  wind-up, however long he reels or roars, and a hold that waits outlives the binding and its Thaw. `reset()` forgets
  it. `rimeHold()` reads it for tests. Neither `rimeHold` nor `thawLeft` is saved, so a king loaded mid-Thaw shows that
  binding once more. His movement is his own steps, so Rimebound's slow does not change his pace.
- **The column** (B-P56): `YetiKingEntity.baited`, after the column deed, calls `Rime.column(player)`, which with the
  Concordance switched on records `practice:jugcraft:rimework:elective` (`Rime.KEY_ELECTIVE`) for a player who
  understands Rimekeeping (the practice's gate). The Frost boon's slow records the same key (unit 4's `frost_boon`,
  renamed), so the two electives stand for one kind between them: with `restraint` they make two, and every mastery
  needs a Preserve key, so the Alembic Arts.
- **Texts:** the Braced and Rimebound codex entries each gain an "Against the Yeti King" page, the Rimekeeping entry's
  Understood page and the Rimekeeper advancement name the column, and the Glacier Hall dossier's credit line names
  bracing a friend (B-P57), all generated; the player guide's advice was written by hand.
- **Checks:** `check_yeti` (the push through `Brace.spend` and the share on `up`, no knockback-resistance read, the
  spike through `king.push`, the deed's caster and key, the hold: set not added, `- 2`, not while waiting, reset, the
  Java constant against the tool, the column's call in `baited`); `check_winter_links` (`Rime.column`'s one caller, the
  switch, the elective key, recording only); `check_rime` (the electives on one key; no mastery without the Alembic
  Arts). Deliberate faults on a scratch copy: each of 12 with the counters and 15 with the column failed
  `check_mod_data` in their commits, and the spec's list for these checks, adapted to B-P56, was caught again before
  the records commit.
- **Tests,** in `YetiKingGameTests`, each passed in CI (run 38591108210, with and without the optional integrations):
  `aBracedPlayerIsPushedHalfAsFarOnce` (his slam's push the same
  with knockback resistance 1 from elsewhere, half braced, whoever put the Brace on; a second push that tick braced too;
  gone the next), `hisBlowAndSpikeAreBracedOnceNotTwice` (a swipe and a spike's lift halved once, not a quarter),
  `aBraceOnAFriendIsTheCastersDeed` (one deed a push for the caster; nothing for oneself, the bangle's or a forgotten
  caster's), `aRimeboundKingWindsUpLongerOnceAThaw` (a swipe strikes 16 ticks after it begins, not 12; on time while he
  thaws; a binding while he reels holds the next one), `aRimeboundChargeKeepsItsPace` (bound as his run begins, seven
  steps on he is where an unbound run puts him; the binding waits for his next wind-up) and
  `aColumnIsElectiveRimeworkForItsBaiterOnce` (the column's kind once, for its baiter; nothing for one who has only
  encountered Rimekeeping or with the Concordance off; kept through a save; nothing more after a Frost boon slow, or the
  reverse; restraint and the elective do not master without a Preserve). `ConcordanceWinterGameTests`
  `aFrostBoonSlowIsElectiveRimePractice` reads the renamed key. With no counter the B-04 suite stands as it was.

### B-10. Winter gear: the Rime travel kit, Thallite and the trophies, without stat inflation (plan B4; B-D2)

Built as unit 4 with B-08 ([record](arcane-concordance-winter.md)); rewritten on 14 October 2026 to what was built. Its
new choices are B-P46 to B-P53.

The owner's water sets become Rime's travel kit (02:168: a preservation focus, snow-safe boots and a short Brace
accessory, resisting environmental freezing without permanent boss immunity), made in survival like
[Ember's regalia](arcane-concordance-ember-regalia.md) (`tools/concordance_rime_kit.py`; `concordance/rime/RimeGear`,
`RimeTrinketItem`). Names and art are the owner's; the numbers are Proposed (B-P23 to B-P25, as B-P47, B-P49 and
B-P50). Sources are under `art/owner-library/originals/Magic/assets/ars_jymbaumental/`.

| Pieces (the owner's names) | Source | Ids | As built |
|---|---|---|---|
| Nereid's Hood, Tunic, Pants and Shoes (light) | `textures/item/aqua_hood.png`, `aqua_tunic.png`, `aqua_pants.png`, `aqua_shoes.png`; `textures/armor/light_armor_water.png` | `nereids_*` | 1/2/1/1 (5, the Pyromaniac's), ×7 durability, enchantability 15, repaired with wool; wool and a snowball; worn on the imported medium model (`geckolib/models/armor/pyromancers.geo.json`) |
| Aquamancer's Hat, Robes, Leggings and Boots (medium) | `aqua_hat.png`, `aqua_robes.png`, `aqua_leggings.png`, `aqua_boots.png`; `medium_armor_water.png` | `aquamancers_*` | 1/3/2/1 (7, leather's), ×10, 15, wool; wool and lapis lazuli; the same model |
| Winterguard's Helmet, Chestplate, Leggings and Boots (heavy) | `aqua_helmet.png`, `aqua_chestplate.png`, `aqua_leggings_heavy.png`, `aqua_boots_heavy.png`; `heavy_armor_water.png`; `geo/heavy_armor_water.geo.json` | `winterguards_*` | 2/6/5/2 (15, iron's), ×15, 9, iron ingot; iron armour's iron and a snow block; its own model, imported as supplied (`jugcraft:armor/winterguards`) |
| Lesser Focus of Water and Focus of Water | `lesser_water_focus.png`, `water_focus.png` | `lesser_water_focus`, `water_focus` | The preservation focus, in the Spell Focus slot: Preserve holds 90 s or 120 s instead of 60 s (`RimeGear.holdTicks`, B-P50). 2 string, 2 gold, 2 snowballs and an amethyst shard; the focus is the lesser one with 2 diamonds and 2 gold |
| Water Bangle | `water_bangle.png` | `water_bangle` | The short Brace accessory, in a Bracelet slot: with Rimekeeping understood, Brace's own Braced (100 ticks) when its wearer has had none for 600 ticks, looked at once a second, as nobody's cast (B-P49). 4 gold, a snow block and an amethyst shard |

- **Freezing:** every set piece joins `#minecraft:freeze_immune_wearables`, so powder snow and a full freeze deal no
  damage and add no frost. The Yeti's frost slowdown stays: only Frostward and the Mitten clear his frost, so there is
  no boss immunity.
- **No stat inflation:** no Spell Power, toughness or knockback resistance on any piece, and not fire resistant.
  Frost Spell Power would scale nothing, because Rime's invocations deal no damage and Spell Power scales only damage
  (`concordance/compose/Plan.java:64-81`). Spell Power's attribute enchantments are refused on the sets, as on Ember's.
- **Snow-safe boots** (B-P46): vanilla 26.3 fixes walking on powder snow to leather boots
  (`PowderSnowBlock.canEntityWalkOnPowderSnow`, read from the 26.3 sources fetched for the unit) and has no boots tag,
  so the claim is narrowed to freeze-proof and no mixin is added.
- **Made in survival:** crafting-table recipes from Overworld things (wool, string, snowballs, snow blocks, lapis,
  iron, gold, amethyst, diamonds); no boss item, and no Nether or End step (`check_rime_kit`, and
  `concordance_journey.py` holds every piece to the first-loadout rule); kept off the Assayer's Scale like Ember's; in
  `tools/acquisition.py` `OPTIONAL` (made by recipe), so `check_acquisition` and
  `ConcordanceGameTests.FIRST_LOADOUT_RECIPES` cover them. No JEI `acquisition_info` pages: JEI shows their recipes.
- **Shared with Ember** (B-P51): the Spell Focus and Bracelet slots, `EmberArmorItem`, `EmberClient`'s renderer and the
  medium model; Ember's writer writes both slices' slot tags (`concordance_ember.SLOT_ITEMS`).
- **Imports** go through `tools/owner_art.py` (19 byte copies), with the originals untouched and the provenance in the
  record (paths, checksums, the owner's declaration); the item models are the owner's plain generated models with the
  texture renamed; only the names are taken from the owner's language file. **The heavy texture fits its model:** 71% of
  the model's faces fall on opaque pixels (the head 91%, the arms 58%), as the medium sets do on theirs.
- **Thallite** (09:106) connects through Strata's examination and the `rooted` elective, both unit 3's; unit 4 adds no
  recorder. `earthen_ground` and every Thallite number are unchanged (B-P28; `check_winter_links`, and the record's
  diff).
- **Trophies** (09:106): a slow from a Frost boon arm that takes on a hostile creature at normal tolerance
  (`Rimebind.restrains`, restraint's targets, B-P34) counts as Rime practice, the elective key in `jugcraft:rimework`
  (`ArmItem.boon` → `Rime.frostBoon`; built as `frost_boon`, renamed `elective` in unit 5 when the Yeti's column came to
  share it, B-P56), recorded only for a wielder who understands Rimekeeping (the practice's gate; this ticket first said
  "encountered": understood, an elective, B-P53). The mastery keeps needing three kinds, of which the elective can be
  one. Any Frost boon arm counts, the creative-only Frost Knight Greatsword included (B-P52). The boon itself is
  unchanged (`weapons/ArmVariants.java:123-125`; B-P10), and their JEI pages came with B-02.
- **Creative-only sets stay so:** the Frost Knight and Wight King (`tools/acquisition.py`; B-P27).

**Tests:** in `ConcordanceWinterGameTests`, `theTravelKitIsMadeAndWornAsDesigned`,
`everyPieceKeepsPowderSnowFromFreezingItsWearer`, `theWaterFociLengthenPreservesHold`,
`theWaterBangleBracesARimekeeperEveryThirtySeconds`, `theWaterBanglesBraceIsNobodysCast`,
`spellPowerEnchantmentsAreRefusedOnTheWaterSets`, `aFrostBoonSlowIsElectiveRimePractice`,
`aFrostBoonStaysOutsideTheThaw` and `thalliteStaysOutOfTheCold`; `YetiKingGameTests.aKitWearerIsSlowedButNotHurtByHisFrost`
(a kit wearer takes no freeze damage from his frost and is still slowed); `ConcordancePersistenceGameTests.theTravelKitSurvivesASave`
and the focus's hold in `theCrucibleKeepsPreservesHoldThroughASave`; the kit in
`ConcordanceGameTests.FIRST_LOADOUT_RECIPES`; `ConcordanceWinterClientGameTests` (the sets worn and slotted,
screenshots). The Strata link's tests are unit 3's (`thalliteIsAStrataSpecimen`,
`anAllyARitualAndRootedThalliteAreElective`). No test copies the Thallite numbers again:
`ThalliteGearGameTests.thalliteGearMatchesItsNumbers` (`:60`) already holds them (10:63). All passed in CI run
38549876709 (not merged).

**Save effects:** fifteen new item ids and their equipment assets, stable once released; the non-saved attachment
`jugcraft:water_bangle_braced`; one more evidence key, `practice:jugcraft:rimework:frost_boon`, renamed
`practice:jugcraft:rimework:elective` in unit 5 before any release (B-P56); the crucible's hold up to 2400 ticks from a
focus, within its own cut.

**Review boundary (unit 4, with B-08):** alchemy, items, gear and tests. In lair code, only `chill()` checking Frostward
and the dossier's read-only fact; in `weapons`, only the FROST case's call to `Rime.frostBoon`.

### B-11. Evidence and the owner checkpoint

- **CI candidate** by workflow_dispatch, every job: `mod` (the generated-file and material-data checks, the build and the
  server game tests), the build and server game tests without the optional integrations
  (`-PjugcraftOptionalIntegrations=false`), and the client shards. Locally, `tools/check_mod_data.py` and
  `scripts/check_repository.py`. Each is reported separately.
- **Demonstration table,** tied to 09:108-112: a charge into a column by solo melee, a solo caster and two test players
  (automated); footing through every floor change; cleanup; reward and return across death, disconnect, save and
  restart; the non-boss cold route (Winter Brine; the kit).
- **Client evidence** (10:109-111): screenshots of the three spell icons and the status icons in the HUD; the three sets
  worn, front and back, and the foci and bangle in their slots; the Yeti charging into a column and reeling. New client
  test classes are added to `tools/select_client_tests.py` and the gametest `fabric.mod.json`.
- **Combined fixes** (unit 5's list; each found in the units' CI; both fixed in unit 5 and seen fixed in run
  38591108210):
  - The encyclopedia's home page (`jugcraft_encyclopedia_home`, run 38519234934, shard 0): the Magic card's link is cut
    to "Open Magic: the Arcane Concorda…", and the body's last line is clipped above it (the Machinery card's body also
    stops mid-line). First recorded as a Chunk A defect; the screen is #322's guide directory
    (`EncyclopediaRoutesScreen`), on main through #352, not Chunk A's. Fixed in unit 5 (`d22a55830`, B-P58).
    Merging main f904df3fd (#415's book-style guides directory) on 23 October 2026 replaced these cards; see [Known defects and limits](#known-defects-and-limits).
  - The Water Bangle's shot (`jugcraft_rime_kit_bangle_brace`, run 38549876709, shard 4): the advancement toasts the
    test's research grant raises cover the top right, so Braced's icon is not seen. `ConcordanceWinterClientGameTests`
    should clear the toasts before its HUD shots, as `ConcordanceRimeStrataClientGameTests` does. A unit 4 test defect;
    the bangle's Brace itself passed the test's server check. Fixed in unit 5 (`fc9b404dd`): the test clears them
    before that shot, as unit 3's does.
- **Not run, expected:** two clients in one fight; play by hand; timings against guns and mobility items; completion
  times (04:192-194).
- **The checkpoint:** 300 to 500 words under the fixed headings (10:87-121), with 2 to 4 screenshots and a short
  demonstration route. Ask about movement feel, difficulty and the next school (09:114); do not ask whether to run save
  tests. The pull request states the player result, progression links, actual validation, save compatibility, known
  limits and the AI attribution (CLAUDE.md).

**As built (unit 5):** the two combined fixes above (`d22a55830`, `fc9b404dd`) and the client evidence (`fc9b404dd`);
no new client test class, so neither `tools/select_client_tests.py` nor the gametest `fabric.mod.json` changes. All of
it passed in CI (run 38591108210); what the shots show is under
[Unit 5: evidence and handoff](#unit-5-evidence-and-handoff-b-09-b-11).
- **The home's route cards** (B-P58): each card is as tall as its whole overview and its whole "Open" link, the link
  wraps to the card, and the cards sit in the shared `ComposerScroll` (unchanged), scrolled by the wheel, a scroll bar
  or Tab (which shows the card it reaches from its top) when they are taller than the room below the introduction;
  windows where they fitted look as before. `RouteCard.needed()` sizes a card; the overview and the link are wrapped
  once (`Font.splitIgnoringLanguage`) and drawn without a scissor of their own; `homeCards()` and `HomeCard` read the
  cards out for tests. `check_encyclopedia_routes` requires the wrapped link, no scissor in the card, no cut link and
  the scrolling body (4 deliberate faults, each caught; the old screen fails it). `EncyclopediaRoutesClientGameTests`
  checks every card's lines against its height, its whole title and link and its foot within the body's reach on
  opening, after Tab, scrolled to the end and at GUI scale 1, with the new shots `jugcraft_encyclopedia_home_end` and
  `jugcraft_encyclopedia_home_scale1`. At CI's size `jugcraft_encyclopedia_home` will still show text cut at the
  scrolling body's lower edge beside a scroll bar: that is the scrolling, and `_home_end` and the logged numbers are the
  evidence.
  Merging main f904df3fd (#415's book-style guides directory) on 23 October 2026 replaced these cards; the card fix
  (`d22a55830`), its `check_mod_data` check and the `_home_end`/`_home_scale1` shots were dropped in the merge, and
  `RouteCard`, `homeCards()` and `HomeCard` no longer exist; see [Known defects and limits](#known-defects-and-limits).
- **The Yeti charging into a column and reeling** (`YetiKingClientGameTests`): in the real Glacier Hall, his Avalanche
  Charge baited into the north-west ice column, running (`jugcraft_yeti_king_charge_column`) and reeling at z 28.8
  (`jugcraft_yeti_king_reeling`); the column is checked as the real player's deed.
- **The three spell icons** (`ConcordanceRimeStrataClientGameTests`): the spell bar again at GUI scale 1, where all six
  slots should fit, its width logged (`jugcraft_rime_strata_spell_bar_scale1`; the bar's place is B-P60).
- **The foci and bangle in their slots** (`ConcordanceWinterClientGameTests`): the survival inventory with the Focus of
  Water and the Water Bangle in their Trinkets slots (`jugcraft_rime_kit_slots`); Trinkets may draw one slot of a
  group until it is hovered, so the shot may not show both, and the server's checks remain the proof that they are
  slotted. The purple shape in run 38549876709's bangle shot is identified from the code as the shared effect
  boundary's mark for the bangle's Brace (vanilla witch particles rising through the first-person camera), expected
  presentation ([the winter record](arcane-concordance-winter.md#checks-run-for-the-unit)).
- **The two new dossiers** (`EncyclopediaClientGameTests`): Expeditions scrolled to the Glacier Hall's and the Cinder
  Kiln's, their toasts cleared (`jugcraft_encyclopedia_expedition_glacier_hall`, `_cinder_kiln`).
- **Evidence and handoff:** after the candidate's CI, the section
  [Unit 5: evidence and handoff](#unit-5-evidence-and-handoff-b-09-b-11) (the demonstration table, the client evidence
  as seen, what has not run, known limits) and the owner's checkpoint, reported to the owner and not kept in the
  repository, whose questions are movement
  feel (being thrown, a Brace halving a throw, the 4-tick hold and the charge-and-column loop; the recommendation is to
  keep them), difficulty (the four-player cap, B-P59) and the next school (Chunk C or Tempest; the recommendation is
  C), with one line pointing to B-P20, B-P25 and B-P49, B-P46 and B-P60, and 2 to 4 shots: the charge into the column
  and his reeling (`jugcraft_yeti_king_charge_column`, `jugcraft_yeti_king_reeling`), the kit
  (`jugcraft_rime_kit_close_front`) and the winter workshop (`jugcraft_winter_frostward_hud` or
  `jugcraft_concordance_crucibles`), described only if someone has viewed them.

## Interfaces expected to change

- **Saved data:** no new saved format. New boss ids `yeti_king` and `cinder_tyrant` inside `jugcraft:lair_rewards`; the
  crucible's optional `held_until`; saved mob effects and their attribute modifiers; new research keys; new item ids.
  Not saved: the attachments `jugcraft:brace_caster` and `jugcraft:water_bangle_braced` (unit 4). Unit 5 adds no saved
  key: the column records the existing elective key (B-P56), and the Yeti's held wind-up and his last Thaw reading
  (`rimeHold`, `thawLeft`) are not saved.
- **Registries (stable once released):** mob effects `jugcraft:rimebound`, `rime_thaw`, `braced` and `frostward`, and
  the attribute modifiers `jugcraft:rimebound` and `jugcraft:braced`; components `rimegrip`, `bracing` and `rimehold`;
  invocations and spells `rimebind`, `brace` and `preserve`; research `rimekeeping` and `stratawrighting`, with their
  practices (`winter_survey`, `rimework`, `anchoring`) and advancements, and the elective key `elective` (B-P56);
  fifteen kit items, their equipment assets (`jugcraft:nereids`, `aquamancers`, `winterguards`), the GeckoLib model
  `jugcraft:armor/winterguards` and three repair tags; tags `concordance/preservable`, `rime_specimens` and
  `strata_specimens`; additions to `assay_specimens` (which nests `strata_specimens`), `concordance/resistant`,
  `minecraft:freeze_immune_wearables`, the four vanilla armour slot tags, `equivalence/excluded` and the trinket slot
  tags `chest/spell_focus` and `hand/bracelet`.
- **Shared code:** `YetiKingLoot` and `CinderTyrantLoot` gain `BOSS`, `pay` and `roll` by origin (the entity overloads
  kept); `LairDossiers.Boss` gains `trophies` (one, or a pair), `offSeason` (null for a rite that works all year) and
  `sales`, and `LairDossiers` gains `beaten`, `anyBeaten` and `canOpen`; `FirstLightRoute.FIRST_WINS` is removed (B-02
  as built); `ConcordanceEffects` gains Rime's alteration and `accepts` case, the helpful-push exemption and, after a
  status takes, Rimebind's thaw and Brace's caster; a new Guard; `Band.keep`; `Mixture` gains the Rime heat rule
  (`Mixture.rimeKeep`, `RIME_KEEP_WARM`, `RIME_KEEP_HOT`, `RIME_KEEP_SEARING`) and `Outcome` the reason
  `alchemy.thawed`; `CrucibleBlockEntity` gains the hold and records Rime brew practice; `YetiKingEntity` gains the
  Frostward check (unit 4), the taking of Brace and the Rimebind wind-up; `Benchmark` learns `rimebound`, its thaw and
  knockback. As built in unit 5: `YetiKingEntity.push` is an instance method that every push of his goes through
  (`Brace.spend`, then `braced` for the caster's deed), `YetiKingEntity.RIMEBOUND_WINDUP` and `rimeHold()` (a read-out
  for tests); `Rime.column`; and, outside Chunk B's own code, `EncyclopediaRoutesScreen.homeCards()` and `HomeCard` (a
  read-out for tests; B-11; dropped when merging main f904df3fd, #415, on 23 October 2026). Unit 3's interfaces for later units: `Brace.spend`, `Brace.caster`, `Rimebind.RIME_THAW`
  and `Rime.MAX_HOLD_TICKS`. Unit 4's: `Frostward.STATUS`, `Frostward.FROSTWARD` and `Frostward.warded`;
  `RimeGear.holdTicks` (which `Rime.preserve` asks), `RimeGear.bangle` and `RimeGear.brace`; `Rimebind.restrains`
  (restraint's targets, shared with `Rime.frostBoon`); `Rime.frostBoon` and its key, `Rime.KEY_ELECTIVE` since unit 5
  (B-P56; unit 4's `KEY_FROST_BOON`), which unit 5's `Rime.column` records too; `LairDossiers.prepare`;
  `concordance_ember.SLOT_ITEMS` (one writer for both slices' slot tags); `EmberArmorItem` and `EmberClient`'s renderer,
  now also the water sets'. Unit 2's: `YetiKingEntity.owning` (his whelps and things last only while he counts them
  his), and he and his whelps refuse Levitation (`canBeAffected`). Practice hooks: Fabric's level change
  (`glacier_hall`; no lair code, and no `Encounter.onDeed`), `CrucibleBlockEntity.bottle` (`crucible`), `Rituals.listen`
  (`foundation`) and `AFTER_DAMAGE` (`rooted`, `held`) in unit 3; `ArmItem`'s Frost boon in unit 4 and the Yeti's column
  (`YetiKingEntity.baited`) in unit 5, one key between them (`elective`, B-P56).
- **Tools:** `acquisition.py`, `check_acquisition`, `encyclopedia.py`, the `concordance.py` merge points,
  `concordance_alchemy.py`, `concordance_equivalence.py`, `concordance_progression.py`, `concordance_conclave.py`,
  `owner_art.py`; new `concordance_rime.py`, `concordance_strata.py` and their checks. Unit 4: `concordance_ember.py`
  (`SLOT_ITEMS`), `gear.py` (the kit's armour tags), `concordance_journey.py`, `concordance_delivery.py`,
  `select_client_tests.py`; new `concordance_rime_kit.py`, and the checks `check_winter_brine`, `check_rime_kit` and
  `check_winter_links`. Unit 5: `yeti_king.py` (`RIMEBOUND_WINDUP`), `concordance_rime.py` (the shared elective),
  `concordance_strata.py` and `encyclopedia.py` (texts); `check_yeti`, `check_winter_links`, `check_rime` and
  `check_encyclopedia_routes`.
- **Network:** no new payload. Jade's crucible snapshot carries one more value (unit 3).

## Dependencies and order

1. B-00, then unit 1: B-01, then B-02. B-02 builds on #337's encyclopedia, which #352 keeps.
2. Unit 2 (B-03, B-04) after unit 1: B-03's restart test pays a Due through B-01.
3. Unit 3: B-05, B-06 and B-07 together.
4. Unit 4 (B-08, B-10) after unit 3: Rime research gates the kit's behaviour and the brew evidence, and the foci lengthen
   Preserve.
5. Unit 5 (B-09, B-11) after units 2 to 4.

Can start now, with no further answer: B-01 (the Yeti, then the Tyrant's commit), B-02's generic tooling, B-03 and B-04.
B-05 cannot start on its own.

## Risks

- **Base:** #337 is not merged, and #352 carries it with 30 other pull requests. Whichever merges, Chunk B rebases, and
  #323's crucible change meets B-07's hold field.
- **Invocation budget:** 3 of 16 slots remain after Chunk B; a later Rime or Strata spell must avoid the 13 signatures
  then taken.
- **Alchemy outcomes change:** new Rime brews give Frostward, and a warm stir now costs Rime, in amethyst and gloamcap
  brews too. Bottles already made keep Resistance.
- **Preserve's alteration:** the default branch removes blocks (`ConcordanceEffects.java:274`); a missing `accepts` case
  would delete them. Tested.
- **Brace stacking:** with netherite, Rooted, the Ice Breaker and a shield, one hit may reach full knockback resistance.
  Proposed as tolerable for one hit in five seconds (B-P11); the Yeti ignores all but Brace (B-P20, Proposed).
- **The charge at the hall's edges** is held by server tests at fixed places in the hall's template (unit 2). A change to
  the columns, dais, ramp or dens in `tools/glacier_hall.py` moves what they test, so their places must follow it.
- **Designed worlds** are tested as a world made with the designer's generator, not one made from an exported ZIP
  (B-03's deliberate deviation); a ZIP world stays a check by hand. A dimension another mod adds only through its own
  world preset is not inherited (`docs/WORLD_DESIGNER.md`).
- **Downgrade:** an older build drops kept Yeti or Tyrant rewards (`LairRewards.java:184-189`).
- **Names:** "Focus" is both the resource and the foci; "Brace" is also a war fork trait and the shield's brace.
  Tooltips keep them apart.
- **Size:** unit 4 adds fifteen items and a second armour model. The heavy model's fit is measured: 71% of its faces on
  opaque pixels, the head 91% (the record), held by `check_rime_kit`.
- **The Water Bangle's role** (a Brace) differs from the owner's tooltip text in the library (a chill on each blow); see
  B-P25.
- **The Expeditions page** reaches its grid limit with four keepers; a fifth needs a layout change.
- **The rite words name the default tags** (`#jugcraft:frost_horn_ground`, `#jugcraft:kiln_seal_lands`): a data pack
  that changes them is not reflected in the dossier, as with the items' tooltips.
- **A used-up token hides its lair again:** discovery is read, not saved (A-D9), so a player who went in and used up
  their Frost Horn (or Kiln Seal) reads as not having found the Glacier Hall (or the Cinder Kiln) until they carry one
  again or win; the same as the Witching Season's lairs, now for every lair.
- **Two bosses stay on the old path:** the Mire Hag and the Crypt Lich are on main through #352 with damage-only rewards
  (`lair/hag/MireHagEntity.java:249`, `lair/lich/CryptLichEntity.java:258`; this page first said "in their pull
  requests"); after Chunk B, four bosses are on the encounter credit and two are not, and moving them onto `Encounter`
  is outside Chunk B.
- **Overlap with Chunk C's preservation wash** (09:124): Winter Brine stays a frost counter.
- **Unit 3, as built** ([record](arcane-concordance-rime-strata.md#rollout-and-open-questions)): sneak-use in the air
  now examines snow, ice and four stones, so a sneaking player cannot throw a snowball; the Glacier Hall, whose Frost
  Horn recipe sits under the `agriculture` switch, understands Rimekeeping without a bench, but its mastery needs the
  Alembic Arts, so the bench stays mandatory at Adept; milk clears the Thaw with Rimebound; recasting Preserve each
  minute holds one crucible indefinitely; an older build can drop a creature's effects on meeting an unknown one and
  keep their modifiers until `/attribute` removes them; Spell Engine keys only nine spells, so a player with more than
  nine invocations loses the last ones from the bar (the #250 order question); vanilla's knockback without a damaging
  blow is braced without spending the Brace. B-P24's 120 s hold from a focus worn with the Initiate's Wand exceeds that
  wand's 1200-tick duration limit: unit 4 records why (B-P50: the hold is the crucible's state, cut at 2400).
- **Found in CI** (runs 38509414542 and 38519234934): four or more players scale the Yeti King's health to 1050 and the
  Cinder Tyrant's to 1100, but the game caps a creature's health at 1024, so both stop there (the scale is unchanged);
  Spell Engine's spell bar starts at a fixed place left of the screen's bottom centre, so a bar of 5 or more spells runs
  off the left edge of CI's 427-pixel-wide GUI, and 8 or more at 1920×1080 (players can move it in Spell Engine's HUD
  settings; [record](arcane-concordance-rime-strata.md#rollout-and-open-questions)).
- **Unit 2, as built** ([record](yeti-king.md#rollout-and-open-questions)): on Peaceful the horn is used up and the hall
  opens empty (B-P37); any step that would bring him within 5.2 blocks of a column's centre stuns him, so a charge begun
  that near one can end on its first step though its line passes the column, and a glancing run that would meet a
  column's foot now stuns him too (B-P38); a player who leaves mid-fight and comes back meets none of his kin or ice from
  before; a client back in his sight gets his bar and fury, not the fight's elapsed time.
- **Unit 4, as built** ([record](arcane-concordance-winter.md#rollout-and-open-questions)): "needs the cold band" holds
  per recipe (four snowballs stirred warm twice, or five stirred hot once, still reach Frostward; two ground snowballs
  unstirred do at any heat); Frostward also clears the Frost Horn's arrival chill and a snow werewolf's frostbite chill;
  a Frostward salve on an ally earns no lair support credit; a crucible following an old formula waits while its output
  holds old Resistance draughts; the draught is named "Draught", and "Winter Brine" only in JEI, the codex and the
  dossier; the bangle does nothing against the Yeti's own push until B-09 (since unit 5 its Brace halves one of his
  pushes and credits no one); the kit shares Ember's classes and slot writer; "snow-safe" boots are freeze-proof, not
  powder-snow walkers (B-P46).
- **Unit 5, as built** (B-09; [yeti-king.md](yeti-king.md#optional-counters-concordance-chunk-b-b-09)); its tests
  passed in CI (run 38591108210):
  - *Brace now matters against him.* The Water Bangle halves one of his pushes about every 35 seconds at no Focus cost,
    and a party recasting Brace every 8 seconds halves many. No number changed; a question for the checkpoint's
    movement feel.
  - *Only his scripted push ignores other resistance.* A blow's own small vanilla knockback, from its `hurtServer`, and
    his boulders' and icicles' still read knockback resistance, as before.
  - *The Brace deed is cheap.* One Brace that meets one of his pushes qualifies its caster, as one ward soaking 4 does.
    Its keys grow by one for each braced player and tick a push is met, bounded by Brace's 8-second cooldown and 3
    Focus; past qualifying, more adds nothing, and loot is rolled per player, so there is no gain loop. The dossier's
    share line shows the deed count (`LairDossiers`, `share.deeds()`), so Brace deeds raise that number; qualifying is
    a yes or no.
  - *The hold's edges* (B-P54): a binding mid-wind-up freezes his pose (that GeckoLib holds it is unverified); a
    wind-up cut short carries the rest of the hold to his next; a hold that waits outlives the binding and its Thaw; a
    king loaded mid-Thaw shows that binding once more; his resistance makes it at most 4 ticks in every 70.
  - *Text length:* the Braced and Rimebound pages, the Rimekeeping entry's Understood page and the dossier's credit line
    are longer; no audit or screenshot checks that a Modonomicon page fits.
  - *The home shot can mislead* (B-P58): at CI's size `jugcraft_encyclopedia_home` still shows text cut at the
    scrolling body's lower edge; `_home_end` and the logged numbers are the evidence. The stacked layout (a GUI narrower
    than 384) is not exercised. The Magic title's width is estimated from glyph widths, not read; if the title check
    fails, the fallback is to wrap it into the card's top. The title check passed (run 38591108210).
    Merging main f904df3fd (#415's book-style guides directory) on 23 October 2026 replaced these cards; see [Known defects and limits](#known-defects-and-limits).
  - *The client step's crash point* (z 28.8) relies on the hall generator's geometry, not the template file; if it
    fails, the check may need a band (28.8 to 29.5). The exact check passed (run 38591108210).
  - *Shard time:* client shard 3 took 53m33s and 53m06s in runs 38519234934 and 38543250503 against the job's
    60-minute limit (42m10s in run 38549876709), with other shard assignments. Unit 5 adds steps to five client
    classes; on a full run at this head `tools/select_client_tests.py --all` puts two of them,
    `EncyclopediaClientGameTests` and `ConcordanceRimeStrataClientGameTests`, in shard 3. Each shard's time in the
    candidate run is recorded; above about 55 minutes, `WEIGHTS` in that tool changes in its own commit. In run
    38591108210 shard 3 took 42m6s, the longest, so `WEIGHTS` is unchanged.

## Explicit don'ts

1. Do not rebuild, re-namespace or re-route the Glacier Hall, its blocks or the Frost Horn recipe (09:90; 04:118).
2. Do not rebuild the Yeti King, the Cinder Tyrant, Thallite, `Encounter` or `LairRewards`.
3. Do not create an ore, or let terrain magic pass town protections (09:94).
4. Preserve never stops machine time and never wraps an inventory (02:77; 06:96).
5. No permanent root and no mandatory counter (02:78; 04:141, 04:188).
6. No raw-stat upgrade to the trophies, Thallite or the kit (02:173; 09:106).
7. No boss-only cold requirement (04:141).
8. Do not raise the compiler caps or the 16-invocation cap (02:65-67).
9. Do not change Vesperine's or Madame Tatterlace's tolerance.
10. Do not hand-edit generated data such as `property/rime.json`; change the generator and regenerate.
11. Do not change the trophies' accepted boon without the owner (B-P10).
12. Do not make the Frost Knight or Wight King sets obtainable (B-P27).
13. Do not import whole language files from the owner's library, or execute anything in it.
14. Do not mark a Proposed decision approved because no reply came (10:162). Do not count one client as two, or a marked
    disconnect as a real one (09:20).
15. Do not claim "persistent lairs" (04:9) or "balanced" (04:194).
16. Do not edit the Mire Hag's or the Crypt Lich's files beyond shared docs and lists.

## Validation plan

| Unit | Server game tests | Client game tests | Checks | Save tests |
|---|---|---|---|---|
| 1 | `YetiKingGameTests`, `CinderTyrantGameTests`, `LairEncounterGameTests`, `EncyclopediaGameTests`; passed (runs 38514144531 and 38519234934, and again in 38549876709) | `YetiKingClientGameTests` and `CinderTyrantClientGameTests` unchanged; `EncyclopediaClientGameTests` if the page layout changes (the test is unchanged); each passed by its shard | `check_acquisition`, `check_encyclopedia`, with mutations | A kept Yeti and Tyrant Due across a save; passed |
| 2 | `GlacierHallGameTests` (+2), `YetiKingGameTests` (+17); passed (run 38543250503, and again in 38549876709) | `WorldDesignerClientGameTests` (a world with the designer's generator, created and reopened; not an exported ZIP, B-03's deviation); `LairClientGameTests` steps 7b and 9 (the hall at a simulated and a real restart); `GlacierHallClientGameTests`; `YetiKingClientGameTests` (his lake as placed, away and back); each passed by its shard | `node --test tools/world-designer/model.test.cjs` (+1, in the `mod` job); `check_glacier_hall`, `check_yeti` | A kept `yeti_king` Due across a real save and reopen (step 9); his reload, emulated (`loadedAgainMidFightHeDriftsHisSnowBackAndBringsNoKin`, `loadedAgainHeCountsNoKinOrIceFromBefore`) and real (the client test); all passed |
| 3 | `ConcordanceRimeStrataGameTests`, `ConcordanceEffectGameTests`, `ConcordanceProgressionGameTests`, `ConcordanceInvocationGameTests`, `ThalliteGameTests`, `ConcordancePersistenceGameTests`; passed (run 38519234934, and again in 38543250503 and 38549876709) | `ConcordanceRimeStrataClientGameTests` (icons, spell bar, HUD and inventory screenshots); `GlacierHallClientGameTests` (the field study; a braced player at the edge, the mist and the whirl); each passed by its shard; toasts hid the status icons in run 38519234934, and since the fix (`7d6d0b0d3`) the shots show them (runs 38534127130 and 38549876709) | `check_rime`, `check_strata`, `check_rime_strata_assets` (mutation-tested), `check_invocations`, `check_progression`, `check_conclave`, `check_delivery`, `owner_art.py --check` | The research record (`rimeAndStrataResearchSurviveASave`); saved effects (`rimeAndBraceSurviveASave`); the crucible's field (`ConcordancePersistenceGameTests`); all passed |
| 4 | `ConcordanceAlchemyGameTests` (+4), `ConcordanceWinterGameTests` (new, 10), `YetiKingGameTests` (+2), `EncyclopediaGameTests` (+1), `ConcordancePersistenceGameTests` (+1, and the focus's hold), `ConcordanceGameTests` (`FIRST_LOADOUT_RECIPES`); passed (run 38549876709) | `ConcordanceWinterClientGameTests` (new: Frostward's HUD; the sets worn in four views, close up and on the player; his frost on the real player; the slots; the icons); passed by its shard (4), though the bangle's shot is covered by toasts (B-11's list) | `check_alchemy`, `check_winter_brine`, `check_rime_kit`, `check_winter_links`, `check_ember_regalia`, `check_acquisition`, `check_progression`, `check_delivery`, `concordance_journey.py`, `check_icon_maps.py`, `owner_art.py --check` (the unit's rules mutation-tested: 60 faults, each reported); `check_mod_data` and the generated-file check passed in CI | A Rime bottle from before the change (`oldRimeBottlesStillGiveResistance`); a formula from before; Frostward on a saved creature; the kit's fifteen items and the bangle's unsaved count (`theTravelKitSurvivesASave`); a focus's hold; the `frost_boon` key before mastery; all passed |
| 5 | `YetiKingGameTests` (+6: `aBracedPlayerIsPushedHalfAsFarOnce`, `hisBlowAndSpikeAreBracedOnceNotTwice`, `aBraceOnAFriendIsTheCastersDeed`, `aRimeboundKingWindsUpLongerOnceAThaw`, `aRimeboundChargeKeepsItsPace`, `aColumnIsElectiveRimeworkForItsBaiterOnce`); `ConcordanceWinterGameTests.aFrostBoonSlowIsElectiveRimePractice` (the renamed key); passed (run 38591108210) | `YetiKingClientGameTests` (a charge into the north-west column, running and reeling, the column the real player's deed); `ConcordanceRimeStrataClientGameTests` (the spell bar at GUI scale 1); `ConcordanceWinterClientGameTests` (toasts cleared before the bangle's shot; the foci and bangle in their slots); `EncyclopediaClientGameTests` (Expeditions scrolled to the two new dossiers); `EncyclopediaRoutesClientGameTests` (the home's route cards whole on opening, after Tab, scrolled to the end and at GUI scale 1); each passed by its shard (run 38591108210) | `check_yeti`, `check_winter_links`, `check_rime`, `check_encyclopedia_routes` (31 deliberate faults, each caught: 12, 15 and 4 in the unit's first three commits); every check; the CI candidate (run 38591108210: every job passed on its second attempt) | The elective key through a save before mastery (`aColumnIsElectiveRimeworkForItsBaiterOnce`); nothing new is saved by the Yeti; re-run of all of the above; all passed |

Every unit also runs `tools/check_mod_data.py`, `scripts/check_repository.py` and the build with its server game tests,
and the build without the optional integrations where it touches an optional boundary.

**Results so far** (units 1 to 5, not merged; each run by workflow_dispatch of the Build workflow, with every client
test class):
- Run 38509414542, unit 1 before its last fix: everything passed but `YetiKingGameTests.fourPlayersAreEachPaidOnce` and
  `CinderTyrantGameTests.fourPlayersAreEachPaidOnce`, which expected a party-scaled health above the game's
  `max_health` cap of 1024 (the Yeti King 420 × 2.5 = 1050; the Tyrant 440 × 2.5 = 1100). `579ab5f6f` made the tests
  expect the cap.
- Run 38514144531 on `579ab5f6f` (unit 1): all 9 jobs succeeded: `mod`, `optional integrations absent`,
  `choose client tests`, client shards 0 to 4, and `client`.
- Run 38519234934 on `06dc5ea5f` (units 1 and 3): all 9 jobs succeeded. `mod`: "All 1639 required tests passed" in 3.994
  min ("BUILD SUCCESSFUL in 6m 3s"); `optional integrations absent`: the same in 3.930 min. The server logs do not name
  the tests that pass; every server test units 1 and 3 name is registered and required (none sets `required = false`),
  so each passed as one of the 1639. `choose client tests` chose all 163 client test classes in five shards, and each
  succeeded (0 in 21m35s, 1 in 39m20s, 2 in 22m46s, 3 in 53m33s, 4 in 23m51s). The client logs print no per-class
  result, but a failing class stops the client with its error and fails its shard's `runClientGameTest`, so each class
  passed by its shard: `EncyclopediaClientGameTests` in shard 0; `CinderKilnClientGameTests` in 1;
  `CinderTyrantClientGameTests`, `LairClientGameTests` and `ConcordanceRimeStrataClientGameTests` in 2;
  `GlacierHallClientGameTests` and `YetiKingClientGameTests` in 3. The baselines tables went to
  [the baselines record](arcane-concordance-baselines.md).
- In run 38519234934 the server logs' warnings that name the lairs are expected on a game-test server, which has no
  lair dimensions: "[lairs] No dimension jugcraft:glacier_hall: the lair cannot open" twice, and the same for
  `cinder_kiln` (and other lairs). No WARN or ERROR line names Rime, Strata, Brace, Rimebind, Yeti, `LairRewards` or
  `Encounter`.
- Run 38519234934's screenshots (480×270 previews in the job logs): unit 3's status shots were hidden by advancement
  toasts, and the test now clears them (`7d6d0b0d3`; runs 38534127130 and 38549876709 show the icons); its spell bar
  runs off the screen's left edge (Risks); the Glacier Hall's shots are in [its record](glacier-hall.md#verification);
  the Expeditions shot shows only the top of the page, so no shot shows the Glacier Hall's or the Cinder Kiln's dossier
  (the server tests check their content; since unit 5 the client test scrolls to each, and it passed in run
  38591108210); the
  encyclopedia home shot shows a defect of #322's guide directory (`EncyclopediaRoutesScreen`, on main through #352;
  first recorded here as Chunk A's), fixed in unit 5 (`d22a55830`, B-11).
- Run 38534127130 on `0e7de6bf1` (units 1 to 3, unit 2 before its last two fixes): `mod` and
  `optional integrations absent` each ran 1657 game tests; 1656 passed and 1 required test failed in both,
  `YetiKingGameTests.aChargeThatMeetsAColumnSouthOfItStunsHim`: "Passing the north-west column on its south side his
  body met its foot at (24.0000, 28.3012) in the template, CHARGE, stunned 0 and he slid on unstunned, to (29.7340,
  32.3164) in the template, CHARGE, stunned 0 on tick 87". The review's model had predicted (29.734, 32.3164);
  `215a1c1e0` (D1) fixed it. Client shards 0 to 3 succeeded. Shard 4 failed in `YetiKingClientGameTests`' away-and-back
  step, "java.lang.AssertionError: His ice came back with him", three ticks after he was loaded again; `1266c905a` fixed
  it, with the server test `loadedAgainHeCountsNoKinOrIceFromBefore`. Earlier in that shard
  `WorldDesignerClientGameTests` and `GlacierHallClientGameTests` passed by their log lines ("[designer-check] Authored
  heights, custom biome paint, spawn, city and generator survived save/reopen"; "the Frost Horn passed end to end").
  The fury screenshot showed glacial spikes still standing.
- Run 38543250503 on `1266c905a` (units 1 to 3): all 9 jobs succeeded. `mod`: "All 1658 required tests passed" in
  4.354 min ("BUILD SUCCESSFUL in 6m 29s"); `optional integrations absent`: the same in 3.903 min (6m 17s). Unit 2's 19
  new server tests are registered and required, so each passed as one of the 1658. `choose client tests` chose all 163
  classes in five shards, and each succeeded: `LairClientGameTests` in shard 0 (its log has "[lairs] client game test:
  the framework passed end to end", after which step 9 runs); `CinderTyrantClientGameTests` and
  `ConcordanceRimeStrataClientGameTests` in 1; `CinderKilnClientGameTests` in 2; `WorldDesignerClientGameTests`,
  `GlacierHallClientGameTests` and `YetiKingClientGameTests` in 4, whose log has "[designer-check] Authored heights,
  custom biome paint, spawn, city and generator survived save/reopen" (after the reopened world's lair check), "[lairs]
  client game test: the Frost Horn passed end to end" and "[yeti] client game test: his fight passed end to end". The
  `mod` job also ran `node --test tools/world-designer/model.test.cjs` before the build.
- Unit 2's screenshots in run 38543250503 (shard 4): the client test takes none between the away-and-back step and his
  fall. `jugcraft_yeti_king_defeated` (in run 38534127130 the test stopped before it) shows the lake's rippled drift
  snow, the patch of glare ice in its middle with a faint grey column of mist over it, the dais and throne under the
  tusks beyond, a column on either side and icicles hanging from the vault; no King, whelp or spike is in view.
  `jugcraft_yeti_king_fury` shows him facing the camera with blue fire on his crown, and pale shards of ice standing
  between his feet, which look like the glacial spikes of the pose before, as in run 38534127130.
- Run 38549876709 on `b012875ab` (units 1 to 4; unit 4's first run): all 9 jobs succeeded. `mod`: "Check generated
  JSON is up to date" and "Check material data" passed, then "All 1676 required tests passed" in 4.293 min ("BUILD
  SUCCESSFUL in 6m 24s"); `optional integrations absent`: the same in 4.319 min (7m 6s). Unit 4's 18 new server tests
  (`ConcordanceAlchemyGameTests` 4, `ConcordanceWinterGameTests` 10, `YetiKingGameTests` 2, `EncyclopediaGameTests` 1,
  `ConcordancePersistenceGameTests` 1) are registered and required, and the unit adds no other `@GameTest` and removes
  none, so 1658 + 18 = 1676 and each passed. Both logs re-run three tests that ask for several successes (the lair's
  last rites, Arms VIII's javelin, Tatterlace's brood); their WARN lines are the expected ones (a newer test record, a
  spire entry kept, and "[lairs] No dimension …" for five lairs, the Glacier Hall twice); no WARN or ERROR line names
  Frostward, Winter, Brine, Rime, `RimeGear`, the bangle, a focus, Winterguard, Aquamancer, Nereid or the alchemy.
  `choose client tests` chose all 164 classes in five shards, and each succeeded (0 in 23m20s, 1 in 32m54s, 2 in
  35m14s, 3 in 42m10s, 4 in 27m43s): `EncyclopediaClientGameTests` in shard 0; `CinderTyrantClientGameTests` in 1;
  `LairClientGameTests`, `YetiKingClientGameTests` and `GlacierHallClientGameTests` in 2; `CinderKilnClientGameTests`,
  `WorldDesignerClientGameTests` and `ConcordanceRimeStrataClientGameTests` in 3; `ConcordanceWinterClientGameTests` in
  4. Read: each server log's last 5000 lines, `choose client tests`, and the screenshots in shard 4's last 5000 lines
  and shard 3's last 1500 (longer reads of shard 3 broke the connection).
- Run 38549876709's screenshots (480×270 previews): unit 4's are described in
  [its record](arcane-concordance-winter.md#checks-run-for-the-unit): Frostward's icon on the HUD; the three sets on
  stands and a zombie in four views and close up, and Winterguard's on the player, Nereid's and Aquamancer's alike,
  nothing drawn on the small stand; the frozen hearts all full in Winterguard's; the fifteen icons whole. The bangle's
  shot is covered by advancement toasts (B-11's list), and no shot shows the foci or bangle in their slots (since unit 5
  the test clears the toasts before it and adds `jugcraft_rime_kit_slots`, both in run 38591108210). Unit 3's
  (`jugcraft_rime_strata_*`, shard 3): Braced's and Rimebound's icons at the HUD's top right (Thawing has none); in the
  inventory, three icons between it and JEI's list, Braced, Rimebound and Thawing, without names at that width; the
  spell bar's first slot still cut off by the screen's left edge (since unit 5 a second shot at GUI scale 1,
  `jugcraft_rime_strata_spell_bar_scale1`, which shows all six slots in run 38591108210; the bar's place is B-P60). Run
  38534127130's shard 1
  showed the same.
- Run 38591108210 on `25644502a` (units 1 to 5; unit 5's candidate): every job passed on its second attempt, a re-run
  of the two jobs whose first attempt failed on tests outside Chunk B. Each job, its numbers, the shard times and the
  screenshots are under [Unit 5: evidence and handoff](#unit-5-evidence-and-handoff-b-09-b-11).

**Evidence rules** (10:5-22):
- Each command is reported on its own as run and passed, run and failed, or not run. CI on a commit counts for that
  commit only.
- A server game test with several test players is not a party of people. One client on a dedicated server is not two
  clients. A disconnect marked in a test is not a real one.
- A screenshot shows presentation, not balance. Nothing is called "balanced" without a human playtest.
- Inferences in this page stay inferences until a test runs. Rimebind's slow leaving his steps alone is no longer one:
  `aRimeboundChargeKeepsItsPace` shows it (run 38591108210). Knitwear against the Yeti is no longer one:
  `woolAndLeatherStopTheFreezeButNotHisFrost` shows it (run 38543250503), and `aKitWearerIsSlowedButNotHurtByHisFrost`
  shows the same for the kit (run 38549876709).

## Unit 5: evidence and handoff (B-09, B-11)

- Candidate: `25644502a` on `claude/chunk-b-winter-stone` (units 1 to 5 on main `feb6d04fb`; not merged). CI run
  [38591108210](https://github.com/jimbozoomer-byte/jugcraft/actions/runs/38591108210) (workflow_dispatch, every client
  test class), in two attempts. Every job passed on the second; the first failed two tests outside Chunk B. The commit
  after the candidate changes only these records.
- Attempt 1:
  - `mod` succeeded: "Check generated JSON is up to date", "Check material data" and the World Designer's tests passed,
    then "1682 GAME TESTS COMPLETE IN 2.522 min" and "All 1682 required tests passed" ("BUILD SUCCESSFUL in 3m 46s").
  - `optional integrations absent` failed: of 1682 game tests (3.407 min), "1 required tests failed",
    `NuclearPlantGameTests.reactorBuildingsKeepRadiationIn`: "large plant at full power: 0.10419981004870293 uSv/h in
    the open … want under 0.1" ("BUILD FAILED in 5m 43s"). The same test passed in `mod` (worst 0.0178060248290086
    uSv/h). It and `mediumPlantRelayRoomReadsTheReactor` both build at offset (5000, 0, -5000), a clash between two
    tests on main (both from #352); Chunk B touches no nuclear code.
  - `choose client tests` chose all 164 classes in five shards. Client shards 0 to 3 succeeded.
  - Client shard 4 failed after 10m45s in `local.peepo.PeepoDeliveryClientTests.foodBreak` ("meal:
    position=(4.864861434570458, -56.0, 0.36423322259519786), status=IDLE, hungry=true, output=512, source=64"; "Timed
    out waiting for predicate"), a known intermittent failure that the open pull request
    [#357](https://github.com/jimbozoomer-byte/jugcraft/pull/357) describes and fixes; Chunk B touches no Peepo code.
    The shard stopped before 28 of its classes ran, `ConcordanceWinterClientGameTests` among them, and `client` failed
    with it.
- Attempt 2, a re-run of the failed jobs: every job succeeded.
  - `optional integrations absent` ran again: "1682 GAME TESTS COMPLETE IN 4.112 min" and "All 1682 required tests
    passed" ("BUILD SUCCESSFUL in 7m 1s"; the large plant's worst reading 0.0178060248290086 uSv/h).
  - Client shard 4 ran again, all 33 of its classes, and succeeded ("BUILD SUCCESSFUL in 24m 30s"); then `client`
    ("Every client test shard passed").
  - The attempt's other jobs (`mod`, `choose client tests`, client shards 0 to 3) have new job ids but attempt 1's start
    and end times and logs: GitHub carried their results over, it did not run them again.
- Server game tests: unit 5 adds six `@GameTest`s (`YetiKingGameTests`, row 5 below), removes none and marks none
  `required = false`, so 1676 + 6 = 1682, and each passed with and without the optional integrations.
  `ConcordanceWinterGameTests.aFrostBoonSlowIsElectiveRimePractice` (the renamed key) is among them. The B-04 tests are
  unchanged (unit 5 changed only the class's Javadoc and added helpers and tests) and passed.
- Client game tests: the logs print no per-class result, but a failing class stops its shard's `runClientGameTest`, so
  each passed by its shard: `EncyclopediaRoutesClientGameTests`, `CinderTyrantClientGameTests` and
  `WorldDesignerClientGameTests` in shard 1; `YetiKingClientGameTests` and `GlacierHallClientGameTests` in 2;
  `EncyclopediaClientGameTests`, `CinderKilnClientGameTests` and `ConcordanceRimeStrataClientGameTests` in 3;
  `ConcordanceWinterClientGameTests` and `LairClientGameTests` in 4 (attempt 2).
- Shard times (each job's start to end): 0 in 21m41s, 1 in 39m29s, 2 in 39m19s, 3 in 42m6s, and 4 in 25m29s (attempt 2;
  10m45s to its failure in attempt 1). Shard 3, which took 53m33s and 53m6s in runs 38519234934 and 38543250503
  against its 60-minute limit (42m10s in 38549876709), is the longest and well inside it, so
  `tools/select_client_tests.py` is unchanged.
- CI runs on the way (workflow_dispatch; this branch has no open pull request; details under the Validation plan's
  results): 38509414542 (unit 1 before its last fix: the two four-player tests expected health above the game's cap);
  38514144531 (unit 1) and 38519234934 (units 1 and 3), every job passed; 38534127130 (units 1 to 3), which found unit
  2's two faults (`215a1c1e0`, `1266c905a`); 38543250503 (units 1 to 3) and 38549876709 (units 1 to 4, `b012875ab`),
  every job passed.
- Local, at the candidate: `python3 -B tools/owner_art.py --check`, `tools/check_mod_data.py`,
  `scripts/check_repository.py`, `tools/concordance_journey.py` and `git diff --check` pass; a syntax parse of the 12
  Java files unit 5 changes found no error. Again on this records commit: `tools/check_mod_data.py`,
  `scripts/check_repository.py`, `tools/owner_art.py --check` and `git diff --check` pass. `./gradlew build` and
  `./gradlew runClientGameTest` run in CI only.
- Unit 5's own fixes: the two on B-11's list, each found in the units' CI and each seen fixed in this run: the
  encyclopedia home's route cards (`d22a55830`, B-P58; dropped when merging main f904df3fd, #415, on 23 October 2026) and the advancement toasts cleared before the Water Bangle's
  shot (`fc9b404dd`); and the unit review's text fixes (`25644502a`: what lessens his throws and which wind-up a binding
  holds, in the codex, the player guide and the records). The candidate's CI found no fault in unit 5.

### The demonstration, step by step (B-11)

Tied to 09:108-112. "Server" means a game-test server with test (mock) players; "client" means one real client, never
two. Every test named passed in run 38591108210 (the server tests with and without the optional integrations, the
client tests by their shards).

| # | 09:108-112 item | Evidence |
|---|---|---|
| 1 | A charge into a column, solo melee (09:110) | Server: `YetiKingGameTests.soloMeleeBaitsAChargeIntoAColumnAndStrikesHimReeling` (he stops at (26.5, 28.8); a blow while he reels takes a third more; the blow is credited). Client: `YetiKingClientGameTests`, his Avalanche Charge baited into the north-west column of the real hall, running and then reeling at (26.5, 28.8), the column the real player's deed (`jugcraft_yeti_king_charge_column`, `jugcraft_yeti_king_reeling`) |
| 2 | A solo caster | Server: `soloCasterBaitsAChargeIntoAColumnAndLancesHimReeling` (the Lance, cast through Spell Engine, takes 5 × 1.3333; Focus charged once; the caster credited) |
| 3 | Two test players | Server: `aPartyOfTwoSolvesTheColumnLoop` (side-stepping bait, the caster on his line trampled once, both credited); `twoBaitersEachEarnTheirDeed` (the key `column <uuid>`, once a fight); `anIdleLateArrivalEarnsNothing`. These are mock players, not people |
| 4 | The column's collision, deterministic at the hall's edges (09:102) | Server: `aChargeIntoAColumnStunsHim`, `aGrazingChargeStunsHimOnlyWithinAColumnsReach`, `inTheHallHisChargeStopsAtTheDaisTheRampAndTheDensWall` (the placed hall), `aChargeThatMeetsAColumnSouthOfItStunsHim`, `aChargeThatClearsAColumnsFootRunsOnUnstunned`, `aChargeEndsAtHisLeashTheSameEveryRun`, `inHisFuryAMissedChargeIsFollowedByExactlyOneMore`, `inHisFuryABaitLeadsTheChainedChargeIntoAColumn`, `endGunLevitationLiftsNeitherHimNorHisKin` |
| 5 | The optional counters (B-09) | Server, `YetiKingGameTests`: `aBracedPlayerIsPushedHalfAsFarOnce` (his slam's push the same with knockback resistance 1 from elsewhere, half when braced, whoever put the Brace on; a second push that tick braced too; gone the next); `hisBlowAndSpikeAreBracedOnceNotTwice` (a swipe and a spike's lift halved once, not to a quarter); `aBraceOnAFriendIsTheCastersDeed` (one deed a push for the caster; nothing for one's own Brace, the bangle's or a forgotten caster's); `aRimeboundKingWindsUpLongerOnceAThaw` (a swipe strikes 16 ticks after it begins, not 12; on time while he thaws; a binding while he reels holds the next one); `aRimeboundChargeKeepsItsPace` (bound as his run begins, seven steps on he is where an unbound run puts him); `aColumnIsElectiveRimeworkForItsBaiterOnce` (the elective kind once, for its baiter; nothing without Rimekeeping understood or with the Concordance off; kept through a save; one kind with a Frost boon slow; no mastery without a Preserve); and `ConcordanceWinterGameTests.aFrostBoonSlowIsElectiveRimePractice` (the shared key). With no counter, rows 1 to 4 and 6 to 8 pass unchanged. Client: row 1's column step; the Water Bangle's Brace on the real player (`ConcordanceWinterClientGameTests`, `jugcraft_rime_kit_bangle_brace`) |
| 6 | Footing through every floor change | Server: his slam, `hisSlamBaresTheDriftSnowAndItDriftsBack`; a reset, `leftAloneAfterHisSlamHisLakeIsAsItWas`; the fury ring, an overlapping slam and his death, `whenHeFallsHisSnowDriftsBackAndHisKinAndIceAreGone`; a load, `loadedAgainMidFightHeDriftsHisSnowBackAndBringsNoKin`; the blocks, `GlacierHallGameTests.theHallsSnowAndIce`. Client, `YetiKingClientGameTests`: the lake as placed; each slam changes only drift snow to glare ice; a fury slam inside the north-west column's trampled ring; a real unload, the snow drifted back; his fall restores the lake as placed |
| 7 | Cleanup (adds, temporary ice) | Server: `hisKinComeFromTheDensAndFleeWhenHeIsLeftAlone`, `leftAloneHisIceIsGoneAtOnce`, `whenHeFallsHisSnowDriftsBackAndHisKinAndIceAreGone`, `loadedAgainMidFightHeDriftsHisSnowBackAndBringsNoKin` (none of it is saved), `loadedAgainHeCountsNoKinOrIceFromBefore`. Client: the real unload and his fall (`YetiKingClientGameTests`); `LairClientGameTests` step 7b (he goes with his instance at a restart) |
| 8a | Reward across death | Server: `YetiKingGameTests.supportSharesHisLootOnce` makes one real `ServerPlayer.die` on a mock player, so `AFTER_DEATH` fires; the killing blow is not real, and the hand-over is called directly. Also `aKillingBlowAloneIsPaid` and `aPetsBlowIsItsOwners` |
| 8b | Disconnect | Server: only marked (`Encounter.left` on a test player in `supportSharesHisLootOnce`, then `welcome` called directly). Client: a real rejoin, `LairClientGameTests` step 9 (`beforeRestart`, `afterRestart`): the real join after a real save and reopen pays a kept `yeti_king` Due once |
| 8c | Save | Server: `LairEncounterGameTests.aKeptYetiKingRewardIsSavedAndPaid` and `keptRewardsSurviveASave`; `YetiKingGameTests.aFightLoadedAwakeIsRecordedAfresh`. Client: his real unload and reload (`YetiKingClientGameTests`) |
| 8d | Restart | Client: `LairClientGameTests` step 7b (`Lairs.reset` closes the hall, he goes with it, and a kept Due is paid at home, never in the closed hall) and step 9 (a real save and reopen with the player inside the hall) |
| 8e | Return, and paid exactly once | Return: `GlacierHallClientGameTests.endToEnd` (the hall's edges, out through the Grey Mist, the whirl back in, a closed hall sends the player home); `YetiKingClientGameTests` (the Grey Mist after his fall; a fresh instance has none). Once: the second `die()` in `supportSharesHisLootOnce`, `fourPlayersAreEachPaidOnce` (his health capped at 1024, B-P59), `LairEncounterGameTests.aKeptRewardIsPaidOnce`, and the `!handOver` checks in step 9 |
| 9 | Access in ordinary and designed worlds (09:90) | Client: `GlacierHallClientGameTests.endToEnd`; `WorldDesignerClientGameTests.lairsOpen` (a world made in the creation screen with the designer's generator, then reopened; not one built from an exported design file). Server, the refusals: `GlacierHallGameTests.theHornNeedsTheNightAndTheSnow`, `theHornIsNoHalloweenRite`, `theHornUsesNothingWhenNoLairCanOpen`, `everyRefusalKeepsTheHorn`, `theHornIsAnsweredOnAnyOfItsGroundAndOnPeaceful` |
| 10 | The non-boss cold route | Winter Brine: `ConcordanceAlchemyGameTests.winterBrineNeedsTheColdBand`, `oldRimeBottlesStillGiveResistance`, `aRimeFormulaWrittenBeforeBrewsFrostward`, `frostwardIsNeverCheaperThanItsSnow`; `ConcordanceWinterGameTests.frostwardClearsFrostAndKeepsItsTime`; `YetiKingGameTests.aFrostwardDrinkerIsNotChilledByTheYeti`; `EncyclopediaGameTests.theGlacierHallsRiteNamesWinterBrine`. The kit: `ConcordanceWinterGameTests.theTravelKitIsMadeAndWornAsDesigned`, `everyPieceKeepsPowderSnowFromFreezingItsWearer`, `theWaterFociLengthenPreservesHold`, `theWaterBangleBracesARimekeeperEveryThirtySeconds`, `theWaterBanglesBraceIsNobodysCast`, `spellPowerEnchantmentsAreRefusedOnTheWaterSets`; `YetiKingGameTests.aKitWearerIsSlowedButNotHurtByHisFrost`; `ConcordancePersistenceGameTests.theTravelKitSurvivesASave`; `ConcordanceGameTests.theFirstLoadoutIsMadeInSurvival` (`FIRST_LOADOUT_RECIPES`); `check_rime_kit`; `concordance_journey.py`. The horn's survival recipe (leather, gold, snow blocks and a goat horn; `recipe/frost_horn.json`): `EncyclopediaGameTests.theGlacierHallsDossier`. The Mitten stays the prize: `hisFrostAndTheYetiMitten`, `woolAndLeatherStopTheFreezeButNotHisFrost`. Client: `ConcordanceWinterClientGameTests` (Frostward's HUD; the sets worn; his frost on the real player) |
| 11 | Practise Rime and Strata (09:86, 09:96, 09:112) | Server: `ConcordanceRimeStrataGameTests` (15 tests, from `rimeAndStrataTeachTheirInvocations` to `rimeAndStrataResearchSurviveASave`, `preservationCannotWrapArbitraryInventories` among them); `ConcordanceEffectGameTests.rimebindThawsBeforeItBindsAgain`, `repeatedSlowDoesNotPermanentlyImmobilize`, `onlyTheYetiAndTheTyrantResist`, `braceHoldsTheWholeBlow`, `braceIsTakenBeforeTheBlow`, `harmfulPushesSpendTheBraceAndHelpfulOnesDoNot`, `braceDoesNotCancelBoundaryEnforcement`, `rimeAndBraceSurviveASave`, `preserveNeverRemovesABlock`, `preserveIsRefusedWhereItsCasterMayNotBuild`; `ConcordanceInvocationGameTests.focusIsChargedOnce`, `braceDoesNotShortenFlashstep`; `ConcordanceProgressionGameTests.winterAndStoneAskOnlyForFirstLight`, `oneExaminationServesAssayAndStratawrighting`, `aRimekeeperReachesAdeptByTheGlacierHall`; `ThalliteGameTests.thalliteIsAStrataSpecimen`; `ConcordanceWinterGameTests.aFrostBoonSlowIsElectiveRimePractice`, `aFrostBoonStaysOutsideTheThaw`, `thalliteStaysOutOfTheCold`. Client: `ConcordanceRimeStrataClientGameTests` (the icons, the spell bar, the status icons); `GlacierHallClientGameTests` (arriving counts as the field study; a braced player is still moved by the toll, the mist and the whirl) |
| 12 | Saved data | Server: `ConcordanceRimeStrataGameTests.rimeAndStrataResearchSurviveASave`; `ConcordanceEffectGameTests.rimeAndBraceSurviveASave`; `ConcordancePersistenceGameTests.theCrucibleKeepsPreservesHoldThroughASave` and `theTravelKitSurvivesASave`; `ConcordanceAlchemyGameTests.oldRimeBottlesStillGiveResistance`; `LairEncounterGameTests.aKeptYetiKingRewardIsSavedAndPaid` and `aKeptCinderTyrantRewardIsSavedAndPaid`; `YetiKingGameTests.aColumnIsElectiveRimeworkForItsBaiterOnce` (the elective key through a save) |

These are separate automated tests, each from its own fresh world; no single test walks one player from a fresh world
to a win over the Yeti King, and no person has played the route.

### Client evidence

What the B-11 shots show in run 38591108210, from the 480×270 previews in the job logs (the full-size images are the
run's `client-screenshots-<shard>` artifacts, not downloaded). A screenshot shows presentation; the tests' checks are
the evidence of behaviour.

| Contract item (B-11) | Shots (shard) | Seen |
|---|---|---|
| The three spell icons | `jugcraft_rime_strata_spell_bar`, `jugcraft_rime_strata_spell_bar_scale1` (3) | At CI's default scale the bar's first slot is still cut by the screen's left edge and slots 2 to 6 are whole (B-P60). At GUI scale 1 all six slots stand whole on the screen, left of the hotbar, each with its key number above it. The test checks the icon files |
| The status icons in the HUD | `jugcraft_rime_strata_status_hud` (3); `jugcraft_rime_kit_bangle_brace` (4) | The bangle's shot has no toasts now: Braced's icon (the cream face in a teal frame, as `braced.png`) stands alone at the top right, drawn darker than in unit 3's shot; two chat lines report the research. No purple shape is in this shot; the right edge holds the first-person hand, large. `_status_hud`: Braced's icon above a blue one, Rimebound's, at the top right |
| The three sets worn, front and back | `jugcraft_rime_kit_{front,three_quarter,side,back}`, `jugcraft_rime_kit_close_{…}`, `jugcraft_rime_kit_player`, `_player_sneaking`, `_player_sneaking_back` (4) | Not viewed for this run; run 38549876709's are described in [unit 4's record](arcane-concordance-winter.md#checks-run-for-the-unit). The test passed |
| The foci and bangle in their slots | `jugcraft_rime_kit_slots` (4) | The survival inventory: the paper doll in the player's own skin (the test took the armour off before), the four armour slots empty, and to the doll's right a column of four slots showing grey outline icons, the last the offhand's shield. No item shows in any of them, so the shot shows neither the Focus of Water nor the Water Bangle, as the record warned Trinkets may draw one slot of a group until it is hovered. `wearKit`'s check and the focus's hold (`FOCUS_HOLD_TICKS`) remain the proof that both are slotted |
| The Yeti charging into a column and reeling | `jugcraft_yeti_king_charge_column`, `jugcraft_yeti_king_reeling` (2) | Running: from the lake's edge, blue ice in the foreground, he is small and grey on the snow left of the centre, leaning into his run; the north-west column rises to the vault right of the centre, with the sandy ramp at the right edge and icicles hanging from the vault. Reeling: closer and low, he stands grey and leaning beside the column's stepped blue foot, with small orange star-shaped sparks above him (his stun's crit particles, which the code sends 3.6 blocks above him) and a few snowflakes; at this size his head and crown cannot be made out. The test's checks (the crash at (26.5, 28.8), the player's deed) passed |
| The two dossiers (useful) | `jugcraft_encyclopedia_expedition_glacier_hall`, `_cinder_kiln` (3) | Not seen: shard 3 took 1124 shots, and the part of its log that could be read (its last 1500 lines) holds only shots 644 to 1123. The test, which scrolls Expeditions to each heading and checks it is shown, passed |
| The home's route cards (B-P58) | `jugcraft_encyclopedia_home`, `_home_end`, `_home_scale1` (1) | `_home`: both cards' text runs to the scrolling body's lower edge and is cut there, with a scroll bar at the right: the scrolling, as expected. `_home_end`: scrolled to the end, each card's last paragraph and link are whole, "Open Machinery and Technology ›" on one line and "Open Magic: the Arcane Concordance ›" wrapped onto two. `_home_scale1`: both cards whole, with no scroll bar to be seen, each with its three paragraphs and its link on one line. Merging main f904df3fd (#415's book-style guides directory) on 23 October 2026 replaced these cards; see [Known defects and limits](#known-defects-and-limits). |

The same shard's `jugcraft_encyclopedia_technology` and `_magic` show the top of each guide, its milestones' second row
cut at the scrolling body's lower edge beside a scroll bar; the chapter lists lie below and are not in view.

### Not run

A person playing any of it, or looking at the screenshots; two clients in one fight, or four people; a real disconnect
mid-fight (`Encounter`'s LEAVE path) and a real respawn after a real death by his blow; a world built from the file
`jugcraft design export` writes (the client test designs its world in the creation screen); a restart while he is
awake (step 7b and step 9 restart with him asleep, and his fight record is unsaved by design); a bow or gun solving the
column loop (04:194); his timings against guns and mobility items, how long a fight lasts, and sliding on glare ice;
the foci and bangle drawn on the body (deferred); GeckoLib holding his wind-up pose through Rimebind's hold (no shot
shows a hold); whether the longer codex pages fit Modonomicon's pages (no audit or shot).

### Known defects and limits

- The four-player cap (B-P59): four or more players ask 1050 health of the Yeti King and 1100 of the Cinder Tyrant; the
  game caps a creature's health at 1024, the tests expect the cap, and the scale is unchanged.
- The spell bar (B-P60): at CI's default scale Spell Engine's bar runs off the screen's left edge with 5 or more spells
  (First Light, Rime and Strata make 6), and at 1920×1080 with 8 or more; at GUI scale 1 all six fit. Jugcraft changes
  no Spell Engine setting.
- Spell Engine keys only the first 9 of the 13 invocations, so a player who learns more than nine loses the last ones
  from the bar (the #250 order question; [unit 3's record](arcane-concordance-rime-strata.md#rollout-and-open-questions)).
- Winter Brine's bottle is named "Draught"; "Winter Brine" appears only in JEI, the codex and the dossier.
- The Expeditions page reaches its 12-grid limit with four keepers; a fifth needs a layout change.
- Units 3, 4 and 5's as-built limits are under Risks above (among them: a sneaking player cannot throw a snowball,
  as sneak-use in the air examines; recasting Preserve holds a crucible indefinitely; "snow-safe" boots are
  freeze-proof, not powder-snow walkers; Brace now matters against him, the Water Bangle included; the Brace deed is
  cheap; the hold's edges).
- The slots shot shows neither the focus nor the bangle, and the dossier shots were not seen (above).
- Whether the guides' chapter lists (Chemistry, the Sky) are cut at CI's size is not seen: the `_technology` and
  `_magic` previews show only the top of each page (#322's code, outside Chunk B).
- Outside Chunk B, found by this run: the nuclear site clash on main (`reactorBuildingsKeepRadiationIn` and
  `mediumPlantRelayRoomReadsTheReactor` at one offset) and the intermittent `PeepoDeliveryClientTests.foodBreak`
  (#357, since merged into main and this branch, fixes it).
- **The home's route cards merged out (23 October 2026):** merging main f904df3fd (#415's book-style guides directory)
  replaced the B-11 home cards; the card fix (`d22a55830`), its `check_mod_data` check and the `_home_end` and
  `_home_scale1` shots were dropped in the merge. On main's directory the home is two title tabs, and a route's overview
  is a wrapped paragraph in a scrolling page, which `check_encyclopedia_routes` now requires. Calculated, not yet seen
  in a screenshot: the home tab (`BookArt.Tab`) cuts its label with "…", and at CI's 427×240 GUI it leaves 134 pixels
  for a title, so "Magic: the Arcane Concordance" (about 158 by the font's widths) will be cut there. The dossier
  test (`EncyclopediaClientGameTests`) now opens Expeditions with `showPage` and each dossier's section with
  `showSectionContaining` before scrolling to it, as main's sectioned book requires.

### Where to resume

Every unit is implemented and its tests passed in CI (the candidate `25644502a`, run 38591108210). Nothing is merged.
The owner's checkpoint is awaited: movement feel (being thrown, a Brace halving a throw, the 4-tick hold, the
charge-and-column loop), difficulty (B-P59), the spell bar's place (B-P60) and the next school (Chunk C or Tempest).
Every B-P decision remains Proposed; B-D0 to B-D3 are the owner's. Chunk C had not started then (since built:
[its contract](concordance-chunk-c.md)). Before more work, refresh main and the open pull requests (#357 among them).

## Deferrals

- Ember and Tempest counters in the Yeti fight; Rime footing at authored anchors (04:101).
- Words a player can compose for Rime and Strata, and "Winter's Courtesy" as a composed example (02:94).
- A Cinder Tyrant objective deed; the sluice records no turner (B-P4, confirmed in B-01). One would need the turner
  noted as the wheel turns (a transient hook like `Lairs.onRelit`), a key for each turn, and credit only for a player
  already in his fight.
- Slag, burning and freezing as a fight's hurt (B-P5): if the owner wants them to count, an opt-in hazard rule in
  `Encounter`, narrowed to the Tyrant's surge cells and to burning soon after one of his blows, as its own ticket.
- Thallite slices 3 to 5: arms, the arrow and horse armour, the Thallite Focus, Earthen Steps and Bulwark.
- The owner's earth sets (Nymph's, Geomancer's, Wealdguard's) as Strata gear.
- Strata's civic-project and structural-boss-opening step (02:144). (The Yeti column elective of Rimekeeping, 02:137,
  was built with B-09 in unit 5, B-P56.)
- Syncing the Yeti's elapsed fight time to late joiners (08:127).
- One grace shared by every Concordance and trophy slow (an owner question, B-P10).
- The Mire Hag and the Crypt Lich on the encounter credit (both on main through #352 with damage-only rewards; outside
  Chunk B).
- The Reagent Cabinet, Stillwater Keeping, and Root and Rivet (Chunk C: 06:119; 09:132).
- The Ice Breaker's anti-slip from the owner's text, and the Winter Crown pattern's Fire Resistance attunement *(survey)*,
  left as they are.
- From unit 4 ([record](arcane-concordance-winter.md#rollout-and-open-questions)): the owner's light armour model (no
  light-layout water sheet for it); the Caster Tome of Water; the foci and bangle drawn on the body; the owner's on-hit
  bangle (B-P49's alternative); boots that walk on powder snow (B-P46's alternative, a mixin); a Rimekeeping elective
  for bottling Winter Brine (it would change `CrucibleBlockEntity`); naming the draught "Winter Brine".

## Where to resume

Units 1 to 5 are implemented on branch `claude/chunk-b-winter-stone`, not merged, and every unit's tests pass in CI:
unit 5's candidate `25644502a` passed every job of run 38591108210 on its second attempt (the first failed only two tests
outside Chunk B; [Unit 5: evidence and handoff](#unit-5-evidence-and-handoff-b-09-b-11)). Units 1 to 4 passed
(run 38549876709 on `b012875ab`, every job; units 1 to 3 also in run 38543250503 on `1266c905a`, and units 1 and 3 in
run 38519234934 on `06dc5ea5f`; see the Validation plan). Unit 1, in four commits and two fixes: B-01, the Yeti King on
`Encounter` and `LairRewards` with both payout defects fixed and the column deed, then the Cinder Tyrant the same way,
with no deed (B-P4); B-02, the Glacier Hall's dossier and the Yeti King's JEI pages, with the lair step done by any
keeper's first win and the generalised `check_acquisition` and `check_encyclopedia`, then the Cinder Kiln's dossier and
the Cinder Tyrant's JEI pages (B-D1), which fill the Expeditions page's 12 grids. Unit 3 (B-05 to B-07): see
[its record](arcane-concordance-rime-strata.md#verification); after its CI, its status shots clear the advancement
toasts (shown in runs 38534127130 and 38549876709). Unit 2 (B-03, B-04): tests for the Glacier Hall's access, refusals,
exit and restart and for the Yeti fight, with three fixes to the fight (End-gun Levitation lifts neither him nor his
whelps; a charge into a column's foot stuns him; his ice and whelps last only while he counts them his; the last two
found by its first CI run, 38534127130); records in [yeti-king.md](yeti-king.md#verification) and
[glacier-hall.md](glacier-hall.md#verification). Its owner questions are B-P37 (Peaceful) and B-P38 (the column's
reach). Unit 4 (B-08 and B-10), in four commits, its records and the review's fixes: Winter Brine and Frostward; the
Rime travel kit; the Frost-boon practice with Thallite and the trophies unchanged; the Glacier Hall's preparation fact,
on unit 1's dossiers ([its record](arcane-concordance-winter.md#checks-run-for-the-unit), with run 38549876709's results
and screenshots; its bangle shot was covered by toasts, on B-11's list, and since unit 5 shows Braced). Unit 5 (B-09 and B-11), in four commits, its
records and the review's fixes, passed in run 38591108210: Brace and Rimebind against the Yeti King (`25be6e227`); his
column as Rimekeeping's elective cold work, one kind with the Frost boon's slow (`9cb934ec7`, B-P56); the encyclopedia
home's route cards shown whole (`d22a55830`; dropped when merging main f904df3fd, #415, on 23 October 2026); and the client evidence, with the bangle shot's toasts cleared
(`fc9b404dd`); see B-09 and B-11 as built and its evidence section. Next: the owner's checkpoint, reported to the owner
and not kept here, which asks about movement feel, difficulty (B-P59), the spell bar's place (B-P60) and the next
school (Chunk C or Tempest). Open for the owner: B-P2 to B-P5; B-P10; B-P30 to B-P34 and "Stonebrace" as Brace's
display name ([unit 3's record](arcane-concordance-rime-strata.md#rollout-and-open-questions)); B-P36 to B-P38; B-P40 to
B-P60 (B-P39 is free). Chunk C had not started then (since built: [its contract](concordance-chunk-c.md)).
#352 has merged, so the base is main; refresh main and the open pull requests first (#357 among them).

## Decisions

Chunk A's decisions are unchanged by these answers: A-D1 is Accepted (owner), A-D2 to A-D13 and A-D15 remain Proposed,
A-D14 is Superseded and A-D16 Open (`concordance-chunk-a.md:164-181`).

| ID | Date | Decision | Status | Affects |
|---|---|---|---|---|
| B-D0 | 13 October 2026 | Chunk B next ("Ok lets begin Chunk B"); its commits on the stacked branch `claude/chunk-b-winter-stone`, on Chunk A (#337) | Accepted (owner) | All of Chunk B |
| B-D1 | 13 October 2026 | "Yes, include him (Recommended)": the Cinder Tyrant joins unit 1 (support credit, kept rewards, dossier, JEI pages) as a separate commit; his two payout defects are fixed either way | Accepted (owner) | B-01, B-02 |
| B-D2 | 13 October 2026 | "Use your water sets as Rime (Recommended)": the owner's water sets in the library (Winterguard's, Aquamancer's, Nereid's, with the water focus, lesser water focus and water bangle) become Rime's winter gear, the plan's "Rime travel kit" (02:168: preservation focus, snow-safe boots, a short Brace accessory; resist environmental freezing without permanent boss immunity), made in survival like Ember's regalia; names and art stay the owner's; no stat inflation | Accepted (owner) | B-10 |
| B-D3 | 13 October 2026 | "A timed brew (Recommended)": Winter Brine from the Alembic Crucible gives a timed Frostward status that the Yeti King's frost respects; the Yeti Mitten stays the permanent prize | Accepted (owner) | B-08 |
| B-P1 | 14 October 2026 | The trophy stays one of the two at random (`BOSSES.md:106-112`); 04:135's "choice" is stale | Proposed | B-02 |
| B-P2 | 14 October 2026 | The dossier wording every lair shares (`won`, `after_win`, `made_from`, `owed`, `credit.wards`, the route's `support.credit`) becomes neutral ("the keeper"); JEI's pages are each one boss's, so they keep that boss's pronoun (her, his) and the ten existing pages stay as they were | Proposed (refined 14 October 2026 in B-02) | B-02 |
| B-P3 | 14 October 2026 | The column deed: key `column <player>`, one per baiter per fight, for the player the charge was aimed at as he set off, only if they already had a part (a share) then; being struck by his fight is not a part | Proposed (revised 14 October 2026 from "moved 2 blocks or more during the wind-up", which a late arrival walking in would pass) | B-01 |
| B-P4 | 14 October 2026 | No Cinder Tyrant objective deed in Chunk B: nothing records who turned a sluice (`SluiceGateBlock.java:46,243-259`; `CinderTyrantEntity.java:1050-1062`) | Proposed | B-01 |
| B-P5 | 14 October 2026 | Harm with no attacking creature (slag, fire ticks, freezing) stays outside the fight's hurt (`Encounter.java:132-134`). Applied to both bosses in B-01, with no change to `Encounter`; pinned for the Tyrant by `CinderTyrantGameTests.healingHisSlagAndBurningEarnsNothing` | Proposed | B-01 |
| B-P6 | 14 October 2026 | Authored invocations only (13 of 16); no words a player composes for Rime or Strata | Proposed | B-06, B-07 |
| B-P7 | 14 October 2026 | Brace's consenting ally is the party (`here allies`) | Proposed | B-06 |
| B-P8 | 14 October 2026 | Only the Yeti King and the Cinder Tyrant join `#jugcraft:concordance/resistant`; Vesperine and Madame Tatterlace do not | Proposed | B-06 |
| B-P9 | 14 October 2026; revised 14 October 2026 (unit 3) | Rimebind: `rimebound`, 20% slower for 60 ticks; a hidden `rime_thaw` refuses a recast for the binding's own time plus 40 ticks: 100 normally, 70 on a resistant creature (bound 30) | Proposed | B-06 |
| B-P10 | 14 October 2026 | The thaw grace covers Rimebind only; the trophies' Frost boon (accepted), `dazzle`, DAZE and the Ice Breaker wave keep their own slows. The alternative, for the owner: one grace for every Concordance and trophy slow. Unit 4's `aFrostBoonStaysOutsideTheThaw` is the evidence for today's rule; the recommendation is to keep their own | Proposed | B-06, B-10 |
| B-P11 | 14 October 2026; revised 14 October 2026 (unit 3) | Brace: +0.5 knockback resistance for 100 ticks, not stacking, spent by the first push it meets and bracing the rest of that tick (B-P30) rather than one hit | Proposed | B-06 |
| B-P12 | 14 October 2026; revised 14 October 2026 (unit 3) | Brace's order: a Brace is spent by the first push it meets (a blow vanilla knocks back, a harmful Concordance push, or a scripted push through `Brace.spend`, as the Yeti's in B-09) and braces the rest of that tick (B-P30), so the Yeti may call `Brace.spend` before or after his blow's `hurtServer`; a helpful push (one's own Flashstep) neither uses it nor is shortened by it | Proposed | B-06, B-09 |
| B-P13 | 14 October 2026 | Preserve: `touch struck`, the crucible only, holding its temperature band for 60 s | Proposed | B-07 |
| B-P14 | 14 October 2026 | No Thallite Focus in Chunk B (it stays in Thallite slice 5); the crafted upgrade (09:94) is the water foci | Proposed | B-07, B-10 |
| B-P15 | 14 October 2026; revised 14 October 2026 (unit 3) | Stratawrighting reads its own `#jugcraft:strata_specimens` (granite, diorite, andesite, tuff, raw Thallite), nested inside `#jugcraft:assay_specimens`, so one examination serves both entries. Cobblestone, stone, cobbled deepslate and deepslate are never specimens (sneak-use in the air would examine a bridging block), nor is the Thallite ingot | Proposed | B-05 |
| B-P16 | 14 October 2026; revised 14 October 2026 (unit 3) | Rimekeeping understood by bench study, by the winter survey gated by First Light alone (arriving in the Glacier Hall, or bottling a Rime brew by hand), or by notes; mastered by three kinds of cold work (Preserve a crucible holding a mixture while cold, and while warm or hotter, and Rimebind a hostile creature), which stand for the plan's preserve and compare steps; the Yeti column charge joins as an elective with B-09 | Proposed (the column elective built in unit 5, sharing the Frost boon's key: B-P56) | B-05, B-09 |
| B-P17 | 14 October 2026 | Winter Brine needs no salt: a snowball carries the Rime, and salt has its own feature switch (`config/JugcraftConfig.java:24`) | Proposed (built as proposed in unit 4) | B-08 |
| B-P18 | 14 October 2026 | A stir above the cold band keeps part of the dissolved Rime: warm 600, hot 300, searing 0 per thousand (starting values) | Proposed (built as proposed in unit 4; per stir, B-P41) | B-08 |
| B-P19 | 14 October 2026 | Frostward lasts as Ember's Fire Resistance brew does: 1200 ticks a unit, at most 2400 | Proposed (built as proposed in unit 4) | B-08 |
| B-P20 | 14 October 2026 | The Yeti's knockback respects only Brace, not all knockback resistance: the plan's former B-D3, not asked of the owner. Its source is 04:186's "bracable knockback" (the plan cited 04:184) | Proposed (built in unit 5 for every scripted push of his; a blow's own vanilla knockback and his boulders' and icicles' still read knockback resistance: B-09 as built) | B-09 |
| B-P21 | 14 October 2026 | Rimebind on the Yeti: once per thaw window, his next wind-up is a few ticks longer (set by test) | Proposed (built in unit 5 as B-P54) | B-09 |
| B-P22 | 14 October 2026 | The Brace caster earns a deed, one per occurrence, when a Brace they put on another player absorbs a Yeti push | Proposed (built in unit 5 as B-P55) | B-09 |
| B-P23 | 14 October 2026 | The kit's sets: freeze-immune; no Spell Power or knockback resistance; protection no greater than the Pyromaniac's (light), leather's (medium) and iron's (heavy), toughness 0 | Proposed (built as proposed in unit 4, with the numbers of B-P47) | B-10 |
| B-P24 | 14 October 2026 | The water foci are the preservation focus: Preserve holds 90 s (lesser) or 120 s | Proposed (built as proposed in unit 4; B-P50) | B-10 |
| B-P25 | 14 October 2026 | The Water Bangle is the short Brace accessory, a Brace every 30 s with Rimekeeping understood. The alternative is the owner's own text: a chill on each blow, like the Fire Bangle | Proposed (built as proposed in unit 4; B-P49) | B-10 |
| B-P26 | 14 October 2026 | A Frost boon slow counts as Rime practice, and Rooted holding as Strata practice; no numbers change | Proposed (built as proposed: `rooted` in unit 3, `frost_boon` in unit 4, its key `elective` since B-P56) | B-05, B-10 |
| B-P27 | 14 October 2026 | The Frost Knight and Wight King sets stay creative-only (continuing A-D10) | Proposed (kept in unit 4) | B-10 |
| B-P28 | 14 October 2026 | Nothing is added to `earthen_ground`; no Thallite or trophy number changes | Proposed (built as proposed in unit 4; `check_winter_links` and the record's diff) | B-10 |
| B-P29 | 14 October 2026 | Winter Brine's dossier fact is text and icons, with no recipe grid | Proposed (built as proposed in unit 4; B-P45) | B-02, B-08 |
| B-P30 | 14 October 2026; revised 14 October 2026 (unit 3 review) | A spent Brace braces the rest of that server tick and goes at its end, so the extra knockback of sprints, weapons and mob attacks after `hurtServer` is braced too; two blows in one tick are both braced. Spent between ticks (a push in a cast's packet), it goes when the next tick starts. The Leaf Blower's stream spends it. Accepted exception: vanilla's knockback without a damaging blow (a mace smash's bystanders) is braced but does not spend it | Proposed | B-06, B-09 |
| B-P31 | 14 October 2026 | The owner's icons and sounds byte for byte at their own sizes: Rimebind `freeze.png`, Preserve `extend_time.png`, Brace and Braced `gravity.png` (the owner's own pair), Rimebound `freezing.png`, the Thaw `soaked.png`; Brace's release `gaia_1` to `gaia_4`; the other cues drawn. It replaces the earlier `shield`, `shielding` and `frozen`, scaled to 32 | Proposed | B-06, B-07 |
| B-P32 | 14 October 2026 | The benchmark models Rimebound and its thaw, starts no cast that would change nothing, casts a harmful defense at any foe in reach, and measures knockback taken (never moved), with two acceptance checks | Proposed | B-06 |
| B-P33 | 14 October 2026 | Roles: Rimebind defense, Brace support, Preserve utility | Proposed | B-06, B-07 |
| B-P34 | 14 October 2026 | Rimebind's targets follow the shared boundary (monsters; creatures the caster could strike by hand; players only under PvP and outside the party), and only a hostile creature at normal tolerance counts as restraint | Proposed | B-05, B-06 |
| B-P35 | 14 October 2026 | Preserve's hold is a fixed Java constant (60 s, not scaled by Spell Power), restarts on a recast and never adds, runs on the world clock, and is capped at 2400 ticks so B-P24 fits | Proposed | B-07, B-10 |
| B-P36 | 14 October 2026 | The Glacier Hall's dossier names the Seasonal Stall's snow blocks (8 for 3 Jugs, under its winter and December themes) as a sale that helps with the horn, shown only where a town stands and while it is on sale | Proposed | B-02 |
| B-P37 | 14 October 2026 | On Peaceful the Frost Horn is still answered at night on snow: it is used up and opens the hall empty, because the Yeti King is a monster and Peaceful removes him (`FrostHornRite.check`; `yeti-king.md`). Pinned by `GlacierHallGameTests.theHornIsAnsweredOnAnyOfItsGroundAndOnPeaceful`. The alternative, for the owner: refuse the horn on Peaceful and keep it, which would need a new refusal and its words, the same in the other rites, and the test reversed | Proposed (unit 2; nothing changed) | B-03 |
| B-P38 | 14 October 2026 | A charge stuns him at a column when a step would bring him within 5.2 blocks of its centre, so a charge begun that near one can end on its first step though its line passes the column; and (D1, built in `215a1c1e0`) when a step within a block more would put his body against the column's flared foot, so a glancing run stuns him instead of sliding round it. The alternative, for the owner: only a step that brings him nearer the column stuns him | Proposed (unit 2; the 5.2 rule unchanged, D1 built) | B-04 |
| B-P40 | 14 October 2026 | Frostward clears frost in its own effect tick, on any creature a salve reaches and from any source; never Slowness, never sinking | Proposed | B-08 |
| B-P41 | 14 October 2026 | The heat rule acts on stirs only: ground Rime keeps its ready share until stirred, and additions record no band (the formula format is unchanged) | Proposed | B-08 |
| B-P42 | 14 October 2026 | Every Rime brew (snowball, amethyst, gloamcap) gives Frostward; no brew gives Resistance any more; bottles already made keep it | Proposed | B-08 |
| B-P43 | 14 October 2026 | Frostward's icon is the owner's `frozen.png` (their "Frozen", used for its opposite); alternative `shielding.png` | Proposed | B-08 |
| B-P44 | 14 October 2026 | Winter Brine needs only the Alembic Arts, not Rimekeeping; JEI's Rime ingredient pages name Frostward as a property fact | Proposed | B-08 |
| B-P45 | 14 October 2026 | The Glacier Hall's preparation fact sits in the rite section (shown while a Frost Horn is carried), with a codex link at the Alembic Arts observed; Mitten-only when the Concordance is off or the data's Rime brews no Frostward. Built on unit 1's B-02 | Proposed | B-02, B-08 |
| B-P46 | 14 October 2026 | "Snow-safe boots" means freeze-proof: vanilla 26.3 limits walking on powder snow to leather boots, and there is no mixin. Alternative: a mixin reading `#jugcraft:powder_snow_walkable_boots` | Proposed | B-10 |
| B-P47 | 14 October 2026 | Set numbers: Nereid's 1/2/1/1, ×7, enchantability 15, wool; Aquamancer's 1/3/2/1, ×10, 15, wool; Winterguard's 2/6/5/2, ×15, 9, iron. Toughness and knockback resistance 0; no Spell Power; not fire resistant | Proposed | B-10 |
| B-P48 | 14 October 2026 | Recipes are Ember's patterns with a cold accent (snowball, lapis, snow block); Winterguard's is iron armour's iron plus a snow block. Alternative: Aquamancer's in wool and gold | Proposed | B-10 |
| B-P49 | 14 October 2026 | The bangle's Brace is Brace's own (100 ticks, exclusive), after 600 ticks with no Brace from any source, looked at once a second. The first look only starts the count; nobody's cast; a second bangle adds nothing; not saved; needs Rimekeeping understood and the Concordance on. Alternative (B-P25's): the owner's chill on each blow | Proposed | B-10 |
| B-P50 | 14 October 2026 | The foci hold 1800 and 2400 ticks, beyond the Initiate's Wand's 1200 (the hold is the crucible's state, cut at 2400); one Spell Focus slot holds a fire or a water focus, never both | Proposed | B-10 |
| B-P51 | 14 October 2026 | The kit reuses Ember's slots, `EmberArmorItem`, renderer and medium model; Ember's writer writes both slices' slot tags (`SLOT_ITEMS`) | Proposed | B-10 |
| B-P52 | 14 October 2026 | Any Frost boon arm counts (the boon, not the line), the creative-only Frost Knight Greatsword included; no survival route is added (B-P27) | Proposed | B-10 |
| B-P53 | 14 October 2026 | `frost_boon` is an elective of `jugcraft:rimework` on restraint's targets (B-P34), gated by Rimekeeping understood (B-10 first said "encountered"); Rimekeeping can then be mastered without casting Rimebind | Proposed (refined by B-P56: the key is `elective`, shared with the Yeti King's column) | B-10 |
| B-P54 | 14 October 2026 | Rimebind on the Yeti holds a wind-up `RIMEBOUND_WINDUP` = 4 ticks (untuned, 04:194): the one under way if it is before its last two ticks, else his next; a binding is seen as his Thaw growing; the hold is set, never added; nothing while he waits on his throne; his reset forgets it; a hold that waits outlives the binding and its Thaw until his next wind-up, which shows it; not saved, so a king loaded mid-Thaw shows that binding once more. His steps keep their pace | Proposed (built in unit 5; `aRimeboundKingWindsUpLongerOnceAThaw` and `aRimeboundChargeKeepsItsPace` passed in CI, run 38591108210) | B-09 |
| B-P55 | 14 October 2026 | The Brace deed needs no earlier part; its caster must be in his fight and able to fight him; the key is `brace <braced player> <game time>`, so two pushes in one tick are one deed; a Brace on oneself, the Water Bangle's (nobody's cast) and one whose caster a relog forgot credit no one. The dossier shows the deed count (`share.deeds()` in `LairDossiers.share`), so Brace deeds raise the number shown there; that is cosmetic, as qualifying is a yes or no (`Encounter.Share.qualifies`) | Proposed (built in unit 5; `aBraceOnAFriendIsTheCastersDeed` passed in CI, run 38591108210) | B-09 |
| B-P56 | 14 October 2026 | Rimekeeping's electives share one key, `elective` (Java `Rime.KEY_ELECTIVE`; unit 4's `frost_boon`, renamed before any release): a Frost boon arm's slow (B-P53), and the Yeti King's charge broken on an ice column, recorded for the player its column deed names whenever the deed's conditions hold (`YetiKingEntity.baited` → `Rime.column`), gated by Rimekeeping understood (the practice's gate, as B-P53; 02:137 says only "elective field evidence") and the Concordance switch. Together they stand for at most one kind: after either the other adds nothing, and with Rimebind's restraint they make two, so every mastery needs a Preserve, so the Alembic Arts, and the bench (or someone's notes) stays mandatory, as unit 3's review asked, and the progression graph's `rimework` route still asks for the Alembic Arts. The alternative, for the owner: each elective its own kind, which lets restraint and the two electives master Rimekeeping without Preserve or the Alembic Arts. A world saved by unit 4's unmerged build keeps a `frost_boon` key as a kind of its own (not migrated) | Proposed (built in unit 5; `check_rime`, `check_winter_links` and `YetiKingGameTests.aColumnIsElectiveRimeworkForItsBaiterOnce`, passed in CI, run 38591108210) | B-05, B-09, B-10 |
| B-P57 | 14 October 2026 | The texts name the counters: the Glacier Hall dossier's credit line (bracing a friend who then takes one of his throws), an "Against the Yeti King" page in the Braced and Rimebound codex entries, and the column in the Rimekeeping entry's Understood page and the Rimekeeper advancement; the player guide's Yeti advice by hand | Proposed (built in unit 5) | B-02, B-09 |
| B-P58 | 14 October 2026 | The encyclopedia home's route cards are sized to their whole overview and link; the link wraps; they scroll in the shared `ComposerScroll` when the window is too small; Tab shows a card from its top; larger windows unchanged | Proposed (built in unit 5, `d22a55830`; `EncyclopediaRoutesClientGameTests` passed in CI, run 38591108210). Superseded: merging main f904df3fd (#415's book-style guides directory) on 23 October 2026 replaced the cards and dropped `d22a55830` | B-11 |
| B-P59 | 14 October 2026 | Four or more players: keep the party scale and the game's 1024 cap on a creature's health (the Yeti King asks 1050, the Cinder Tyrant 1100) until four people have fought them. The alternative, 2.3 times (966 and 1012), is a tuning change made only on the owner's word, with a test | Proposed (for the owner's checkpoint: difficulty) | B-01 |
| B-P60 | 14 October 2026 | The spell bar's place: Jugcraft changes no Spell Engine configuration now. The recommendation for the owner: a Jugcraft default that keeps 9 slots on screen; the alternative: each player's own Spell Engine HUD setting ([unit 3's record](arcane-concordance-rime-strata.md#rollout-and-open-questions)) | Proposed (for the owner; named in the checkpoint's pointer line) | B-06 |

Unit 2's are B-P37 and B-P38; B-P39 is kept free; unit 4's are B-P40 to B-P53; unit 5's are B-P54 to B-P60 (B-P56
came with its column elective, the others with its records; B-P59 and B-P60 are the questions CI raised).

## How this contract was checked

- Re-read at `d4c07bf56` for this page: the Yeti King's fight, rewards, reset, death, hurt, chill, charge, push and boss
  bar; the whelp; the Yeti's registration and loot; the Mitten; the glacial spike; the Tyrant's reward, reset, death and
  hurt; Vesperine's Chunk A pattern and the deed keys; `Encounter`, `LairRewards`, `Lairs` (hooks, restart, edges);
  `ConcordanceEffects` (creature and block paths, tolerance, lasting effects, push), `Tolerance`, the resistant tag;
  `Mixture`, `Band`, `Heat`, `Brew`, the crucible's tick and save; `tools/concordance_alchemy.py`; `ArmItem`'s boon,
  `ArmVariants`, `JugcraftArms`, `Wayfaring`'s wave; `Plan.scaled`, `Compiler`, `ComposedSpells`, `Component`,
  `Ember`; `Baselines` and `Benchmark`; `Hexes`, `Sympathy`'s guard, `EmberGear`; `ConcordanceProgress`, `Assaying`,
  the `assay` research and practice and its generator; `LairDossiers`, `FirstLightRoute`, `PageWriter`;
  `tools/acquisition.py`, `check_acquisition`, the invocation checks, `check_alchemy`, `check_delivery`,
  `concordance_delivery.py`; the Initiate's Wand limits and the Adept's Attunement; the loot tables; the freeze and
  earthen tags; the test methods named; the Thallite, Ember regalia, Cinder Tyrant and Glacier Hall records; the
  owner's water-set files and their names in the library's language file; plan chapters 02, 03, 04, 06, 07, 08, 09 and 10
  at the cited lines; mc263src `LivingEntity` (hurt, death, knockback, freezing) and `Entity.push`.
- Checked again for this page's review on 14 October 2026: every code, data, test and plan citation above at
  `d4c07bf56`; the research-unlock check; `Examination` and the specimen tags; the World Designer client test and
  `world/design/`; the Kiln Seal's rules; Ember part 1's commit (67 files); `ConcordanceSpells`' cost settlement; the
  Jade crucible snapshot; the icon files' sizes and the runtime spell and status icons; the owner's `gaia` recordings;
  `bare`, `drift` and `driftAll`; the Frost Horn recipes; the salt feature switch; the existing invocations' signatures
  and Focus costs.
- Read by REST on 14 October 2026 (the PR list at 00:30 UTC): the open pull requests and their file counts; #352's body
  and record, and at its head the files compared above and the Mire Hag's and Crypt Lich's reward fields; the file lists
  of #323, #330, #343, #351, #354 and #355; #337's check runs on `d4c07bf56`; main's head.
- Taken from the survey reports or the plan's review without re-reading, and marked *(survey)*: #322's conflicts with
  #337; #336's lamp listener; #330's audit; #332's Levitation; #344's contents; the Thallite leaching pull requests; the
  merge points in `tools/concordance.py`; the Conclave check; the context of `pewatermagic.ogg`; the heavy model's
  unchecked fit (since measured by unit 4: 71%, head 91%); whether each listed recipe draws one grid (the Expeditions
  count); what an examination records; the sluice's turner; the Winter Crown pattern.
- Read for unit 4 (14 October 2026), from the 26.3 sources fetched for it (not the local mirror):
  `PowderSnowBlock.canEntityWalkOnPowderSnow` (leather boots or an entity-type tag only: no boots tag) and
  `InsideBlockEffectType.FREEZE` (frost only while `canFreeze()`); B-P46 follows from them.
- Read for unit 2's records (14 October 2026): run 38543250503's job logs by the GitHub API (`mod` and
  `optional integrations absent`, their last 5000 lines, which hold the game tests' summary; `choose client tests`;
  client shards 0 and 4, their last 5000 lines, with the screenshots, viewed as 480×270 previews); run 38534127130's
  results as noted at the time.
- Read for units 4 and 3's evidence (14 October 2026): run 38549876709's nine jobs and their steps by the GitHub API;
  the logs of `mod` and `optional integrations absent` (their last 5000 lines), `choose client tests`, `client`, client
  shard 4 (its last 5000 lines) and shard 3 (its last 1500), with the screenshots viewed as 480×270 previews; the
  gametest `fabric.mod.json` and the `@GameTest` annotations unit 4 adds (`git diff 1266c905a b012875ab`).
- Read for unit 5 (14 October 2026): the Yeti King's push, `fight()`, AI step, `baited`, `reset` and save; the glacial
  spike; `Brace`, `Rimebind`, `Rime`, `RimeGear.brace`, `ConcordanceEffects.apply` and `Encounter`;
  `EncyclopediaRoutesScreen`, `ComposerScroll`, `EncyclopediaScreen.find` and `line`, and `LairDossiers`; the client
  tests it changes; in mc263src the server's tick order (entities before game tests; effects before the AI),
  `Player.isClientAuthoritative` and `canSimulateMovement` (mock players do not move between ticks, as
  `ConcordanceGameTests`, `ArmsVGameTests` and `YetiKingGameTests` already state), `LivingEntity`'s knockback,
  `Font.splitIgnoringLanguage`, `AbstractScrollArea` and `Window.getGuiScaledWidth`; runs 38534127130, 38543250503 and
  38549876709 by `gh`. Not readable in the mirror: `net.minecraft.gametest` (how mock players are made; the order of
  `onEachTick` against `runAfterDelay`, which the tests do not rely on). Unconfirmed until something runs: that GeckoLib
  holds the wind-up pose through the hold, what Trinkets' slot groups draw unhovered, Tab focus inside `ComposerScroll`
  and the cards' narration.
- Read for unit 5's evidence (14 October 2026): run 38591108210's jobs and their steps, both attempts, by the GitHub
  REST API; the logs of `mod`, both attempts' `optional integrations absent` and attempt 1's client shard 4 (their last
  5000 lines), `choose client tests`, and attempt 2's client shard 2 (its last 5000 lines) and shards 1, 3 and 4 (their
  last 1500), with the screenshots viewed as 480×270 previews (shard 3's dossier shots fall before the part read; the
  tests' own log lines, such as the bar's width and the home cards' numbers, come before it in every shard); the
  `@GameTest` annotations unit 5 adds (`git diff b012875ab 25644502a`); and the test methods the demonstration table
  names, resolved from the evidence part's line numbers at `b012875ab` and found again at `25644502a`.
  Settled by checks that passed in that run: the exact crash point at z 28.8, the home cards' title check, and Tab
  reaching a card inside `ComposerScroll`. Seen in it: the slot column beside the inventory's paper doll drew no item
  while the server had the focus and bangle slotted.
- Undetermined until something runs or is read: whether a game-test player can be killed so that a real death event
  fires; the state of the pull requests after 00:30 UTC on 14 October. Unit 2 settled the lair dimensions in a designed
  world, the charge at the dais and walls, and the Yeti under Levitation (B-03 and B-04, as built).
