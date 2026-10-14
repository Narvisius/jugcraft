# PVC with hydrogen chloride recovery: ethylene dichloride, cracking and oxychlorination (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on [contact sulfuric acid](industrial-contact-sulfuric-acid.md) and the branches below it, down to the [Chemical Infuser](industrial-chemical-infuser.md).
Proposal issue: none. TODO item: "Detail H2/chlorine -> HCl and selected refinery-derived ethylene -> ethylene dichloride -> vinyl chloride -> PVC, with appropriate HCl recovery/reuse; retain existing PVC, butadiene rubber and natural-rubber routes."

The owner's direction ([chemistry and fuels plan](industrial-chemistry-and-fuels-plan.md)) is refinery-derived ethylene → ethylene dichloride → vinyl chloride → PVC, on shared machinery, with HCl recovery and reuse. The quantities, times and which machines run each step were left to design. They are this change's choices, made at the contributor's request ("just pick reasonable numbers") and listed below for review.

Owner: jimbozoomer-byte
Target milestone and tier: refinery era, polymers
Primary specialty and supported player role: chemical processing and polymers

## Player experience

A new named intermediate, **ethylene dichloride** (a liquid, with a bucket), joins the PVC chain. Every step runs on machines players already have:

1. **Direct chlorination**, in the Chemical Infuser with no bed: 250 mB of refinery gas (its ethylene) and 250 mB of chlorine → 500 mB of ethylene dichloride. 60 ticks.
2. **Cracking**, in the Catalytic Cracker, by heat alone with no catalyst used up: 500 mB of ethylene dichloride → 250 mB of **vinyl chloride** and 250 mB of **hydrogen chloride**. 80 ticks.
3. **Oxychlorination**, in the Synthesis Converter: 250 mB of refinery gas, 500 mB of hydrogen chloride and 125 mB of oxygen → 500 mB of ethylene dichloride. 60 ticks. This is where the cracker's hydrogen chloride is reused.
4. **Polymerization** is unchanged: the Polymerization Reactor turns 500 mB of vinyl chloride into four PVC resin.

The existing one-step route (the Synthesis Converter joining refinery gas and chlorine straight into vinyl chloride) is kept, as the TODO requires. So are the rubber routes.

## Connections

- **Existing input producers:**
  - refinery gas (the distillation tower and cracker);
  - chlorine (brine electrolysis);
  - oxygen (the Separator, the Air Separation Unit);
  - hydrogen chloride (the Infuser, and now the cracker).
- **Existing output consumers:**
  - vinyl chloride in the Polymerization Reactor;
  - hydrogen chloride in the Bulk Chemical Reactor's hydrochloric acid and in oxychlorination.
- **Technology connection:**
  - the Infuser's `gas_combination` capability gains its second recipe;
  - two legacy fluid machines gain one recipe each.
- **Magic connection:** none.
- **Reachable entry path:** every input comes before the Polymerization Reactor. No step needs PVC.
- **Required vs optional:** optional. The one-step route remains.

## Balance and automation

- **Chlorine.** The balanced route (one direct chlorination, two crackings, one oxychlorination) turns 500 mB of refinery gas, 250 mB of chlorine and 125 mB of oxygen into 500 mB of vinyl chloride. That is twice what the same chlorine makes in the one-step route. The price is oxygen, three machines and more time and energy.
- **Units.** Gases are 1 mB per reaction amount. Liquids are 2 mB per reaction amount: ethylene dichloride, and the oxychlorination water, which is not kept.
- **No fluid from nothing.** Each step makes no more fluid than it takes in.
- **No loop.** Hydrogen chloride is only recycled, never created, and nothing is a fuel.
- **Catalytic Cracker tanks.** The cracker puts the vinyl chloride in its first output tank and the hydrogen chloride in its second. A cracker still holding diesel or naphtha from oil cracking must be emptied first, like any machine switching products.

## Choices made here for review

- **Which machine runs each step:**
  - direct chlorination in the Infuser;
  - cracking in the Catalytic Cracker, with no catalyst;
  - oxychlorination in the Synthesis Converter.
- **Quantities and times:** 60, 80 and 60 ticks.
- **The oxychlorination water** is not kept, because the Synthesis Converter has one output tank.
- **Ethylene:** refinery gas stands for refinery-derived ethylene, as the existing vinyl chloride recipe already treats it.

## Multiplayer and persistence

- New registrations, stable from now on:
  - the liquid `jugcraft:ethylene_dichloride`, with its bucket and fluid block;
  - the recipes `gas_synthesis/infuser_ethylene_dichloride`, `catalytic_cracking/ethylene_dichloride` and `gas_synthesis/ethylene_dichloride`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The liquid's still, flow and bucket textures are drawn from its colours by `tools/petro_textures.py`, listed last so no other liquid's texture changes.

## Verification

Locally:
- `python tools/generate_material_data.py` and `python tools/check_mod_data.py`: passed.
- `javac` reported no syntax errors in the changed Java files.

For CI, server game tests:
- `ChemicalInfuserGameTests.ethyleneAndChlorineJoinIntoEthyleneDichloride`:
  - 250 mB of refinery gas and 250 mB of chlorine give 500 mB of ethylene dichloride, for exactly 7,680 JE;
  - the Catalytic Cracker's recipe cracks 500 mB of it into 250 mB of vinyl chloride and 250 mB of hydrogen chloride;
  - the Synthesis Converter's recipe reuses 500 mB of hydrogen chloride into 500 mB of the dichloride.
- `ChemicalInfuserGameTests.itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill`: now six recipes.

The cracker and converter steps are checked as registered recipes, not run in placed machines.

### Results

- **Run [38439262837](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38439262837) on the contributor's fork (commit 34d6d0f6): green.** Everything compiled; every server game test passed, `ethyleneAndChlorineJoinIntoEthyleneDichloride` among them; the build without optional integrations and all five client shards passed.

## Rollout and open questions

- The machine assignments, quantities and times await the owner's review.
- Still to come: keeping the oxychlorination water, a named ethylene feed (the plan's later renewable ethylene), and HDPE/LDPE.
