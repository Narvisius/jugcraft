# Contact sulfuric acid: the vanadium bed, contact conversion and absorption (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review).
Stacked on [coal gasification](industrial-coal-gasification.md) and the branches below it. It uses:
- the [Chemical Oxidizer](industrial-chemical-oxidizer.md)'s sulfur dioxide;
- the [Chemical Infuser](industrial-chemical-infuser.md)'s bed socket;
- the [Bulk Chemical Reactor](industrial-bulk-chemical-reactor.md)'s absorption.

Proposal issue: none. TODO items:
- "contact conversion and absorption, for the sulfuric acid chain" (package 2);
- the industrial route of "Make sulfuric acid a central industrial supply with reachable sulfur oxidation/conversion/absorption".

**From the owner's plan** ([starter gas and acid factory](industrial-starter-gas-and-acid-factory.md)):
- the conversion and absorption quantities, times and energies;
- a named reusable vanadium-oxide/ceramic contact bed, prepared independently from vanadium-bearing iron concentrates without sulfuric acid;
- contact conversion in the Infuser family;
- the existing simple acid recipe kept.

**This change's choices,** made at the contributor's request ("just pick reasonable numbers") and listed below for review:
- the vanadium preparation;
- the bed's recipe;
- leaving out the retained carrier-acid charge.

Owner: jimbozoomer-byte
Target milestone and tier: steel era, the acid line of the starter gas and acid factory
Primary specialty and supported player role: chemical processing

## Player experience

The full industrial sulfuric acid chain:

