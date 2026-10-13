# Flow batteries: vanadium electrolyte as grid storage

Status: implemented on `feature/chemistry-17` (batch 17). Compiles and tests in CI only; **not yet played**.
Proposal issue: the owner's chemistry list, item 5 (1 October 2026: "lets do 1-5 of the chemistry list"), from the saved backlog in [MACHINE_ROADMAP.md](../MACHINE_ROADMAP.md).
Owner: jimbozoomer-byte
Target milestone and tier: late power storage, after the lithium battery bank
Primary specialty and supported player role: power and chemistry

## Owner-requested major grid-storage expansion (7 October 2026)

The [industrial chemical catalog](industrial-chemical-catalog-and-routes.md#battery-progression-major-grid-storage-and-other-advanced-uses) selects vanadyl-sulfate/formulated vanadium electrolyte chemistry for major grid storage, building on this existing battery. General storage develops before portable/specialty banks. Modular tank additions increase capacity; cell-stack upgrades increase charge/discharge output. Exact electrolyte processing, module sizes, stack tiers and limits remain to design. No routine cycle-degradation upkeep is selected; energy losses, finite capacity and charging requirements still apply. Account for electrolyte/energy once across the connected installation and preserve existing IDs and saved storage behavior during review.

The [twelfth owner batch](industrial-chemistry-and-fuels-plan.md#grid-storage-synthetic-fuels-and-cryogenics-decisions-twelfth-batch), recorded 8 October 2026, selects mineral-derived electrolyte supply alongside refinery residue, attached paired tank modules around a bounded controller/cell stack, and optional local charge/discharge priorities with a minimum-charge reserve setting. Remote control stays later. Exact source/refining, paired-side composition, module geometry/bounds, costs, losses, output and reserve semantics remain to specify. These planning additions preserve the selected capacity/output split and do not change the current battery behavior below.

## Player experience
- **Vanadium electrolyte:** two asphalt binder leached in a bucket of sulfuric acid in the chemical reactor make a bucket of deep-blue **vanadium electrolyte**. Heavy oil residue is rich in vanadium. The electrolyte is a liquid with a bucket.
- **Flow battery** (3 wide, 3 tall, 2 deep, the electric look): two tall electrolyte tanks with blue sight glasses either side of a cell stack.
  - It holds **1,000 JE for every mB of electrolyte** in it: empty, it holds nothing; with all **64 buckets** in, **64,000,000 JE**, twice a lithium battery bank.
  - Fill it by pipe or bucket. It only takes vanadium electrolyte, and the electrolyte cannot be pumped back out.
  - Broken, it drops with its electrolyte (like the steel tank and gas holder), but its charge is lost.
  - Like the other batteries it charges from any side and gives power out of its front, up to **8,192 JE/t** each way: half the lithium bank's rate. It stores a lot but moves it slowly.

## Connections
- Input producer: asphalt binder (vacuum distillation), sulfuric acid (chemical reactor), power from any generator.
- Output consumer: any machine on the grid.
- Technology connection: petrochemistry, industrial chemistry, glass chemistry (borosilicate glass in its recipe), power storage.
- Magic connection: none.
- Reachable entry path: two borosilicate glass, a processor, two steel tanks, a capacitor bank and two steel plates. All come from earlier tiers.
- Required vs optional: optional; it is a bigger, cheaper-per-JE store than the lithium bank, at a lower rate.

## Balance and automation
- **No energy from nothing.** The electrolyte only sets how much the battery may hold; it is never used up and makes no energy.
- **Cost per JE:** 64 buckets of electrolyte are 128 asphalt binder and 64 buckets of sulfuric acid (128 sulfur dust) on top of the machine.
- **Rate:** 8,192 JE/t, so a full battery takes about 7,800 ticks (6.5 minutes) to fill or empty at full rate.
- The fluid audit in `check_mod_data.py` passes: as much electrolyte comes out of the reactor as acid goes in.

## Multiplayer and persistence
- Server-side. Its electrolyte and charge are saved with the block entity; the charge is loaded after the electrolyte so a full battery reloads full.
- The menu shows the capacity the electrolyte allows (the synced capacity), not the 64,000,000 maximum.

## Dependencies and assets
No new dependencies. The model (`tools/electric_models.py`, `flow_battery`), the blue sight-glass texture (`el_glow_blue`, `tools/electric_textures.py`) and the fluid textures are original.

## Verification
- `tools/check_mod_data.py` passes.
- Game test `flowBatteryHoldsWhatItsElectrolyteAllows`:
  - an empty battery holds 0 JE;
  - it refuses water, takes a bucket of electrolyte, and then holds exactly 1,000,000 JE;
  - the electrolyte cannot be pumped out;
  - the chemical reactor makes 1,000 mB of electrolyte from two asphalt binder and 1,000 mB of sulfuric acid.
- Not run: client play, two players, performance with many batteries.

## World and event applicability
Not applicable: no worldgen, mobs or dimensions.

## Rollout and open questions
- A vanadium ore, if the owner wants a mined source as well as oil residue.
- The battery's screen could show the electrolyte level; it comes with the machine screen redesign.
