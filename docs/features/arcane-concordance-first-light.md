# Arcane Concordance: First Light

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; compiled and server-tested in CI (831 server game tests
pass, see Verification); not yet played in a client or on a dedicated server.
Proposal issue: none; the owner's Arcane Concordance brief is the scope approval (this record describes the proposal).
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 1, Initiate stage (Jugcraft Discovery). Roadmap step 5.
Primary specialty and supported player role: Lampwrights (Radiance), an exploring and lighting role; the lantern is a
tradeable product for players who never study magic.

Architecture, vocabulary and the 32-step checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

1. **Discover.** Amethyst shards (from geodes) are luminous specimens, as are glowstone dust, glow ink sacs, glow
   lichen, glow berries, Jugcraft's glowcap, glimmerbloom and jack-o'-lantern mushroom. Their tooltip says so. Sneak and
   use one to examine it: First Light is **Encountered**.
2. **Observe.** Examined somewhere dark (light 4 or less at eye level), the specimen glows by itself: **Observed**.
3. **Understand**, by any of three routes (the third, reading another player's Research Notes, is in
   [arcane-concordance-sharing.md](arcane-concordance-sharing.md)):
   - the field: observe three different specimens in the dark; or
   - the laboratory: craft a **Lampwright's Bench** (copper, amethyst or glowstone dust, planks, sticks), put a
     specimen in its dish and press Study; after 5 seconds the bench uses the specimen and hands over the notes.
   The codex's Kindle and Kindled Lantern pages unlock, and **Kindle** is learned.
4. **Cast.** Craft an **Initiate's Wand** (amethyst or glowstone dust, stick, copper ingot) and hold it. Kindle
   appears on Spell Engine's spell bar; casting it (0.6 s, with an original gesture) sets a mote of steady light
   (level 14) in the open block you look at, up to 16 blocks away, for 60 seconds. It costs 4 Focus of 20 (one returns
   every 2 seconds) and has a 1.5 s cooldown. Casting in 8 different chunks **masters** First Light, and Kindle then
   costs 3.
