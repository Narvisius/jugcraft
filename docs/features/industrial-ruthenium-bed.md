# Ruthenium catalyst bed: faster methanation from nickel ore's ruthenium

Status: implemented on the contributor's fork; CI green; pending review.
Stacked on [factory payoffs](industrial-factory-payoffs.md) and the branches below it, down to [methane power](industrial-methane-power.md).

Proposal issue: none. TODO item: "optional later ruthenium improvement" of the methanation recipes.

**From the owner's plan** ([chemistry and fuels plan](industrial-chemistry-and-fuels-plan.md)):
- ruthenium is an optional later catalyst;
- it needs its own obtainable material and producer;
- it should process faster or cost less to run;
- changing catalyst never permits more methane than the carbon and hydrogen charge supports.

**This change's choices,** made at the contributor's request ("just pick reasonable numbers") and listed below for review:
- the ruthenium source;
- its quantities;
- the bed recipe;
- the speed-up.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, after the nickel bed
Primary specialty and supported player role: chemical processing and power

## Player experience

1. **Ruthenium.** The platinum-group metals ride along in nickel ores. In the Bulk Chemical Reactor:
   - eight washed nickel ores in the hatch;
   - a bucket of hydrochloric acid in the side inlet;
   - 250 mB of chlorine in the lid inlet;

   leave one **ruthenium powder** at the chute in 400 ticks. The nickel itself is spent.
2. **The bed.** The powder on a Nickel Catalyst Bed (shapeless crafting) makes the **Ruthenium Catalyst Bed**. It fits the Chemical Infuser's bed socket and is never used up.
3. **Faster methanation.** With the ruthenium bed in, every methanation runs in **half the time, and so for half the energy**:
   - CO2 methanation: 100 ticks;
   - CO methanation: 75 ticks;
   - biogas upgrading: 100 ticks.

   The same gases go in and come out, so there is no extra methane.

The Engineer's Handbook pages for the Infuser and the Bulk Chemical Reactor describe both steps, and JEI lists the recipes.

## Connections

- **Existing input producers:**
  - washed nickel ore (ore washing or leaching);
  - hydrochloric acid (the Bulk Chemical Reactor);
  - chlorine (brine electrolysis);
  - the Nickel Catalyst Bed.
- **Existing output consumers:** the Infuser's three methanation recipes, which supply methane to the Gas Burning Generator and the [Combined Cycle Power Plant](industrial-factory-payoffs.md).
- **Technology connection:**
  - the Bulk Chemical Reactor gains the capability `jugcraft:precious_metal_recovery`;
  - the bed joins the `jugcraft:catalyst_beds` socket tag;
  - its recipes use the shared tool-socket mechanism. The bed is matched per recipe, so the nickel bed keeps its own recipes.
- **Magic connection:** none.
- **Reachable entry path:** every input comes before it, and the nickel bed it upgrades comes first. Nothing it makes is needed to make it.
- **Required vs optional:** optional. The nickel bed keeps working.

## Balance and automation

- **Same amounts, faster.** A catalyst changes only the rate: 100 mB of CO2 + 400 mB of H2 still give 100 mB of methane and 400 mB of water.
- **Lower running cost.** Half the ticks at the same 128 JE/t means half the energy:
  - CO2 methanation drops from 25,600 to 12,800 JE.
  - Methane has no electricity-only route (its carbon comes from coal, crops or waste), so the power-loop audit is unaffected.
- **A real investment.** Eight washed nickel ores, a bucket of hydrochloric acid and 250 mB of chlorine, once per bed.
- **No accidental mixing.** With the nickel bed in the socket, only the nickel recipes run. With the ruthenium bed, only the ruthenium ones.

## Choices made here for review

- **The source:** washed nickel ore, as platinum-group metals are recovered with nickel. The amounts are eight ores, a bucket of hydrochloric acid and 250 mB of chlorine for one powder, in 400 ticks.
- **The bed:** the powder on an existing nickel bed, so the ruthenium bed is an upgrade of it.
- **The benefit:** half the time and half the energy for every methanation.
- **Not built:** methanation's readable heat requirements, and any other use for ruthenium.

## Multiplayer and persistence

- The server decides everything. The bed socket takes items only from a player at the screen.
- New registrations, stable from now on:
  - the items `jugcraft:ruthenium_powder` and `jugcraft:ruthenium_catalyst_bed`;
  - the capability `jugcraft:precious_metal_recovery`;
  - the recipes `chemical_reaction/bulk_ruthenium_powder`, `gas_synthesis/infuser_ruthenium_{carbon_dioxide_methanation,carbon_monoxide_methanation,biogas_upgrading}` and `ruthenium_catalyst_bed`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new.
- **The powder icon:** drawn at 16×16 in `tools/petro_textures.py` in the item icon manner. It is a heap of steel-grey powder with a cold blue sheen.
- **The bed icon:** the nickel bed's brick tray, its pellets dusted with ruthenium. Each part is outlined in its own darkest tone and lit from the top left.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.

For CI, server game tests:
- `ChemicalInfuserGameTests.theRutheniumBedMethanatesInHalfTheTime`: 100 mB of methane and 400 mB of water in 100 ticks, for exactly 12,800 JE, and the bed stays.
- `ChemicalInfuserGameTests.itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill`: now nine recipes, the ruthenium ones at half time.
- `BulkChemicalReactorGameTests.washedNickelOreGivesRuthenium`: one ruthenium powder at the chute, and the powder on a nickel bed crafts the ruthenium bed.

### Results

- Earlier runs ([38468994620](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38468994620), [38471427250](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38471427250)) failed only on the Combined Cycle Power Plant underneath (its 7-block width, then its client test's full store), fixed in [factory payoffs](industrial-factory-payoffs.md).
- **Run [38494565308](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38494565308) (commit 0f0b1d33) passed:** the build, the data checks, every server game test (this change's included) with and without optional integrations, and all five client test shards.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The source, quantities, bed recipe and speed-up await the owner's review.
- Still to come: methanation's readable heat requirements.