1. **Vanadium.** In the Bulk Chemical Reactor, four washed iron ores (the game's vanadium-bearing iron concentrate) roasted with 500 mB of lye give a **vanadium pentoxide** at the chute. It takes 200 ticks, with no sulfuric acid involved.
2. **The contact bed.** The oxide on two fired bricks crafts the **Vanadium Contact Bed**. It goes in the Chemical Infuser's catalyst-bed socket, like the Nickel Catalyst Bed, and is never used up.
3. **Contact conversion.** With the vanadium bed in, the Infuser turns 250 mB of sulfur dioxide (right inlet) and 125 mB of oxygen (left inlet) into **250 mB of sulfur trioxide** in 100 ticks. The nickel bed does not serve for this, and the screen names the bed socket.
4. **Absorption.** The Bulk Chemical Reactor dissolves 250 mB of sulfur trioxide (lid inlet) in 750 mB of water (side inlet) into **1,000 mB of sulfuric acid** in 100 ticks (raised in [factory payoffs](industrial-factory-payoffs.md)).

The Chemical Reactor's simple recipe (2 sulfur dust + 1,000 mB of water → 1,000 mB of acid) is unchanged, as the plan requires.

## Connections

- **Existing input producers:**
  - sulfur dust;
  - oxygen (the Separator, the Air Separation Unit);
  - washed iron ore (ore washing or leaching);
  - lye (brine electrolysis);
  - bricks.
- **Existing output consumers:** sulfuric acid feeds every existing consumer: ore leaching, fertilizer, lithium, rare earths, vanadium electrolyte, microchips, iodine and the electroplating bath.
- **Technology connection:**
  - the Infuser's socket now takes the tag `jugcraft:catalyst_beds`, which holds both beds. It replaces the unreleased `jugcraft:methanation_beds`.
  - new capabilities `jugcraft:contact_conversion` (Infuser) and `jugcraft:vanadium_recovery` (Bulk Chemical Reactor);
  - the Bulk Chemical Reactor's `acid_absorption` gains its second recipe.
- **Magic connection:** none.
- **Reachable entry path:** the vanadium comes from iron and lye, never from the acid it helps make, so there is no self-output gate. Every step's inputs come before it.
- **Required vs optional:** optional. The simple recipe remains.

## Balance and automation

- **Sulfur accounting.**
  - The Oxidizer burns one sulfur dust into 250 mB of sulfur dioxide. Contact conversion and absorption then give 1,000 mB of acid per dust.
  - The simple recipe gives 500 mB per dust, so the industrial route doubles the acid from each sulfur (raised in [factory payoffs](industrial-factory-payoffs.md)).
  - It costs more energy: 9,600 + 12,800 + 8,700 JE per dust, against the simple recipe's 4,800 for half the acid. It trades power and plant for sulfur. It needs no sulfur in the reactor's own slot, and every step can be piped.
- **Oxygen.** 125 mB per 250 mB of sulfur trioxide: SO2 + ½ O2 → SO3.
- **The bed** is a one-time purchase: four washed iron ores, 500 mB of lye and two bricks.
- **Labels.** The Infuser's tanks are named "Left inlet tank" and "Right inlet tank", since the left one holds hydrogen or oxygen depending on the recipe.

## Choices made here for review

- **Vanadium preparation:** four washed iron ores + 500 mB of lye → one vanadium pentoxide, 200 ticks, in the Bulk Chemical Reactor.
- **The bed recipe:** a vanadium pentoxide between two bricks.
- **The retained carrier-acid charge is not built.** The plan selected a small retained acid charge, recirculated while the absorber makes more. Absorption here takes plain water instead.
- **The socket tag** was renamed `catalyst_beds`, so one socket holds either bed.

## Multiplayer and persistence

- The server decides everything. The bed socket takes items only from a player at the screen.
- New registrations, stable from now on:
  - the items `jugcraft:vanadium_pentoxide` and `jugcraft:vanadium_contact_bed`;
  - the gas `jugcraft:sulfur_trioxide`;
  - the item tag `jugcraft:catalyst_beds`;
  - the capabilities `jugcraft:contact_conversion` and `jugcraft:vanadium_recovery`;
  - the recipes `gas_synthesis/infuser_sulfur_trioxide`, `chemical_reaction/bulk_sulfuric_acid`, `chemical_reaction/bulk_vanadium_pentoxide` and `vanadium_contact_bed`.
- The unreleased tag `jugcraft:methanation_beds` is removed. No released save used it.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new.
- **Icons:** both are drawn at 16×16 in `tools/petro_textures.py` in the item icon manner, each part outlined in its own darkest tone and lit from the top left.
  - the vanadium pentoxide is a heap of orange-ochre powder;
  - the contact bed is the nickel bed's fired-brick tray, heaped with ochre pellets.
- **Gas texture:** the sulfur trioxide texture is drawn from its colours like every gas.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`, `python tools/check_mod_data.py`, `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- `javac` reported no syntax errors in the changed Java files.

For CI, server game tests:
- `ChemicalInfuserGameTests.theVanadiumBedTurnsSulfurDioxideIntoSulfurTrioxide`: the nickel bed does not serve (the screen names the bed socket). With the vanadium bed, 250 mB of sulfur dioxide and 125 mB of oxygen give 250 mB of sulfur trioxide for exactly 12,800 JE, and the bed stays.
- `ChemicalInfuserGameTests.itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill`: now five recipes.
- `BulkChemicalReactorGameTests.sulfurTrioxideDissolvesIntoSulfuricAcid`: 250 mB of sulfur trioxide and 750 mB of water give 1,000 mB of acid for exactly 8,700 JE.
- `BulkChemicalReactorGameTests.washedIronOreGivesTheContactBedsVanadium`: four washed iron ores and 500 mB of lye give a vanadium pentoxide at the chute, and the oxide on two bricks crafts the bed.

### Results

- **Run [38438099938](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38438099938) on the contributor's fork (commit b325ef6e): green.** Everything compiled; every server game test passed, including the vanadium bed, sulfur trioxide, absorption and vanadium tests; the build without optional integrations and all five client shards passed.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- The vanadium preparation, the bed recipe and the missing carrier charge await the owner's review.
- Still to come:
  - the retained carrier acid;
  - concentrated and electronic-grade acid (the reagent purification station);
  - sulfur recovery from refinery gas.
