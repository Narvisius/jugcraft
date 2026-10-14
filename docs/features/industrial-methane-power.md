# Methane power: carbon dioxide, methanation and the generator's second fuel (factory package 2)

Status: implemented (CI green on the contributor's fork; pending review). Stacked on the [Chemical Infuser](industrial-chemical-infuser.md) and [Chemical Oxidizer](industrial-chemical-oxidizer.md), with the [Gas Burning Generator](industrial-gas-burning-generator.md).
Proposal issue: none. The TODO list's "methanation with its nickel bed and the selected water drain, then methane as the generator's second fuel (256 JE/t)" and "Define distinct CO2 + 4 H2 and CO + 3 H2 methanation recipes, the reusable first catalyst bed from existing nickel/ceramic supplies" (the CO2 half). Quantities, bed, burn value and output are the owner's selections and the plan's proposals in the [starter gas and acid factory](industrial-starter-gas-and-acid-factory.md). The carbon dioxide source is this change's choice, described below for review.
Owner: jimbozoomer-byte
Target milestone and tier: steel-era power, the methane line of the starter gas and acid factory
Primary specialty and supported player role: power generation and chemical processing

## Player experience

A starter factory can now turn coal and hydrogen into methane and burn it for twice the generator's hydrogen output:

1. **Carbon dioxide.** The Chemical Oxidizer burns a coal or charcoal with 250 mB of oxygen into 250 mB of carbon dioxide (100 ticks, 9,600 JE), the same way it burns sulfur.
2. **The nickel bed.** A **Nickel Catalyst Bed** is a nickel ingot between two bricks, the owner's selected one nickel and ordinary fired ceramic. It goes in the Infuser's new **Catalyst bed** socket from the screen. It is never used up, and it cannot be taken out while a batch that needs it runs.
3. **Methanation.** With the bed in, the Chemical Infuser joins 400 mB of hydrogen (left inlet) and 100 mB of carbon dioxide (right inlet) into 100 mB of methane at the product collar and 400 mB of water at the drain below it: 200 ticks, the plan's 25,600 JE. Without a bed, the gases wait and the screen names the bed socket.
4. **Methane power.** The Gas Burning Generator burns methane at the plan's 448 JE per mB and the owner's selected 256 JE/t, so a bucket lasts 87.5 seconds and gives 448,000 JE. Its hydrogen is unchanged: 128 JE per mB at 128 JE/t.

Hydrogen chloride is unchanged and needs no bed. The Engineer's Handbook pages of the three machines describe the new recipes, and JEI lists them.

## Connections

- Existing input producers: coal and charcoal (mined, or charcoal from logs); oxygen and hydrogen from the Electrolytic Separator, the Electrolytic Cell and the Air Separation Unit; nickel ingots from nickel ore; bricks from clay.
- Existing output consumers: methane in the Gas Burning Generator; the drained water in anything that takes water.
- Technology connection: the shared form foundation's tool sockets (their first real use), the Infuser's `jugcraft:nickel_methanation` capability, the Oxidizer's `jugcraft:solid_oxidation` capability and the generator form's fuel list.
- Magic connection: none.
- Reachable entry path: every input comes before the Separator; no step needs methane, so there is no circular unlock.
- Which connections are required vs optional: methane is optional. Hydrogen alone still runs the generator.
- How the specialty stays useful alone: methane stores 3.5 times as much electricity per tank as hydrogen and doubles the generator's output.

## Balance and automation

- **No power loop.** `tools/check_mod_data.py` now counts the cheapest cost of each fluid a recipe uses up, as well as the recipe's own energy, when it checks a generator fuel. Water and lava count as free. With four efficiency cards everywhere:
  - hydrogen still costs 168 JE per mB;
  - carbon dioxide 351.6 JE per mB, mostly its electrolysed oxygen;
  - methane 1,127.6 JE per mB, against the 448 JE per mB it burns for (40%).

  Coal's own fuel value is not counted, so the real margin is wider. (The [Anaerobic Digester](industrial-anaerobic-digester.md) later refined the audit to cost only electricity-only routes; methane, whose carbon always comes from coal, crops or waste, is now a primary fuel there.) Without the change, the check would have seen only the 104 JE per mB of the methanation step and reported a loop. Hydrogen's figure is unchanged.
- **Carbon conservation.** A coal or charcoal counts as 250 reaction amounts of carbon, like a sulfur dust, giving 250 mB of carbon dioxide, enough for 250 mB of methane. Burning it in the Oxidizer recovers no heat.
- **Water.** Methanation gives back 400 mB of water per 100 mB of methane, the plan's liquid anchor of 2 mB per reaction amount. It leaves at the drain and stops the Infuser when the water tank fills (2,000 mB), like any output.
- **Tool.** The bed is a one-time purchase with no wear, as selected.

## Choices made here for review

- **Carbon dioxide from burning coal or charcoal in oxygen.** The plan's methane line wants CO from the Coal Gasifier, whose quantities await the carbon ledger, or "independently supplied CO2". Fermentation already makes bioethanol in the Chemical Reactor, but that reactor has no second output for its CO2. Burning carbon in the Oxidizer is the independent supply chosen. The Gasifier's CO + 3 H2 recipe and brewery CO2 can join later.
- **The audit change** described above.

## Multiplayer and persistence

- The server decides everything; the bed socket takes items only from a player at the screen, never from pipes or hoppers.
- The Infuser's form gains its socket before its first release, so no saved Infuser lacks it and its form version stays 1.
- New registrations: the item `jugcraft:nickel_catalyst_bed`, its item tag (now `jugcraft:catalyst_beds`, shared with the vanadium contact bed), and the gases `jugcraft:carbon_dioxide` and `jugcraft:methane`.
- Not done: two-client and dedicated-server play.

## Dependencies and assets

None new. The Nickel Catalyst Bed's icon is drawn at 16×16 in `tools/petro_textures.py`: a fired-brick tray heaped with nickel pellets, each part outlined in its own darkest tone and lit from the top left. The two gas textures are drawn from their colours like every gas.

## Verification

Locally, without Gradle or a game:
- `python tools/generate_material_data.py`: regenerated the two recipes, the bed's item model, recipe and tag, the gases' fluid tags and names, the socket label and the handbook pages.
- `python tools/check_mod_data.py` with the extended loop audit (hydrogen 168, carbon dioxide 351.6, methane 1,127.6 JE/mB), `python tools/check_icon_maps.py` and `python scripts/check_repository.py`: passed.
- `javac` reported no syntax errors in the changed Java files; it could not compile them without Minecraft's libraries.

For CI, server game tests:
- `ChemicalInfuserGameTests.theNickelBedMakesMethaneAndWater`: with the bed, 400 + 100 mB give 100 mB of methane and 400 mB of water at their collars for exactly 25,600 JE; the bed is locked during the batch and still there after it.
- `ChemicalInfuserGameTests.methanationWaitsForItsBed`: without the bed the gases wait on the bed socket, taking and spending nothing; with it they start.
- `ChemicalInfuserGameTests.itRunsItsOwnRecipeAndIsBuiltFromTheStarterBill`: its two recipes, the bed recipe needing the tool; a nickel ingot between two bricks crafts the bed.
- `ChemicalOxidizerGameTests.charcoalBurnsIntoCarbonDioxide`: a charcoal and 250 mB of oxygen give 250 mB of carbon dioxide for 9,600 JE.
- `GasBurningGeneratorGameTests.methaneBurnsAt256JEATick`: every millibucket burnt is 448 JE made or in hand, at 256 JE a tick.
- `GasBurningGeneratorGameTests.itsFuelItsBalanceAndItsBill`: two fuels, methane at 448 and 256.

### Results

- **Run [38299359276](https://github.com/mmme123-spec/jugcraftfork/actions/runs/38299359276) on the contributor's fork (commit e1d6791e): green.** Everything compiled; "All 1393 required tests passed", the new methanation, bed, charcoal and methane-fuel tests among them; the build without optional integrations and every client test class passed.

No survival playtest, two-client test or performance measurement has been done.

## Rollout and open questions

- CO methanation (CO + 3 H2) waits for the Coal Gasifier and Gas Cleanup and Separation.
- An optional later ruthenium bed, and methane's cryogenic and rocket roles, are later work.
- Reversible: the recipes and the fuel are data and form declarations; the item and gases would need removal migrations once released.
