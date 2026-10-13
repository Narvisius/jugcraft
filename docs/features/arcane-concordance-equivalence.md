# Arcane Concordance: bounded material equivalence

Status: implemented on branch `claude/awesome-davinci-iwv3b9`; see Verification for what has run.
Proposal issue: none; the owner's Arcane Concordance brief (roadmap step 21) is the scope approval.
Owner: @jimbozoomer-byte. AI-assisted implementation with Claude Code; the model is named in the commit trailers.
Target milestone and tier: Concordance milestone 13, Practitioner stage (Jugcraft Workshops). Roadmap step 21.
Primary specialty and supported player role: the Balancewrights (Strata, Tide). Weigh mundane matter exactly, dissolve
it into Prima Materia and form more of it, always at a loss, and never anything that is more than matter.

Builds on [typed resources](arcane-concordance-sharing.md) (Prima Materia is the seventh resource; its ledger is a
`Reservoir`) and First Light. Contract and checklist: [ARCANE_CONCORDANCE.md](../ARCANE_CONCORDANCE.md).

## Player experience

Once First Light is understood, examining raw matter (raw iron, copper or gold, calcite, clay) begins **Assay**;
studying it at a Lampwright's Bench (or reading notes) understands it.

**The Assayer's Scale** (gold, copper and iron on a dark-oak plinth) works with what you hold:

- a material in your main hand: it is **weighed**: its exact worth in grains of Prima Materia, what the stack would
  dissolve into, and what forming one costs. Nothing changes. Use the scale again within ten seconds and the stack is
  **dissolved** into your ledger;
- an empty main hand and a material in your other hand: the material is the scale's pattern (it is kept), and one more
  of it is **formed** from your ledger;
- both hands empty: your ledger (`/jugcraft concordance equivalence balance` too).

Dissolving pays the exact worth of the whole stack, rounded down once; forming costs five quarters of the worth, rounded
up once. So every round trip loses (an iron ingot dissolves into 256 grains and costs 320 to form), splitting a stack
never earns more than the whole, and nothing is ever rounded into existence. A ledger holds at most 1,000,000 grains; a
stack too big for the room left is dissolved only in part.

**What is weighed**: only the 45 catalogued mundane materials (stone, soil, sand, wood, the common metals and their
blocks and nuggets, coal, redstone, lapis, quartz, glass and bottles, clay, honey, wheat, sugar cane, paper, string,
wool, bone), and only plain stacks. The scale refuses, with the reason, anything that:

| Refusal | Because |
|---|---|
| excluded | it is tagged `#jugcraft:equivalence/excluded`: research notes, wands, lanterns, relics, rings, living equipment, charms and keys, enchanted books, potions, totems, written books, maps, heads, spawners |
| magical | it carries a component of Jugcraft, Spell Engine, Spell Power or Trinkets, enchantments, potion contents or a glint |
| inventory | it holds other items (a container, a bundle, a loaded crossbow) |
| unique | it is named, written, mapped, a player head, a lodestone compass or locked |
| bound | it carries a creature or a block's own state (a bucket of fish, bees, block entity data) |
| metadata | anything else about it has been changed |
| uncatalogued | the scale does not know its worth (diamonds, emeralds and netherite are not catalogued) |

Owning something, or seeing it in a recipe viewer, never makes it weighable.

## How it works

**The catalogue** (`data/<ns>/concordance/material/`, `concordance/transmutation/`, read by `EquivalenceParser` into
`EquivalenceCatalog`, `concordance/equivalence`, pure Java). A material gives its item, its exact value as a fraction
of grains (an iron nugget is 256/9), and whether it may be dissolved and formed. A transmutation declares one
conversion: what it takes, makes, gives back (containers, such as the glass bottles a honey block returns) and yields
as byproducts, its catalysts (needed, never consumed), how it happens (crafting with its grid, smelting with its fuel,
breaking), and whether it is a recipe or an external **source** (a cobblestone generator, farms, trees, bees).

