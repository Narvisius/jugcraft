# Batteries and power management

Technology package 3, implemented locally with OpenAI Codex (GPT-6). On 16 October
2026 the owner authorized integration with main and local validation of all seven
packages. The earlier testing hold is superseded. **Keep this work local until the
owner has played it and approves publication.**

Current build, test evidence, draft reconciliation and remaining release checks:
[seven-package integration record](tech-seven-local-integration.md). The detailed
verification lists below are release checklists; only results explicitly recorded
there are claimed complete.

## Storage progression

| Storage | Capacity | Maximum input / output per tick |
|---|---:|---:|
| Existing Battery Box | 400,000 JE | 256 / 256 JE |
| New Lead-acid Battery Bank | 2,000,000 JE | 1,024 / 1,024 JE |
| Existing Capacitor Bank | 4,000,000 JE | 4,096 / 4,096 JE |
| Existing Lithium Battery Bank | 32,000,000 JE | 16,384 / 16,384 JE |
| Existing Flow Battery, full base tanks | 64,000,000 JE | 8,192 / 8,192 JE |
| Flow Battery with four paired tank modules | 320,000,000 JE | 8,192 / 8,192 JE |
| Flow Battery with one cell-stack module | Capacity determined by electrolyte/modules | 16,384 / 16,384 JE |

These are base limits before cable bottlenecks, selected output limits and reserve.
Every new bank starts empty. Existing construction recipes, IDs and capacities
remain available. Energy transfer remains lossless to preserve the established
storage model; finite capacity, shared per-tick limits and charging requirements
prevent energy creation. No routine battery aging or electrolyte replacement.

This adds one compact bank and two physical expansion blocks, **no new processing
machines**. Lead, nickel, aluminum, lithium and chemical supplies retain their
existing acquisition paths. Portable packs, exotic high-output banks and remote
grid dispatch remain later work.

## Shared production routes

Quantities below are abstract game batches, not real chemistry formulas. Salt and
solvent charges are items, not fluid buckets; they have no unmetered unpacking route.

| Product | Input | Station |
|---|---|---|
| 2 galena concentrate | 2 existing raw lead + 250 mB water | Mineral Conditioner |
| 2 lead ingots | 2 galena concentrate + 1 coke | Arc Furnace |
| 1 lead electrode | 1 lead plate | Metal Press |
| 1 lead oxide electrode | 1 lead electrode + 250 mB oxygen | Bulk Chemical Reactor |
| 4 battery separators | 1 plastic sheet | Metal Press |
| 1 dry lead cell | 1 electrode, 1 oxide electrode, 1 separator | Crafting |
| 1 filled lead-acid cell | 1 dry cell + 250 mB sulfuric acid | Bulk Chemical Reactor |
| 1 lead-acid bank | 4 filled cells, 2 steel plates, 2 copper wire, 1 basic circuit | Crafting |
| 1 activated spodumene | 1 existing spodumene | Industrial Oven |
| 2 lithium carbonate | 2 activated spodumene + 500 mB sulfuric acid | Mineral Conditioner |
| 1 lithium fluoride | 1 lithium carbonate + 250 mB HF | Bulk Chemical Reactor |
| 1 fluorophosphate precursor | 1 phosphate + 500 mB HF | Bulk Chemical Reactor |
| 1 LiPF6 salt | 1 lithium fluoride + 1 fluorophosphate precursor | Chemical Reactor |
| 2 carbonate solvent charges | 500 mB bioethanol + 250 mB carbon dioxide | Bulk Chemical Reactor |
| 2 formulated electrolyte charges | 1 LiPF6 salt + 2 solvent charges | Chemical Reactor |
| 1 cathode mix | 1 lithium carbonate, 1 iron ingot, 1 phosphate | Crafting |
| 2 LFP cathodes | 1 cathode mix | Bulk Chemical Reactor |
| 1 carbon anode | 1 coke | Metal Press |
| 1 dry lithium cell assembly | 1 cathode, 1 anode, 1 separator, 1 aluminum plate | Crafting |
| 1 existing lithium cell | 1 dry assembly + 1 electrolyte charge | Circuit Assembler |
| 1 vanadyl sulfate charge | 1 existing vanadium oxide OR pentoxide, 1 coke, 500 mB sulfuric acid | Chemical Reactor |
| 1,000 mB existing vanadium electrolyte | 1 sulfate charge, 500 mB sulfuric acid, 500 mB water | Bulk Chemical Reactor |

Raw lead already represents galena-bearing feed: no redundant ore ID, retrogen or
changed lead drops. The optional concentrate route does not multiply metal.
Vanadium-bearing concentrate/oxide already have paid independent producers, including
the existing non-sulfuric contact-bed route. Those and the refinery-residue electrolyte
recipe remain available. Expanded lithium is an optional specialist route alongside
the simpler released recipe; making it mandatory or economically dominant is not
claimed. Balance comparison remains in deferred validation.