5. **Make a lantern.** At the bench, put a plain lantern in the work slot and an amethyst shard in the dish, press
   Kindle: a **Kindled Lantern** holding 8 Radiance. Infuse more specimens (amethyst 8, glowstone 6, glowcap 4,
   glimmerbloom 3, jack-o'-lantern mushroom 3, glow ink 3, glow lichen 2, glow berries 1) or channel 6 Focus for 2.
6. **Use the lantern.** Use it to light or put it out. Lit and in either hand, it keeps a light (level 12) beside you;
   it burns 1 Radiance every 20 seconds while lit, carried or not, and holds up to 64 (21 minutes). Out, it keeps its
   charge. LambDynamicLights, when installed, also makes it glow in hand.

The *Arcane Concordance* codex (crafted from a book and an amethyst shard or glowstone dust) explains each step. A new
player can do all of this without commands. (The bench, the wand, the sconce and the codex take glowstone dust as well
as amethyst since roadmap step 24, so that a world whose geodes are gone or out of reach still has a way in.)

## Connections

- Existing input producer: vanilla geodes, caves and the Nether (specimens); Jugcraft's glowcap, glimmerbloom and
  jack-o'-lantern mushroom (agriculture); copper and planks (base materials).
- Existing output consumer: lighting for caves, mines, farms and builds; the lantern as a trade good. Kindled light
  obeys town protection and spawn protection like placing a block.
- Technology connection: the bench is a hands-on station with no automation in this milestone (no hopper faces);
  automating lantern charging is left to a later Practitioner station so the early work keeps its purpose.
- Magic connection: First Light is the entry point of the research graph (no prerequisites); later Radiance and Echo
  research will require it.
- Reachable entry path: every input is obtainable in early survival (amethyst, copper, a vanilla lantern). Nothing in
  this milestone needs an item produced by a later step. `ConcordanceRules` and `check_mod_data.py` both check for an
  entry point and for cycles.
- Required vs optional: Spell Engine, Spell Power, Modonomicon, Player Animation Library and Cloth Config are required
  dependencies already; Jade, JEI, LambDynamicLights and Mod Menu are optional and only add display. Solo route: both
  understanding routes are solo. Trade route: anyone can carry and light a Kindled Lantern made by someone else.
- How the specialty stays useful without the rest: light is always useful; the lantern needs only specimens or Focus.
- Not applicable: no energy, fluids or machines are involved yet.

## Balance and automation

- Focus: 20 points, one back per 40 ticks (full in 40 s). Kindle 4 (3 mastered): five casts from full.
- Kindled light: level 14, 15 steps of 80 ticks (60 s). Casting on an existing Kindled light refreshes it to full and
  still costs Focus. Light is placed only in air, never replacing anything.
- Lantern: capacity 64 Radiance; 1 per 400 ticks while lit. Making one costs a vanilla lantern and an amethyst shard
  and gives 8. Infusion values are in `tools/concordance.py` (SPECIMENS). Channelling turns 6 Focus into 2 Radiance
  (a loss, checked by the parser and `check_mod_data.py`).
- Conversion cycles: Radiance converts into nothing (it only burns as light), Focus cannot be stored, and specimens
  are consumed when studied or infused. There is no loop that gains anything.
- Study: 100 ticks, uses one specimen, teaches only if it would add evidence (the bench refuses "nothing to learn").

## Multiplayer and persistence

- Server validation: examination is a server-side use callback with a 10-tick per-player limit; the light level comes
  from the server's world. Bench buttons go through vanilla's menu-button packet (container id and reach checked by
  vanilla) and every press is checked again on the server (feature switch, research, contents, Focus, overfill). Casts
  are checked by the Spell Engine gate and again in the impact; Focus is paid once after success. Light placement uses
  the player's block-placement permissions and town protection.
- Ownership: a study belongs to the player who started it; only they can cancel it. Notes for a player who is offline
  when it finishes wait in the bench until they next open it. Anyone may use the workings they know.
- Concurrent use: one study per bench; workings that use the dish wait while it studies; channelling does not.
- Chunk unload and restart: the bench saves contents, study and pending notes; Kindled lights are blocks with
  scheduled ticks; lantern and Focus clocks use game time, so unloaded time is counted when next read.
- Schema: knowledge, Focus and rules carry `schema: 1`; an unknown research state reads as not started instead of
  failing the player file.
- Disable behaviour (`concordance.enabled=false`): everything stays registered and saved; recipes stop, examination,
  study, workings and casts are refused with a message. Lanterns still light and burn (they are ordinary items).

## Dependencies and assets

No new dependency. Uses the approved, pinned Spell Engine, Spell Power, Modonomicon, Player Animation Library, Cloth
Config, Fabric API, and the optional Jade, JEI, LambDynamicLights and Mod Menu ([FRAMEWORKS.md](../FRAMEWORKS.md)).
All textures (`tools/concordance_art.py`), sounds (`tools/concordance_sounds.py`, synthesised), models, animations and
texts are original and generated by Jugcraft's tools.

**Item icons, redrawn at 16×16 (7 October 2026).** When `main`'s item icon rules (`docs/ITEM_ICONS.md`) merged into this
branch, the Concordance's seven flat item icons (the Initiate's and Adept's Wands, the Kindled Lantern unlit and lit,
the Research Notes blank and written, and the codex) were redrawn from their old 64×64 and 32×32 drawings as 16×16 maps
in `tools/item_icons/`, with three new materials in `tools/icon_materials.py` (amethyst, paper and the lantern's smoked
glass). `tools/check_icon_maps.py` passes them with no warnings, `tools/check_mod_data.py` checks that each committed
texture is its map's drawing, and they were looked at at 1× and 2× on light and dark slots. The owner asset library
was searched (lanterns, staffs and a crystal-staff reference sheet); nothing was reused, as the maps were drawn from the
old icons' designs. The owner approved the redraws on 7 October 2026; they have not yet been looked at in the game. Weapons: `data/jugcraft/spell_assignments/` opts every Jugcraft
weapon out of Spell Engine's automatic weapon skills, so they keep their own weapon arts and are no spell casters.

