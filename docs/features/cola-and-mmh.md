# Cola and MMH

Status: implemented (pending CI and review).
Proposal issue: none. The owner, 26 October 2026, asked for the suggested polish items in order ("then do 2-9 in order"). The fifth was "food-grade acid/cola + MMH propellant", the two chemistry follow-ups the earlier records left open:
- [phosphate processing](phosphate-processing-and-fertilizers.md) and [sour candy](sour-candy.md): "food-grade phosphoric acid to cola";
- [cryogenics and rocket fuels](cryogenics-and-rocket-fuels.md): "hydrazine first, MMH later".

**This change's choices:** the quantities, cola's effect and MMH's fourth motor, listed below for the owner to review.

Owner: jimbozoomer-byte
Target milestone and tier: the phosphate and storable-propellant branches, after the Chemical Reactor and the Bulk Chemical Reactor
Primary specialty and supported player role: chemistry (with a farm, for cola), and rocketry

## Player experience

**Cola.** Industrial phosphoric acid is not an ingredient by itself; the chain runs:
1. The compact **Chemical Reactor** purifies it. 250 mB Industrial Phosphoric Acid, 250 mB water, one charcoal and one glass bottle make a vial of **Food-grade Phosphoric Acid** (200 ticks, base 48 JE/t).
2. Crafting the vial with one Caramel, two sugar, one Cinnamon, one Vanilla Pods and one Lemon makes a bottle of **Cola Syrup**. The syrup is mixed in the vial, so no bottle is left over.
3. The reactor **carbonates** one Cola Syrup with two glass bottles, 500 mB water and 250 mB carbon dioxide into three bottles of **Cola** (120 ticks, base 32 JE/t).

Cola gives 3 hunger, 0.3 saturation and **Speed for 45 seconds**. It leaves its glass bottle and stacks to 16.

**MMH.** The **Bulk Chemical Reactor** methylates hydrazine: one redstone, 250 mB hydrazine and 250 mB methyl chloride give 250 mB **Monomethylhydrazine (MMH)** and one Propellant Process Residue (320 ticks, base 96 JE/t, the storable-propellant job). Then:
- the compact Chemical Reactor seals 1000 mB of it into one Rocket Casing as a **Sealed MMH Tank**, as it does the other fuels;
- in the **Rocket Workshop**, one MMH tank, one N2O4 tank and two nozzles make **four** Storable Bipropellant Motors. Hydrazine makes three: the fourth is the reward for the extra step.

The motor is the same item whichever fuel made it, with the same payload and range. MMH is not a monopropellant here: the Hydrazine Thruster still needs hydrazine.

The Engineer's Handbook has a page for each: "Food-grade acid and cola" and "MMH: the second storable fuel".

## Connections

- **Cola's inputs:**
  - Industrial Phosphoric Acid (the phosphate filter press);
  - charcoal, water and glass bottles;
  - caramel, cinnamon, vanilla pods and lemons (the farm and orchards);
  - sugar (cane, or glucose syrup made into sugar);
  - carbon dioxide (calcite in hydrochloric acid, cement burnt with capture, the Biogas Cleaner).
- **Cola's output:** a drink.
- **MMH's inputs:** hydrazine (the Bulk Chemical Reactor), methyl chloride (methane and chlorine in the Chemical Reactor), redstone and N2O4.
- **MMH's output:** the existing Storable Bipropellant Motor, and through the `atmospheric_rocket_motors` tag every rocket that takes a motor. Its residue washes like the other propellants'.
- **Machines:** no new machine. The compact Chemical Reactor gets three recipes (purifying, carbonating, sealing MMH), the Bulk Chemical Reactor one, and the Rocket Workshop one.
- **Optional:** yes. Nothing else needs cola or MMH.

## Balance

- 250 mB industrial acid, a charcoal and a bottle make one vial, and a vial makes three colas. Each cola costs about 83 mB of acid and a third of the farm ingredients.
- MMH costs as much hydrazine as it yields (250 mB in, 250 mB out) plus 250 mB methyl chloride, a redstone and the reactor's time. That buys a third more motors per N2O4 tank.
- **No loops:** neither product turns back into its inputs.

## Multiplayer and persistence

New registrations, stable from now on:
- the items `jugcraft:food_grade_phosphoric_acid`, `cola_syrup`, `cola` and `mmh_tank`;
- the fluid `jugcraft:monomethylhydrazine`, with its bucket;
- the recipes:
  - `chemical_reaction/phosphate_food_grade_acid`, `phosphate_cola`, `cryo_mmh` and `cryo_mmh_tank`;
  - `rocket_assembly/cryo_mmh_bipropellant_motor`;
  - the crafting recipe `phosphate_cola_syrup`.

Existing recipes, items and yields are unchanged. The server applies cola's effect.

## Dependencies and assets

No new dependencies. The icons are drawn by the generator, and none is copied:
- the vial, the syrup bottle and the cola bottle in `tools/petro_textures.py`;
- the MMH tank from `tools/item_icons/mmh_tank.txt` (the hydrazine tank's map with an olive band);
- the MMH fluid's textures from its palette.

Two generated tables gain the five new items by hand, since this machine has no Minecraft jar or game to rebuild them:
- `data/jugcraft/alchemy/values.json`: every existing record is kept; the new items get the builder's own rules applied
  to the local recipes. Cola syrup costs what its crafting ingredients cost (5); the rest take the labelled
  one-essence fallback, as the other fluid-made chemicals do.
- `docs/audits/survival-item-registry.json` (the registry snapshot the survival audit reads): the five new item IDs.
  The snapshot game test's next export will confirm them.

## Verification

Server game tests (`ColaAndMmhGameTests`):

| Test | What it checks |
| --- | --- |
| `theReactorMakesFoodGradeAcidAndCola` | The reactor's recipes give one food-grade vial, three colas and one MMH tank. |
| `theBulkReactorMethylatesHydrazine` | The MMH job is a storable-propellant job taking hydrazine and methyl chloride, giving 250 mB MMH. |
| `foodGradeAcidCraftsIntoColaSyrup` | The vial, caramel, two sugar, cinnamon, vanilla and lemon craft one cola syrup. |
| `colaGivesSpeed` | Drinking cola gives Speed. |
| `mmhMakesFourBipropellantMotors` | MMH and N2O4 make four motors; hydrazine and N2O4 still make three. |

### Results

Local, without Gradle or a game: the generator, the data checks, the Python unit tests and the Java syntax parse; see the PR.

CI: see the PR.

Not run: a human playing the chains.

## World and event applicability

Everywhere the phosphate and rocket branches are.

## Rollout and open questions

- The quantities, cola's Speed and MMH's fourth motor await the owner's review.
- Methylamine chemistry, preservation chemistry and orbital/station uses of MMH remain separate items.

## History

Before this:
- the handbook said "This industrial acid is not a food-grade cola ingredient";
- it said "MMH ... remain future work".