The Conditioner, Oven and Bulk Reactor retain their single solid-input hatches.
Dry assemblies combine solids before filling; two-solid chemical jobs use the
existing two-input Chemical Reactor. No inventory or port layouts were enlarged.
Form jobs have distinct station capabilities and share existing recipe previews,
locks, escrow and full-output handling. Most new form jobs specify 200 ticks at
64 JE/t before profile modifiers; activation/solvent/cathode preparation use 240
ticks. Standard processors use their existing power costs. Canonical receipts live
in `tools/battery_expansion.py` and the generated recipe viewer.

## Physical flow modules and accounting

Each **Paired Flow Tank Module** represents two equal 32-bucket sides, adding 64
buckets total. Place a contiguous row of up to four directly behind the middle
bottom block of the Flow Battery, with the same facing. For a north-facing controller
at `(0,0,0)`, sockets are `(-1,0,2)` through `(-1,0,5)`; coordinates rotate with it.
Each costs two Steel Tanks, a Precision Stainless Valve, two Stainless Fittings
and a Lightweight Frame. Capacity still needs electrolyte: modules create none.

A **Flow Cell-stack Module** sits immediately above the middle front top block,
at `(-1,3,0)` in that orientation. One raises the shared input/output budget from
8,192 to 16,384 JE/t; extra stacks have no effect. Construction uses two cell stacks,
an advanced circuit, two fittings and a lightweight frame. Each cell stack uses
four PTFE liners, two carbon anodes, two separators and copper wire.

The controller owns one electrolyte quantity and one energy quantity. Paired sides
are represented equally within that one ledger; players do not fill independently
typed anolyte/catholyte tanks. Modules own no fluid or charge and have no standalone
pipe or cable connections. Their directed fixed sockets prevent sharing modules
between differently oriented controllers. Reads are bounded to four tank sockets
and one stack socket and never load chunks.

If removal/unloading reduces attached capacity below the electrolyte already stored,
all energy transfers and further filling stop. Contents remain on the controller;
reattach enough modules to resume. Detaching a stack lowers throughput immediately
without changing contents. Detached module drops are empty. Breaking the controller
retains its electrolyte through the existing StoredFluid item component and loses
charge, as before. Replacing an expanded controller retains all electrolyte even
before its modules are restored. Expanded fluid amount is saved separately as well
as through the reservoir so early load order cannot truncate it.

## Controls and electrical integration

All five bank types have four buttons in the existing screen:

- **Mode:** Automatic, Charge only, Discharge only, Off. Default Automatic.
- **Reserve:** 0–90% in steps of ten. Discharge never crosses the reserve. With a
  nonzero reserve, it starts/resumes after reaching five percentage points above
  the floor. After reload that hysteresis gate starts closed until the threshold
  is met. Zero reserve preserves ordinary automatic operation.
- **Charge priority:** Low, Normal, High; default Normal. On a cable network,
  higher groups receive offers first, then unused budget falls through. Within
  each group, the existing even-share/spill algorithm is retained. A directly
  connected single receiver has no competing group to prioritize.
- **Output limit:** 100%, 50%, 25%; default 100%. Applies to active pushes and
  external extraction on the front, across all parts together.

Input/output tick budgets share transactional snapshots across every face, so aborted
simulations restore both energy and budget. Sided wrappers expose a common identity
for receiver deduplication and rejecting self-transfer. Reserve and mode also apply
to unsided lookups; changing a screen button cannot write energy or capacity. Menu
actions validate the open container and distance. Controls save on placed banks;
pickup does not preserve these settings, matching prior item-component behavior.

The screen shows a missing-module warning; its mode tooltip shows tank-pair count
and stack rate. These are local charging priority and output constraints, not remote
generator scheduling or global discharge-order arbitration.

## Art and provenance

The supplied owner catalog was consulted, including battery/capacitor textures.
These additions extend the existing repository electrical models and palettes for
consistency with the current banks. New code-native cabinets use graphite, muted
metal, inset cells and supported bus bars. Paired tank manifolds meet at actual
module interfaces; the cell stack connects to its controller socket. No loose
decorative pipe runs. New component art uses editable 16×16 text pixel maps and
existing material ramps. No source-library files were modified or executables run.

## Release verification checklist

After packages 1–7: compile both optional-framework configurations; resource/icon,
recipe and registration audits; capacity and recipe progression checks; old battery
save fixtures; transactional rollback; multiple-face rate limits and self-transfer;
mode/reserve/hysteresis/priority saturation and starvation behavior; expanded-fluid
pickup, detach/rejoin, unload/reload and rotated module layouts; automation and full
outputs; two-player controls and representative grid load. Review the lossless
compatibility decision and old/new recipe costs before release. The owner controls
the local playtest. Local build/test results are recorded in the integration record; no remote publication was performed.