## Verification

Commands and results:

- `python3 scripts/check_repository.py`: pass. `python3 tools/check_mod_data.py`: pass (1440 IDs). It also checks the
  Java constants against `tools/concordance.py`, the spell file's Spell Engine fields, the research graph, the codex's
  research ids and every translation key the Concordance's Java names.
- The pure rules (`concordance/rules`) compiled with JDK 21 and passed a standalone harness locally (parser
  rejections, cycle and orphan removal, state order, duplicate evidence, Focus regeneration and spending).
- Server game tests added (`src/gametest/.../ConcordanceGameTests.java`): rules load clean and a malformed file is
  reported; field evidence through all states with duplicates ignored and a codec round trip; sneak-use examination
  and its rate limit; Kindle through `SpellExecution.performSpell` (refused before research, cast with the wand: mote,
  Focus, cooldown, mastery evidence, codex advancement; no cost during the cooldown, without an instrument, or without
  Focus); mote expiry and trail lights; lantern burn and codec; bench study, cancel and Jade snapshot; bench workings
  and their refusals; wand and weapon resolvers.

CI results (Build workflow, run manually on this branch):

- Run 37495256429 (first push): compile failed on two 26.3 renames (`PushReaction.DESTROY` is `POPPED`; DFU has no
  `Codec.unit`). Fixed.
- Run 37496004568: `mod` job passed. `./gradlew build` compiled main, client and test code and the server reported
  **"All 830 required tests passed"**: the 821 existing tests plus the 9 above. `optional integrations absent` passed
  (the same build and server tests without Jade, JEI, Mod Menu and the other optional mods). The log showed one
  Modonomicon error: the codex's first entry used a non-existent item as its icon, which stops the book opening. Fixed
  (a vanilla book icon), with a new `check_mod_data.py` rule for codex icons and a new game test,
  `codexLoadsWithoutErrors`, which asks Modonomicon for the book's load errors.
- Run 37497884738 (with the codex fix): `mod` passed with **"All 831 required tests passed"** (the 10 Concordance
  tests included) and no Modonomicon load error; the rules loaded with 0 problems. `optional integrations absent`
  passed.
- The three client test shards fail before any test runs: the game cannot create an OpenGL context on the CI runner
  ("Couldn't find matching GLX visual"), then Iris aborts the JVM. The same three shards fail identically on the
  foundation branch this work is built on (run 37496512659 on `feature/frameworks-and-modrinth-pack`, whose server
  jobs pass), so this is not caused by the Concordance; `main` (without Iris and Sodium) passes its client shards.

Not yet run: the client (screen, HUD, cast animation, codex rendering, LambDynamicLights glow), a save-and-reload in a
real world, a dedicated server with two clients, the optional-absent client path, and a player journey from a fresh
world.

## World and event applicability

Specimens come from existing biomes and caves; no worldgen changes. No creatures, bosses, loot or dimensions are
involved. Not seasonal.

## Rollout and open questions

- Known limits: no automation for the bench; Kindle is the only invocation; the PAL cast clip has not been seen in a
  client; Jugcraft's arm animation (ArmsMotion) only drives Jugcraft weapons, which are no instruments, so the two never
  run together today. A future instrument that is also a weapon must decide which owns the arms.
- Pending notes are lost if the bench is broken before its owner returns (the specimen was already used).
- Removing the feature: the ids listed in [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md) are stable. Back up the
  world before removing the mod; players' knowledge and Focus live in their player files.
