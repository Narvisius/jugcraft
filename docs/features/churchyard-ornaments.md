# The Churchyard's Ornaments

Status: implemented in source. Not yet played by hand. The Build workflow compiles it, and CI's game tests and client screenshots are recorded below.
Proposal issue: none. The owner asked for it directly on 4 October 2026 ("also make a bunch more halloween and graveyard decoration use the attached images for references"), with pictures of graveyard props: a gargoyle on its pedestal, skulls and bones, a giant skeleton hand rising from the ground and a purple lantern. This is the first of two batches of them (Halloween decorations batch 15). It is stacked on the graveyard flora.
Owner: @jimbozoomer-byte
Target milestone and tier: Discovery tier. Every ornament is made from bones, bone blocks, stone, iron nuggets, a torch and an amethyst shard. The gargoyle is cut from polished granite.
Primary specialty and supported player role: building. These ornaments are for a churchyard, crypt or catacomb.

## Player experience
1. **Gargoyle** (five polished granite and chiselled stone bricks; two blocks tall): a gargoyle crouched on a granite plinth. It hunches forward over the cornice on its haunches, its clawed forefeet gripping the edge and its bat wings folded high. It has curled horns, a heavy brow over hollow eyes, open jaws for the rain to run out of, and a tail curling down the plinth's back. It is a graveyard monument like the others:
   - it weathers from clean to worn, mossy and overgrown;
   - a brush, honeycomb, an axe and bone meal keep it, as with the headstones;
   - **the plinth's panel takes an inscription** from the Stonemason's Chisel (a motto, or a name);
   - it stirs restless spirits as a grave does.
2. **Bone Pile** (three bones and bone meal): old bones and skulls heaped on the ground, crossing every way, with the skulls staring out. Place more on it to heap it higher, four layers at most (12 pixels), with more skulls as it grows. It needs solid ground and breaks into one pile a layer. Walking over it rattles the bones now and then.
3. **Ossuary Wall** (four bone blocks and four cobbled deepslate make eight): a building block from a catacomb. It has three rows of skulls looking out between courses of long bones laid crosswise, set into a core of packed bone ends. It faces whoever places it.
4. **Giant Bone Hand** (five bone blocks and coarse dirt; two blocks tall): a giant's skeletal hand reaching out of a heap of grave earth. It shows radius and ulna, a knot of wrist bones and the palm's long bones, then four fingers of three joints each and a thumb, spread and clawing. **Give it a redstone signal and it clenches into a fist** with a crack of bones. It opens again when the signal goes.
5. **Witch's Lantern** (iron nuggets, a torch and an amethyst shard): a gothic iron lantern. It has four corner posts round violet leaded glass with a flame behind it, a stepped roof with a spire, and a ring. It stands on a floor or hangs from a ceiling on a chain, like a vanilla lantern, and gives light 13.

## Connections
- Existing input producer: bones and bone blocks (skeletons), bone meal, cobbled deepslate, coarse dirt, polished granite and chiselled stone bricks, iron nuggets, a torch, an amethyst shard (geodes).
- Existing output consumer: building. The gargoyle joins the graveyard pack's monuments, with their weathering, epitaphs and spirits (ghost hunting). Redstone drives the bone hand.
- Technology connection: redstone for the bone hand. Magic connection: none (decoration).
- Reachable entry path: all vanilla materials of the early game, and a crafting table.
- Required vs optional: optional decoration.

## Balance and automation
- Recipes take bones in and give decorations, and breaking gives the decoration back: one bone pile a layer, one ossuary wall, one hand, one lantern, one gargoyle. Nothing turns back into bones, so there is no loop.
- No ticking: the bone hand changes only on a neighbour update, and the bone pile only rattles when stepped on.

## Multiplayer and persistence
- Placing, breaking, redstone and the gargoyle's chisel and brush are server-side, through vanilla's paths and the graveyard pack's (`Epitaphs` checks the chisel session, reach and build rights).
- Nothing is saved but block states and the gargoyle's inscription (the headstone block entity).
- New IDs:
  - blocks and items `gargoyle`, `bone_pile`, `ossuary_wall`, `giant_bone_hand`, `witchs_lantern`;
  - their recipes.
- The recipes follow the agriculture feature switch.

## Dependencies and assets
No new dependencies. The gargoyle is sculpted with `tools/sculpt.py` (in `tools/graveyard_models.py`) on the graveyard's stone textures. The other models are built on `tools/flora_art.py` by `tools/decor15_data.py`, which paints their 64 × 64 textures by code. The lantern is vanilla's `LanternBlock` with these models. All original. The look follows the owner's reference pictures, and nothing is traced from them. Since 5 October 2026 the creature props are painted with the clean, cute painters in `tools/cute_art.py` ([ART_DIRECTION.md](../ART_DIRECTION.md#creatures-and-faces-cute-and-clean)).

## Verification
Automated checks run on CI's Build workflow: run 37177097502 on 2e913a39 (every job passed), and run 37178330514 on bfd738e9, after the close-ups were reframed and the flora (with main) merged in (every job passed). The branch has since merged the flora again, with Arms III from main:
- `python3 scripts/check_repository.py` and `python3 tools/check_mod_data.py`: pass (1271 IDs with the flora and main merged in). The audit's new churchyard check compares `BonePileBlock` and the lantern's light with `tools/decor15.py`. It checks that each ornament is registered, named, drops and has its recipe and 64 × 64 texture, and that the gargoyle is a headstone style. The graveyard check covers the gargoyle like every headstone.
- `./gradlew build` with the game tests: pass in both runs, among them:
- `ChurchyardOrnamentsGameTests` (six tests, all pass):
  1. a bone pile heaps to four layers and no more, gives one pile a layer, won't stand in the air and goes with its ground;
  2. an ossuary wall faces its builder and drops itself;
  3. the bone hand stands two tall, clenches both halves while powered, opens again, and breaks as one;
  4. the lantern stands and hangs with its light;
  5. the gargoyle is placed whole on its plinth and bone meal weathers both blocks;
  6. recipes and loot load.
- The headstone tests' data check covers the gargoyle's recipe and loot.
- `ChurchyardOrnamentsClientGameTests` (CI job `client`, pass): a catacomb corner by day, the ossuary walls and bone piles, the gargoyles (one mossy), the bone hands (one clenched) and the lanterns, and the whole by night. Its six screenshots are in [the agriculture branch](../branches/AGRICULTURE.md#the-churchyards-ornaments).

Not run:
- Building with them by hand.
- A two-player dedicated server.
- This environment can't run a game client interactively.

## World and event applicability
Placed by players only; no worldgen. They work all year, and nothing depends on the Halloween event.

## Rollout and open questions
- Batch 16 follows on this branch's successor: [the haunted house's props](haunted-house-props.md), from the same pictures.
