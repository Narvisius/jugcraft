# Arms IV: the ornate arms

Status: implemented on `claude/arms-iv` (batch 47), awaiting review. Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner, 4 October 2026: "start on the next batch of weapons", with five reference sheets of fantasy weapon sets attached. Three of the sheets are signed AlanizMc9: a gold set, an emerald-and-obsidian set with bow, crossbow and shield, and a violet set. The other two are a gold "Battleblade" with a row of skill icons, and a gold set with armor, bow, crossbow and shield.
Owner: jimbozoomer-byte
Target milestone and tier: bronze (tin) and steel (machines), beside the arms of batches 42 to 46 ([arms.md](arms.md), [arms-ii.md](arms-ii.md), [arms-iii.md](arms-iii.md)).
Primary specialty and supported player role: fighting; the kama and war pick also clear and mine.

## Player experience
Five more kinds, each in bronze and steel, drawn in a new ornate style after the reference sheets. Three swing two-handed (Arms III's committed swings: the blow lands as the swing comes round, on every foe in an arc, with a finishing blow).

| Arm | Hit (bronze / steel) | Attacks a second | Reach | Swing | Trait |
|---|---|---|---|---|---|
| Labrys | 11.5 / 12 | 0.7 | 3.25 | two-handed, 22 ticks, blow at 8; 100°, 3 foes | **Whirl:** the finishing blow sweeps right round you, striking up to 6 foes behind and to the sides as well as in front. Stops a shield for 3 s. |
| Battleblade | 10 / 10.5 | 0.8 | 3.5 | two-handed, 20 ticks, blow at 7; 110°, 4 foes | **Sunder:** each hit wears every piece of armor the foe wears by 4 more. |
| War Fork | 8.5 / 9 | 0.9 | 4.5 | two-handed thrust, 18 ticks, blow at 6; 30°, 2 foes | **Brace:** half again as much damage to a foe coming at you at 2 blocks a second or faster (a charging mob, a rider, a sprinting player). |
| Kama | 4.5 / 5 | 2.0 | 2.75 | 5 ticks | **Clear:** use on grass, ferns, vines or leaves to cut every one of them in the 3 by 3 by 3 about it. Each drops what it drops with a blade (not shears), at 1 durability each. Crops are never cut. |
| War Pick | 6 / 6.5 | 1.4 | 3 | 7 ticks | **Delve:** mines stone and ore as its metal's pickaxe does (bronze as iron, to diamond ore; steel as diamond, to obsidian). |

- **Motion:**
  - labrys: a side swing, then the whirl (wound far round, the axe carried flat all the way across);
  - battleblade: a broad sweep, then a falling cleave;
  - war fork: a thrust, then a driving thrust from a braced stance;
  - kama: quick hooking cuts forehand and back;
  - war pick: a downward peck and a side swing.

  Each has first-person strokes, as batch 43's.
- Handbook: "Arms: Masterworks" (labrys, battleblade, war fork) and "Arms: Kama and War Pick", in the gear chapter. Advancement: **Masterwork** (forge any of the five), after Bronze Age.

## What was learned from the reference sheets
The sheets were studied for their look only. Nothing is traced or copied: the art is drawn by `tools/arms_art.py` from shapes, in Jugcraft's own metals.

| On the sheets | What Jugcraft's new arms do |
|---|---|
| A faceted stone as the focal point where head meets haft or blade meets hilt, with a white glint. | A set stone in a ring of the fitting metal, cut into four lit facets with a glint. Bronze arms take a deep **garnet**; steel arms a **lit green phosphor** cabochon, the dieselpunk green of the gauges (`docs/ART_DIRECTION.md`). |
| Flat tones from a short colour ramp, with a dark core, a lighter face and a bright edge, rather than soft shading. | The new heads are painted in flat steps of each metal's ramp (`flat`, `arc_blade`): dark by the haft, a lighter cheek, a bright cutting edge. |
| Guards that sweep up into winged points; spiked lozenge pommels; dark wrapped grips. | Winged guards (`wing`), lozenge pommels with a spike (`pommel`) and the existing wrapped grips. |
| A lit rim along the edges facing the light. | A rim pass brightens every edge pixel that faces the top-left light. |
| Loose sparkles about enchanted heads. | Two or three small glints of light in the open air about each head, drawn after the outline. |
| Shapes: double-bitted axes, saw-backed cleavers, tridents, hooked sickles, pickaxe-headed weapons. | The labrys, battleblade (with notches cut from its back), war fork, kama and war pick. |
| Bows, crossbows, shields and armor; a weapon with active skills ("The Battleblade"). | **Not in this batch.** Bows and crossbows need a draw-and-loose mechanic and item models for each draw stage. Shields need their own renderer. Active skills would be a system of their own. Each is a candidate for a later batch. |

The earlier arms keep their art. Giving them stones and the ornate finish would be a change of its own.

## Connections
- Existing input producer: bronze ingots (tin), steel ingots (the steel foundry), sticks and leather.
- Existing output consumer: the player against mobs and, where PvP is on, players. The kama clears foliage; the war pick mines.
- Recipes (shaped, `#` the metal's ingot):

  | Arm | Pattern | Cost |
  |---|---|---|
  | Labrys | `#S#/#S#/ S ` | 4 ingots, 3 sticks |
  | Battleblade | ` ##/###/L# ` | 6 ingots, leather |
  | War Fork | `# #/#S#/ S ` | 4 ingots, 2 sticks |
  | Kama | `## /  #/ S ` | 3 ingots, stick |
  | War Pick | `## / S#/ S ` | 3 ingots, 2 sticks |

  None repeats another shaped recipe in the mod (all were compared, mirrored too).
- Technology connection: the batch 25 metal tiers; electroplating plates and repairs them. Magic connection: none.
- Reachable entry path: as the other arms. Required vs optional: optional; craftable solo and tradeable.
- Not to be confused with agriculture's farming sickles (`jugcraft:flint_sickle`, `jugcraft:bronze_sickle`), which harvest crops. That id clash is why the new weapon is the kama. `check_arms` now fails if an arm's id is taken elsewhere.

## Balance and automation
- Per foe and per second, every kind is below its metal's sword (bronze 9.6, steel 10.4), counting the finishing blow over the combo:

  | Arm | Damage a second (bronze / steel) |
  |---|---|
  | Labrys | 9.1 / 9.45 |
  | Battleblade | 9.0 / 9.45 |
  | War Fork | 8.6 / 9.1 |
  | Kama | 9.0 / 10.0 |
  | War Pick | 8.4 / 9.1 |
- **The traits pay only in their situation:**
  - the whirl only on the finishing blow;
  - sunder only against armor (it wears the armor; the blow is unchanged);
  - brace only against a foe closing at 2 blocks a second or more, measured from where the foe was a tick before, on the server;
  - delve only for mining.
- **Clearing** gives no more than cutting by hand, and wears the kama by one a block. It touches only blocks in `#jugcraft:kama_cuts` (data):
  - leaves;
  - short and tall grass, ferns, vines and dead bushes;
  - glow lichen and hanging roots;
  - the dry grasses, bushes and leaf litter where they exist.
- No conversion, no recycling.

## Multiplayer and persistence
- **Server authority:** every trait runs on the server:
  - the bonuses in `getAttackDamageBonus`;
  - sunder in `hurtEnemy`;
  - the whirl in `TwoHanded`;
  - clearing in `useOn`, changing blocks only on the server.
- **Protection:** clearing cuts only blocks the player may change: `mayUseItemAt`, `mayInteract` (spawn protection) and the town's protection. It works once per use, over at most 27 blocks.
- **Saved state:** none beyond ordinary items with stable ids (`jugcraft:<bronze|steel>_<labrys|battleblade|war_fork|kama|war_pick>`). Recipes follow the `tin` and `machines` switches.

## Dependencies and assets
- No new dependencies, mixins, entities or packets.
- Art: ten 64x64 sprites from `tools/arms_art.py`. New helpers: `gem`, `wing`, `pommel`, `flat`, `cut`, `arc_blade`, `rim` and `glints` (drawn after the outline). New materials: `GARNET` and `PHOSPHOR`.
- Motion: `tools/arms_moves.py`.
- A block tag, `data/jugcraft/tags/block/kama_cuts.json`. Its newer vanilla blocks are listed as optional entries.

## Verification
- `python3 tools/check_mod_data.py` (`check_arms`):
  - Java and the tools agree on the kinds, traits, constants and the two-handed table (strike ticks against each kind's animation blow);
  - damage a second stays below the sword;
  - no arm takes an id another generator registers. Tried with an arm named `bronze_sickle` put in on purpose: the check failed on it.
- Game tests (CI job `gametest`, `ArmsIVGameTests`):
  - `aLabrysOrdinaryBlowKeepsToItsArc`: the ordinary blow strikes the husk ahead and not those behind or beside;
  - `aLabrysFinishingBlowWhirlsRightRound`: the finishing blow strikes all four;
  - `aBattlebladeSundersArmor`: a hit wears a husk's iron helmet and chestplate by 4 each;
  - `aWarForkBracesAgainstACharge`: +50% against a husk that came 0.3 blocks towards the wielder; nothing against one going or standing;
  - `aKamaClearsGrassAndLeaves`: a mock player's use cuts a 3 by 3 of grass and the leaves above it, keeps the grass 3 blocks off, and wears the kama by 10;
  - `aWarPickDelvesAsAPickaxe`: it mines stone fast and logs at hand speed; bronze is right for iron and diamond ore but not obsidian; steel is right for obsidian. (The first run expected bronze to stop short of diamond ore; it is iron's tier, which mines diamond ore, so the test was corrected.)
- Client game tests: the racks (now four parts a metal) and frames; each new kind's guard and swing in `ArmsMotionClientGameTests`, and first person for the labrys and kama.
- **Not run:**
  - play;
  - the whirl's and brace's feel against real mobs;
  - clearing other plants;
  - two players.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- All numbers are first values for the owner to tune.
- Next, from the reference sheets: bows and crossbows in the arms' metals, shields, active weapon skills, and the ornate finish on the earlier arms. Each needs the owner's choice.