**The audit** (`CycleAudit`, also run in Python by `tools/check_mod_data.py`): over the declared graph plus the scale's
own dissolving and forming, it refuses any conversion with an unvalued item; any recipe whose gives are worth more than
what it takes, at exact values; and any cycle, found by searching every simple cycle of up to six steps, that returns at
least what it was fed while gaining value. Sources stand apart: matter may enter from them, and no cycle runs through
them. If every item has a positive value and no recipe gains value, no closed cycle can create value however it is
run, because a cycle's gain is the sum of its steps'; the cycle search is the second, independent look. A catalogue
with any finding is out of balance: the scale weighs nothing until it is fixed (`/jugcraft concordance equivalence
audit`, operators, lists the findings).

**Eligibility** (`Eligibility`): the server reads what a stack really carries (the components its stack adds or
removes) and judges it by explicit rules, in the order of the table above. The scale also refuses to work if any
catalogued item is excluded or carries an inventory, a creature or magic on every stack, so no adapter or datapack can
catalogue one.

**On the server** (`concordance/assay`): the ledger is a player attachment (`jugcraft:prima_ledger`, kept through
death) moved by the shared resource rules (`Reservoir` with `Overflow.REJECT` to dissolve, an exact extraction to
form): all or nothing, never past the cap, never below empty. A weighing waits for its confirming use against the exact
item and count. When the data loads, the server also **probes its live recipes** (vanilla's, other mods' and
datapacks'): each catalogued material alone, as a column of two, a 2x2 and a 3x3 in a crafting grid, and in a furnace
(allowed an eighth of a coal's worth of fuel). Any that makes catalogued matter (with what the grid gives back) worth
more than went in puts the scale out of balance until fixed, so an adapter cannot reopen a profitable cycle.

## Connections

- Input producer: First Light (Assay needs it understood); mundane materials from mining, farming and crafting.
- Output consumer: formed materials for building and crafting; Assay's mastery.
- Technology connection: the declared graph is the game's own recipes (the game tests check each crafting and smelting
  conversion against the server's recipe manager).
- Magic connection: Prima Materia is the Concordance's seventh resource; the scale refuses every magical thing.
- Reachable entry path: First Light understood → examine raw iron → study at the bench → build an Assayer's Scale
  (gold, copper, iron, smooth stone and dark oak) → weigh and dissolve cobblestone.
- Solo, trade and cooperative routes: solo; grains are personal and cannot be traded (formed items can).
- Specialty use without other branches: needs only First Light.
- Mastery: five different materials dissolved master Assay.

## Balance

- **No positive loop**: every declared recipe gives at most what it takes; the scale pays rounded down and charges 5/4
  rounded up; the audit refuses the catalogue otherwise and searches every cycle for a profitable one.
- The catalogue is restricted to common matter: nothing scarce (diamonds, emeralds, netherite, ancient debris) and
  nothing magical can be formed. Forming an item needs one already in hand.
- A ledger is capped; a weighing must be confirmed; uses are rate-limited.

## Multiplayer and persistence

- Server authority: eligibility, worth, rounding and the ledger are the server's; the client only sees messages.
- Permissions: anyone who understands Assay may use any scale; grains belong to their player. The audit command needs
  operator rights.
- Persistence: the ledger in `jugcraft:prima_ledger`; confirmations are not saved.
- Disable behaviour: with `concordance.enabled=false` the scale does nothing; ledgers are kept.

## Dependencies and assets

No new dependency. Framework use:

- **Fabric API**: the attachment, commands and events. The ledger moves through Jugcraft's own `Reservoir` (the shared
  resource rules), formed items through the player's inventory.
- **JEI (optional)**: not used. Possession or visibility in JEI never establishes eligibility; the codex lists the
  catalogue.
- **GuiLib**: not used; the scale's messages, the codex and the commands are the inspection. A screen belongs to step 26.
- **Modonomicon**: a new **Equivalence** codex category (Assay, the scale, what is weighed, the catalogue with every
  worth, the balanced graph and its sources).

Art (`tools/concordance_equivalence_art.py`, `tools/item_icons/assayers_scale.txt`, on the block model in
`tools/concordance_equivalence_models.py`): the scale's one 16x16 texture is four material regions (dark-oak planks with
a violet socket for the pillar, a bevelled brass plate, two iron chains of alternating links, a brass dish for the pans'
insides), and its icon shows the whole balance. Everything is drawn fresh in the Concordance's palettes (its dark oak,
brass and violet, and the arms' chain iron); the owner's library holds no balance, weight or chain, and its cabinets
and brasses were looked at and not used (the module's docstring says why). The art has not been shown to the owner
yet.

## Verification

- `python3 tools/check_mod_data.py`: new step 21 checks (`check_equivalence`): the Java's markup, cap, batch, limits,
  excluded tag and the scale's numbers equal the generator's; the catalogue and graph on disk are the generator's;
  Python's own audit (an independent implementation in exact fractions) finds no unvalued item, no recipe that gains
  value and no profitable cycle; forming always costs more than dissolving pays; nothing excluded or scarce is
  catalogued; every reason and message has its text; the ledger moves through the shared `Reservoir`; the model,
  texture and icon agree. The pure-package rule covers `concordance/equivalence`.
- The pure core compiles with JDK 21 and the rules load the generated catalogue (45 materials, 42 transmutations) with
  no problems. The step 21 harness passes **31 checks** against it: exact fractions add and round as asked; nuggets
  dissolve at 256/9 rounded down once on the whole batch, and splitting never earns more; forming costs five quarters
  rounded up, and for every material and batch costs more than dissolving pays; eligibility refuses named, filled,
  bound, magical (Jugcraft's, Spell Engine's, enchantments), excluded, changed and uncatalogued stacks; a greedy recipe
  is refused (and, greedier, its cycle with the scale found profitable); a recipe pair that multiplies planks is
  refused with its cycle; an unvalued item cannot be certified; a declared source may add matter; 106 cycles are
  searched and none is profitable, every cycle through forming losing; and the parser's refusals.
- Game tests added: `ConcordanceAssayGameTests` (six): a stack is weighed then dissolved (nothing changes on the first
  use; nine nuggets give 256; one gives 28); forming costs more than dissolving pays (an ingot formed for 320, the
  pattern kept, dissolved again for 256; too few grains form nothing); only plain catalogued matter is weighed (named,
  magical, glinting, changed, uncatalogued and excluded stacks are refused and nothing moves either way); the ledger
  never passes its cap; every declared crafting and smelting conversion is the game's own recipe, output and count;
  and the catalogue is balanced, this server's live recipes included.
- CI: the first run on this step (commit fdf36745) found three faults, fixed in 65d98e8b (26.3's component patch
  has no entry set, so a stack's changes are compared component by component), 65870d7d (an item's nature is judged
  against a plain item's defaults, which already carry an empty enchantment list) and ca66a3e1 (a formed ingot may
  join the stack the pattern is in). Run 37640896275 (commit a1088c59, which also carries step 22) passes the whole
  Build workflow: it builds, passes the data checks and all 1017 required server game tests (the six above among
  them), and the client test shards pass.

Not yet run: any client (the scale's model and icon, the codex pages), a two-client dedicated server.

## World and event applicability

Works everywhere; no seasonal content.

## Rollout and open questions

New registrations only: block and item `jugcraft:assayers_scale`; attachment `jugcraft:prima_ledger`; research
`jugcraft:assay`; practice `jugcraft:assay`; item tags `jugcraft:assay_specimens` and `jugcraft:equivalence/excluded`;
data folders `concordance/material` and `concordance/transmutation`. Removing them needs a migration.

Open items:

- Only oak among the woods; other woods, stones and metals can be catalogued with their recipes.
- The live probe tries each catalogued material alone, as a column of two, a 2x2, a 3x3 and in a furnace; a recipe
  mixing a catalogued material with other things is checked only through the declared graph.
